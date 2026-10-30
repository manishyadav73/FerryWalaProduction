package backend.service;

import backend.error.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Vendor profile photos live on disk (UPLOAD_DIR), not in MySQL; the database
 * only stores the generated file name.
 *
 * Security: we never use the client's file name or content type. Type is
 * decided from magic bytes; JPEG/PNG are decoded and re-encoded (strips EXIF
 * GPS data and any payload hidden in the file) and capped at 1280 px; WebP is
 * stored as-is after a signature check. Files are served with a fixed
 * content type and "nosniff".
 */
@Service
public class MediaStorageService {

    public static final Pattern FILENAME = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    private static final int MAX_DIMENSION = 1280;
    private static final long MAX_PIXELS = 25_000_000L;

    private final Path dir;
    private final long maxBytes;

    public MediaStorageService(@Value("${app.upload-dir}") String uploadDir,
                               @Value("${app.upload.max-bytes:2097152}") long maxBytes) {
        this.dir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("vendors");
        this.maxBytes = maxBytes;
        try {
            Files.createDirectories(this.dir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload directory " + this.dir, e);
        }
    }

    /** @return the generated file name that should be stored on the vendor. */
    public String storeVendorImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("NO_FILE", "Choose an image to upload.");
        }
        if (file.getSize() > maxBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                    "That file is too large. Please choose an image under 2 MB.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("UPLOAD_FAILED", "We couldn't read that file. Please try again.");
        }

        String ext = detectExtension(bytes);
        if (ext == null) {
            throw ApiException.badRequest("INVALID_IMAGE", "Please upload a JPG, PNG or WebP image.");
        }

        byte[] toStore = "webp".equals(ext) ? bytes : reencode(bytes, ext);
        String filename = UUID.randomUUID() + "." + ext;
        try {
            Files.write(dir.resolve(filename), toStore, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "UPLOAD_FAILED",
                    "We couldn't save that image. Please try again.");
        }
        return filename;
    }

    public void deleteQuietly(String filename) {
        if (filename == null || !FILENAME.matcher(filename).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(dir.resolve(filename));
        } catch (IOException ignored) {
            // an orphaned file is harmless; do not fail the request
        }
    }

    public byte[] read(String filename) {
        if (filename == null || !FILENAME.matcher(filename).matches()) {
            throw ApiException.notFound("Image not found.");
        }
        Path path = dir.resolve(filename).normalize();
        if (!path.startsWith(dir) || !Files.isRegularFile(path)) {
            throw ApiException.notFound("Image not found.");
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw ApiException.notFound("Image not found.");
        }
    }

    public String contentType(String filename) {
        if (filename.endsWith(".png")) {
            return "image/png";
        }
        if (filename.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private static String detectExtension(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }

    private byte[] reencode(byte[] bytes, String ext) {
        try {
            checkDimensions(bytes);
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(bytes));
            if (src == null) {
                throw ApiException.badRequest("INVALID_IMAGE", "That image could not be read. Try a different one.");
            }
            int w = src.getWidth();
            int h = src.getHeight();
            double scale = Math.min(1.0, (double) MAX_DIMENSION / Math.max(w, h));
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));

            boolean jpeg = "jpg".equals(ext);
            BufferedImage out = new BufferedImage(nw, nh,
                    jpeg ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            try {
                if (jpeg) {
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, nw, nh);
                }
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(src, 0, 0, nw, nh, null);
            } finally {
                g.dispose();
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            if (!ImageIO.write(out, jpeg ? "jpg" : "png", bos)) {
                throw ApiException.badRequest("INVALID_IMAGE", "That image could not be processed.");
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw ApiException.badRequest("INVALID_IMAGE", "That image could not be read. Try a different one.");
        }
    }

    /** Reject decompression bombs before decoding the whole image. */
    private void checkDimensions(byte[] bytes) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (in == null) {
                throw ApiException.badRequest("INVALID_IMAGE", "That image could not be read. Try a different one.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw ApiException.badRequest("INVALID_IMAGE", "That image could not be read. Try a different one.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                long pixels = (long) reader.getWidth(0) * (long) reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    throw ApiException.badRequest("IMAGE_TOO_BIG", "That image is too large in dimensions. Please resize it.");
                }
            } finally {
                reader.dispose();
            }
        }
    }
}
