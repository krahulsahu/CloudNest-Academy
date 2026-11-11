package com.cloudnest.academy.service;

import com.cloudnest.academy.config.ApiException;
import com.cloudnest.academy.config.AppConfig;
import com.cloudnest.academy.security.Crypto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Private file storage for videos, lab files, PDFs and captions.
 * Files are stored outside the web root and are only reachable with short-lived HMAC-signed URLs.
 * For AWS production, replace store()/resolve() with S3 + CloudFront signed URLs.
 */
@Service
public class MediaService {
    private static final Set<String> ALLOWED = Set.of("mp4", "webm", "m4v", "mov", "vtt", "pdf", "zip", "png", "jpg",
            "jpeg", "gif", "svg", "txt", "md", "json", "yaml", "yml", "xml", "csv", "sh", "py", "java", "js", "ts", "tf");
    private static final Pattern SAFE_NAME = Pattern.compile("^[a-f0-9\\-]{36}\\.[a-z0-9]{1,5}$");
    private final AppConfig cfg;

    public MediaService(AppConfig cfg) { this.cfg = cfg; }

    public String store(MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(ext)) throw ApiException.badRequest("File type ." + ext + " is not allowed");
        if (file.isEmpty()) throw ApiException.badRequest("File is empty");
        String name = UUID.randomUUID() + "." + ext;
        try (InputStream in = file.getInputStream()) {
            Path dir = Paths.get(cfg.uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Files.copy(in, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store file", e);
        }
        return name;
    }

    /** Turns "upload:abc.mp4" into a signed, expiring URL. External URLs are returned unchanged. */
    public String publicUrl(String ref) {
        if (ref == null || ref.isBlank()) return null;
        if (!ref.startsWith("upload:")) return ref;
        String name = ref.substring(7);
        long exp = Instant.now().getEpochSecond() + cfg.mediaUrlMinutes * 60;
        return "/api/media/" + name + "?exp=" + exp + "&sig=" + Crypto.hmacUrl(cfg.mediaSecret, name + "|" + exp);
    }

    public Path resolve(String name, long exp, String sig) {
        if (!SAFE_NAME.matcher(name).matches()) throw ApiException.notFound("File not found");
        if (exp < Instant.now().getEpochSecond()) throw ApiException.forbidden("This link has expired. Reload the lesson.");
        if (!Crypto.safeEquals(Crypto.hmacUrl(cfg.mediaSecret, name + "|" + exp), sig)) throw ApiException.forbidden("Invalid link");
        Path p = Paths.get(cfg.uploadDir).toAbsolutePath().normalize().resolve(name).normalize();
        if (!Files.exists(p)) throw ApiException.notFound("File not found");
        return p;
    }
}
