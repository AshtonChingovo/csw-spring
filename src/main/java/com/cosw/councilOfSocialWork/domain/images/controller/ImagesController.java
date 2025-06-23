package com.cosw.councilOfSocialWork.domain.images.controller;

import com.cosw.councilOfSocialWork.domain.images.dto.ImageDeleteDto;
import com.cosw.councilOfSocialWork.domain.images.dto.ImageDto;
import com.cosw.councilOfSocialWork.domain.images.service.ImagesService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/attachments")
public class ImagesController {

    ImagesService imagesService;

    public ImagesController(ImagesService imagesService) {
        this.imagesService = imagesService;
    }

    @GetMapping
    public ResponseEntity<Object> downloadImageAttachments(){
        var processedEmailData = imagesService.createClientListAndDownloadImages();
        return ResponseEntity.status(processedEmailData ? HttpStatus.CREATED : HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @GetMapping("/list")
    public ResponseEntity<Page<ImageDto>> getImages(
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy){
        return new ResponseEntity<>(imagesService.getImages(pageNumber, pageSize, sortBy), HttpStatus.OK);
    }

    @PostMapping("/remove")
    public ResponseEntity<ImageDto> delete(@RequestBody ImageDeleteDto imageDeleteDto){
        return new ResponseEntity<>(imagesService.softDeleteImage(imageDeleteDto), HttpStatus.OK);
    }

    @PostMapping("/remove/undo")
    public ResponseEntity<ImageDto> undoDelete(@RequestBody ImageDeleteDto imageDeleteDto){
        return new ResponseEntity<>(imagesService.undoDeleteImage(imageDeleteDto), HttpStatus.OK);
    }

}
