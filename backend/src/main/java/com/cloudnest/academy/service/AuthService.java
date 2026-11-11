package com.cloudnest.academy.service;

import com.cloudnest.academy.config.ApiException;
import com.cloudnest.academy.config.AppConfig;
import com.cloudnest.academy.domain.*;
import com.cloudnest.academy.security.Crypto;
import com.cloudnest.academy.security.JwtService;
import com.cloudnest.academy.web.Dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Transactional(noRollbackFor = ApiException.class)
public class AuthService {
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");
    private static final int OTP_MINUTES = 10;
    private static final int OTP_MAX_ATTEMPTS = 5;

    private final UserRepository users;
    private final OtpCodeRepository otps;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MailService mail;
    private final AppConfig cfg;

    public AuthService(UserRepository users, OtpCodeRepository otps, RefreshTokenRepository refreshTokens,
                       PasswordEncoder encoder, JwtService jwt, MailService mail, AppConfig cfg) {
        this.users = users;
        this.otps = otps;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mail = mail;
        this.cfg = cfg;
    }

    public static UserDto toDto(User u) {
        return new UserDto(u.id, u.name, u.email, u.role.name(), u.emailVerified, u.country, u.headline, u.bio, u.passwordHash != null);
    }

    private static String norm(String email) { return email.trim().toLowerCase(); }

    public MessageResponse register(RegisterReq req) {
        String email = norm(req.email());
        if (users.findByEmailIgnoreCase(email).isPresent()) throw ApiException.conflict("An account with this email already exists. Please log in.");
        if (req.password() != null && !req.password().isBlank() && req.password().length() < 8)
            throw ApiException.badRequest("Password must be at least 8 characters");
        User u = new User();
        u.name = req.name().trim();
        u.email = email;
        u.marketingOptIn = req.marketing();
        u.country = req.country();
        if (req.password() != null && !req.password().isBlank()) u.passwordHash = encoder.encode(req.password());
        users.save(u);
        audit.info("user.registered id={} email={}", u.id, email);
        return sendCode(email, "LOGIN");
    }

    public MessageResponse requestLoginCode(String rawEmail) {
        String email = norm(rawEmail);
        User u = users.findByEmailIgnoreCase(email).orElseThrow(() -> ApiException.notFound("No account found for this email. Please sign up."));
        if (!u.enabled) throw ApiException.forbidden("This account is disabled");
        return sendCode(email, "LOGIN");
    }

    public MessageResponse requestPasswordReset(String rawEmail) {
        String email = norm(rawEmail);
        // Same response whether or not the account exists, to avoid account enumeration.
        if (users.findByEmailIgnoreCase(email).isEmpty()) return new MessageResponse("If the account exists, a reset code has been sent.", null);
        MessageResponse r = sendCode(email, "RESET");
        return new MessageResponse("If the account exists, a reset code has been sent.", r.devCode());
    }

    private MessageResponse sendCode(String email, String purpose) {
        otps.findTopByEmailAndPurposeAndUsedFalseOrderByIdDesc(email, purpose).ifPresent(o -> o.used = true);
        String code = Crypto.randomDigits(6);
        OtpCode o = new OtpCode();
        o.email = email;
        o.purpose = purpose;
        o.codeHash = Crypto.sha256(email + ":" + code);
        o.expiresAt = Instant.now().plus(Duration.ofMinutes(OTP_MINUTES));
        otps.save(o);
        String what = "RESET".equals(purpose) ? "password reset" : "login";
        mail.send(email, "Your " + cfg.name + " " + what + " code: " + code,
                "Your " + what + " code is " + code + ". It expires in " + OTP_MINUTES + " minutes.\nIf you did not request this, you can ignore this email.");
        return new MessageResponse("We emailed you a 6-digit code.", cfg.devShowOtp ? code : null);
    }

    private void consumeCode(String email, String purpose, String code) {
        OtpCode o = otps.findTopByEmailAndPurposeAndUsedFalseOrderByIdDesc(email, purpose)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired code"));
        if (o.expiresAt.isBefore(Instant.now()) || o.attempts >= OTP_MAX_ATTEMPTS) {
            o.used = true;
            throw ApiException.badRequest("This code has expired. Please request a new one.");
        }
        if (!Crypto.safeEquals(o.codeHash, Crypto.sha256(email + ":" + code.trim()))) {
            o.attempts++;
            throw ApiException.badRequest("Invalid code");
        }
        o.used = true;
    }

    public AuthResponse verifyLoginCode(OtpVerifyReq req) {
        String email = norm(req.email());
        consumeCode(email, "LOGIN", req.code());
        User u = users.findByEmailIgnoreCase(email).orElseThrow(() -> ApiException.notFound("Account not found"));
        if (!u.enabled) throw ApiException.forbidden("This account is disabled");
        boolean first = !u.emailVerified;
        u.emailVerified = true;
        if (first) mail.send(u.email, "Welcome to " + cfg.name + "!",
                "Hi " + u.name + ",\n\nYour account is ready. Start with our free Tech Fundamentals course or browse the catalog: " + cfg.frontendUrl + "/courses");
        audit.info("user.login.otp id={}", u.id);
        return issue(u);
    }

    public AuthResponse loginWithPassword(LoginReq req) {
        User u = users.findByEmailIgnoreCase(norm(req.email())).orElse(null);
        if (u == null || u.passwordHash == null || !encoder.matches(req.password(), u.passwordHash))
            throw ApiException.unauthorized("Incorrect email or password");
        if (!u.enabled) throw ApiException.forbidden("This account is disabled");
        if (!u.emailVerified) throw ApiException.forbidden("Please verify your email first using a login code.");
        audit.info("user.login.password id={}", u.id);
        return issue(u);
    }

    public void resetPassword(ResetReq req) {
        String email = norm(req.email());
        consumeCode(email, "RESET", req.code());
        User u = users.findByEmailIgnoreCase(email).orElseThrow(() -> ApiException.notFound("Account not found"));
        u.passwordHash = encoder.encode(req.newPassword());
        u.emailVerified = true;
        refreshTokens.findByUser_IdAndRevokedFalse(u.id).forEach(t -> t.revoked = true);
        audit.info("user.password.reset id={}", u.id);
    }

    /** Refresh-token rotation with reuse detection. */
    public AuthResponse refresh(String token) {
        RefreshToken rt = refreshTokens.findByTokenHash(Crypto.sha256(token))
                .orElseThrow(() -> ApiException.unauthorized("Session expired. Please log in again."));
        if (rt.revoked) {
            // A revoked token was reused: possible theft. Revoke every session of this user.
            refreshTokens.findByUser_IdAndRevokedFalse(rt.user.id).forEach(t -> t.revoked = true);
            audit.warn("refresh.reuse.detected user={}", rt.user.id);
            throw ApiException.unauthorized("Session expired. Please log in again.");
        }
        if (rt.expiresAt.isBefore(Instant.now()) || !rt.user.enabled) throw ApiException.unauthorized("Session expired. Please log in again.");
        rt.revoked = true;
        return issue(rt.user);
    }

    public void logout(String token) {
        refreshTokens.findByTokenHash(Crypto.sha256(token)).ifPresent(t -> t.revoked = true);
    }

    public void logoutEverywhere(Long userId) {
        refreshTokens.findByUser_IdAndRevokedFalse(userId).forEach(t -> t.revoked = true);
    }

    private AuthResponse issue(User u) {
        String raw = Crypto.randomToken(48);
        RefreshToken rt = new RefreshToken();
        rt.user = u;
        rt.tokenHash = Crypto.sha256(raw);
        rt.expiresAt = Instant.now().plus(Duration.ofDays(cfg.refreshTokenDays));
        refreshTokens.save(rt);
        return new AuthResponse(jwt.issue(u), raw, toDto(u));
    }

    public UserDto updateProfile(Long userId, ProfileReq req) {
        User u = users.findById(userId).orElseThrow();
        u.name = req.name().trim();
        u.country = req.country();
        u.headline = req.headline();
        u.bio = req.bio();
        return toDto(u);
    }

    public void changePassword(Long userId, PasswordReq req) {
        User u = users.findById(userId).orElseThrow();
        if (u.passwordHash != null && (req.currentPassword() == null || !encoder.matches(req.currentPassword(), u.passwordHash)))
            throw ApiException.badRequest("Current password is incorrect");
        u.passwordHash = encoder.encode(req.newPassword());
        audit.info("user.password.changed id={}", u.id);
    }
}
