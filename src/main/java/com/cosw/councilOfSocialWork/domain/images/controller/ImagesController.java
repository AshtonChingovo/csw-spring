package com.cosw.councilOfSocialWork.domain.images.controller;

import com.cosw.councilOfSocialWork.domain.images.service.ForwardedEmailProcessingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
