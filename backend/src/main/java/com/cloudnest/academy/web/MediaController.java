package com.cloudnest.academy.web;

import com.cloudnest.academy.service.MediaService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.time.Duration;

/** Serves private uploads via signed, expiring URLs. Supports HTTP Range requests for video seeking. */
@RestController
@RequestMapping("/api/media")
public class MediaController {
    private final MediaService media;

    public MediaController(MediaService media) { this.media = media; }

    @GetMapping("/{name:.+}")
    public ResponseEntity<Resource> get(@PathVariable String name, @RequestParam long exp, @RequestParam String sig) {
        Path p = media.resolve(name, exp, sig);
        Resource r = new FileSystemResource(p);
        MediaType type = MediaTypeFactory.getMediaType(r).orElse(MediaType.APPLICATION_OCTET_STREAM);
        if (name.endsWith(".vtt")) type = MediaType.parseMediaType("text/vtt");
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate()).body(r);
    }
}
