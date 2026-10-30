package backend.controller;

import backend.service.MediaStorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** Serves uploaded vendor photos. File names are random UUIDs, so responses are cacheable forever. */
@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaStorageService media;

    public MediaController(MediaStorageService media) {
        this.media = media;
    }

    @GetMapping("/vendors/{filename:.+}")
    public ResponseEntity<byte[]> vendorImage(@PathVariable("filename") String filename) {
        byte[] data = media.read(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.contentType(filename)))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'")
                .body(data);
    }
}
