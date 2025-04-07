package com.cosw.councilOfSocialWork.domain.images.controller;

import com.cosw.councilOfSocialWork.domain.images.dto.ImageDeleteDto;
import com.cosw.councilOfSocialWork.domain.images.dto.ImageDto;
import com.cosw.councilOfSocialWork.domain.images.service.ForwardedEmailProcessingService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RestController
@RequestMapping("api/v1/attachments")
public class ImagesController {

    ForwardedEmailProcessingService emailProcessingService;

    public ImagesController(ForwardedEmailProcessingService emailProcessingService) {
        this.emailProcessingService = emailProcessingService;
    }

    @GetMapping
    public ResponseEntity<Object> downloadImageAttachments(){
        try {
            var processedEmailData = emailProcessingService.createClientListAndDownloadImages();
            return ResponseEntity.status(processedEmailData ? HttpStatus.CREATED : HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Page<ImageDto>> getImages(
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy){
        return new ResponseEntity<>(emailProcessingService.getImages(pageNumber, pageSize, sortBy), HttpStatus.OK);
    }

    @PostMapping("/remove")
    public ResponseEntity<ImageDto> delete(@RequestBody ImageDeleteDto imageDeleteDto){
        return new ResponseEntity<>(emailProcessingService.softDeleteImage(imageDeleteDto), HttpStatus.OK);
    }

    @PostMapping("/remove/undo")
    public ResponseEntity<ImageDto> undoDelete(@RequestBody ImageDeleteDto imageDeleteDto){
        return new ResponseEntity<>(emailProcessingService.undoDeleteImage(imageDeleteDto), HttpStatus.OK);
    }

}
