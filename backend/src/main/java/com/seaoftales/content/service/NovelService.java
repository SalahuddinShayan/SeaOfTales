package com.seaoftales.content.service;

import com.seaoftales.content.entity.Novel;
import com.seaoftales.content.repository.NovelRepository;
import com.seaoftales.storage.ImageStorageService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class NovelService {

    private final NovelRepository novelRepository;
    private final ImageStorageService imageStorageService;

    public NovelService(
            NovelRepository novelRepository,
            ImageStorageService imageStorageService) {

        this.novelRepository = novelRepository;
        this.imageStorageService = imageStorageService;
    }

    public Novel createNovel(Novel novel, MultipartFile coverImage) throws IOException {

        if (coverImage != null && !coverImage.isEmpty()) {
            String imageReference = imageStorageService.store(coverImage);
            novel.setCoverImage(imageReference);
        }

        return novelRepository.save(novel);
    }

    public List<Novel> getAllNovels() {
        return novelRepository.findAll();
    }

    public Resource getCoverImage(Long novelId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new NoSuchElementException("Novel not found."));
        return imageStorageService.load(novel.getCoverImage());
    }
}
