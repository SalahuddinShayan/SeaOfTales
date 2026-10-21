package com.seaoftales.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalImageStorageServiceTest {
    @TempDir Path directory;

    private MockMultipartFile png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("profilePicture", "picture.png", "image/png", output.toByteArray());
    }

    @Test
    void acceptsValidProfilePictureAndDeletesGeneratedReference() throws Exception {
        LocalImageStorageService storage = new LocalImageStorageService(directory.toString());
        String reference = storage.store(png(100, 100), ImageUploadPolicy.PROFILE_PICTURE);
        assertTrue(Files.isRegularFile(directory.resolve(reference)));
        storage.delete(reference);
        assertFalse(Files.exists(directory.resolve(reference)));
    }

    @Test
    void rejectsProfilePictureAboveCompressedLimitBeforeDecoding() {
        LocalImageStorageService storage = new LocalImageStorageService(directory.toString());
        byte[] oversized = new byte[1_048_577];
        assertThrows(IllegalArgumentException.class, () -> storage.store(new MockMultipartFile("file", "x.png", "image/png", oversized), ImageUploadPolicy.PROFILE_PICTURE));
    }

    @Test
    void rejectsProfilePictureDimensionsAndPixelCount() throws Exception {
        LocalImageStorageService storage = new LocalImageStorageService(directory.toString());
        assertThrows(IllegalArgumentException.class, () -> storage.store(png(1_025, 1), ImageUploadPolicy.PROFILE_PICTURE));
        assertThrows(IllegalArgumentException.class, () -> storage.store(png(1, 1_025), ImageUploadPolicy.PROFILE_PICTURE));
        assertThrows(IllegalArgumentException.class, () -> storage.store(png(1_001, 1_000), ImageUploadPolicy.PROFILE_PICTURE));
    }

    @Test
    void coverPolicyDoesNotApplyProfileDimensions() throws Exception {
        LocalImageStorageService storage = new LocalImageStorageService(directory.toString());
        String reference = storage.store(png(1_500, 1_500), ImageUploadPolicy.NOVEL_COVER);
        assertTrue(Files.isRegularFile(directory.resolve(reference)));
    }
}
