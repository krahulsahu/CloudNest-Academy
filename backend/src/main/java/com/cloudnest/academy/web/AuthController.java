package com.cloudnest.academy.web;

import com.cloudnest.academy.service.AuthService;
import com.cloudnest.academy.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    public MessageResponse register(@Valid @RequestBody RegisterReq req) { return auth.register(req); }

    @PostMapping("/otp/request")
    public MessageResponse requestCode(@Valid @RequestBody EmailReq req) { return auth.requestLoginCode(req.email()); }

    @PostMapping("/otp/verify")
    public AuthResponse verify(@Valid @RequestBody OtpVerifyReq req) { return auth.verifyLoginCode(req); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginReq req) { return auth.loginWithPassword(req); }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshReq req) { return auth.refresh(req.refreshToken()); }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody RefreshReq req) { auth.logout(req.refreshToken()); }

    @PostMapping("/password/forgot")
    public MessageResponse forgot(@Valid @RequestBody EmailReq req) { return auth.requestPasswordReset(req.email()); }

    @PostMapping("/password/reset")
    public MessageResponse reset(@Valid @RequestBody ResetReq req) {
        auth.resetPassword(req);
        return new MessageResponse("Password updated. You can now log in.", null);
    }
}
