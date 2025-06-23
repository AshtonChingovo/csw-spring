package com.cosw.councilOfSocialWork.domain.images.service;

import com.cosw.councilOfSocialWork.domain.images.dto.ImageDeleteDto;
import com.cosw.councilOfSocialWork.domain.images.dto.ImageDto;
import com.cosw.councilOfSocialWork.domain.images.repository.ImagesRepository;
import com.cosw.councilOfSocialWork.exception.PictureCannotBeDeletedException;
import com.cosw.councilOfSocialWork.exception.ResourceNotFoundException;
import com.cosw.councilOfSocialWork.mapper.images.ImageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class ImagesService {

    private EmailProcessingService emailProcessingService;
    private final ImagesRepository imagesRepository;
    private ImageMapper mapper;

    private static String redirectUri = "https://cswtest.site/api/v1/oauth2/callback";

    private static String TEST_ENV = "test";
    private static String DEV_ENV = "dev";

    @Value("${spring.profiles.active}")
    private String activeProfile;

    public ImagesService(
            EmailProcessingService emailProcessingService,
            ImagesRepository imagesRepository,
            ImageMapper mapper) {
        this.emailProcessingService = emailProcessingService;
        this.imagesRepository = imagesRepository;
        this.mapper = mapper;
    }

    public boolean createClientListAndDownloadImages(){
        return emailProcessingService.createClientListAndDownloadImages();
    }

    public String encodeAttachmentFilePath(String filePath){
        String encodedFileName;
        String baseFilePath = "/api" + File.separator;
        encodedFileName = URLEncoder.encode(filePath.substring(filePath.lastIndexOf(File.separator) + 1), StandardCharsets.UTF_8).replace("+", "%20");
        return baseFilePath + encodedFileName;
    }

    public Page<ImageDto> getImages(int pageNumber, int pageSize, String sortBy){
        Pageable page = PageRequest.of(pageNumber, pageSize, Sort.by(sortBy));
        return imagesRepository
                .findAll(page)
                .map(image -> {
                    image.setAttachmentPath(encodeAttachmentFilePath(image.getAttachmentPath()));
                    return mapper.imageToImageDto(image);
                });
    }

    public ImageDto softDeleteImage(ImageDeleteDto imageDeleteDto){

        var clientImagesSize = imagesRepository.countByCardProClient_IdAndDeletedFalse(imageDeleteDto.clientId());

        if(clientImagesSize > 1){

            var image = imagesRepository.findById(imageDeleteDto.id()).orElseThrow(() -> new ResourceNotFoundException("Picture not found"));

            image.setDeleted(true);
            image = imagesRepository.save(image);

            return mapper.imageToImageDto(image);

        }
        else{

            log.error("Error: image size {} {}", clientImagesSize, imageDeleteDto.clientId());

            throw new PictureCannotBeDeletedException("Cannot delete, Client only has one picture");
        }

    }

    public ImageDto undoDeleteImage(ImageDeleteDto imageDeleteDto){

        var image = imagesRepository.findById(imageDeleteDto.id()).orElseThrow(() -> new ResourceNotFoundException("Picture not found"));

        image.setDeleted(false);

        image = imagesRepository.save(image);

        return mapper.imageToImageDto(image);

    }

}
