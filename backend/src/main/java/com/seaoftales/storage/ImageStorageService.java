package com.seaoftales.storage;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;

public interface ImageStorageService {

    String store(MultipartFile file, ImageUploadPolicy policy) throws IOException;

    default String store(MultipartFile file) throws IOException { return store(file, ImageUploadPolicy.NOVEL_COVER); }

    void delete(String imageReference);

    Resource load(String imageReference);
}
