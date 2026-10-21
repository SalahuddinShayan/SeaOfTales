package com.seaoftales.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.util.Iterator;
import java.util.Locale;

@Service
public class LocalImageStorageService implements ImageStorageService {

    private final Path storageLocation;

    public LocalImageStorageService(
            @Value("${seaoftales.storage.location}") String storageLocation) {

        this.storageLocation = Paths.get(storageLocation)
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public String store(MultipartFile file, ImageUploadPolicy policy) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > policy.maxBytes()) throw new IllegalArgumentException("Image exceeds the allowed size.");
        String format;
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) throw new IllegalArgumentException("Only valid image files are supported.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Only JPEG, PNG, and GIF images are supported.");
            ImageReader reader = readers.next();
            try {
                format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png") && !format.equals("gif")) throw new IllegalArgumentException("Only JPEG, PNG, and GIF images are supported.");
                reader.setInput(input, true, true);
                long width = reader.getWidth(0), height = reader.getHeight(0);
                if (policy.maxWidth() > 0 && (width > policy.maxWidth() || height > policy.maxHeight() || width > policy.maxPixels() / Math.max(1L, height))) throw new IllegalArgumentException("Image dimensions are too large.");
                if (policy == ImageUploadPolicy.PROFILE_PICTURE && format.equals("gif") && reader.getNumImages(true) > 1) throw new IllegalArgumentException("Animated GIFs are not supported for profile pictures.");
                reader.read(0);
            } finally { reader.dispose(); }
        }

        Files.createDirectories(storageLocation);

        String extension = format.equals("png") ? ".png" : format.equals("gif") ? ".gif" : ".jpg";

        String filename = UUID.randomUUID() + extension;

        Path target = storageLocation.resolve(filename);

        file.transferTo(target);

        return filename;
    }

    @Override public void delete(String imageReference) {
        if (imageReference == null || imageReference.isBlank()) return;
        Path reference = Paths.get(imageReference);
        if (reference.isAbsolute() || reference.getNameCount() != 1) return;
        try { Files.deleteIfExists(storageLocation.resolve(reference).normalize()); } catch (IOException ignored) { }
    }

    @Override
    public Resource load(String imageReference) {
        if (imageReference == null || imageReference.isBlank()) {
            throw new IllegalArgumentException("Image reference is required.");
        }

        Path reference = Paths.get(imageReference);
        if (reference.isAbsolute() || reference.getNameCount() != 1) {
            throw new IllegalArgumentException("Invalid image reference.");
        }

        Path image = storageLocation.resolve(reference).normalize();
        if (!image.startsWith(storageLocation) || !Files.isRegularFile(image)) {
            throw new IllegalArgumentException("Image not found.");
        }

        try {
            return new UrlResource(image.toUri());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Image not found.", exception);
        }
    }
}
