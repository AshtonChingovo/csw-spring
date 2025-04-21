package com.cosw.councilOfSocialWork.domain.trackingSheet.controller;

import com.cosw.councilOfSocialWork.domain.trackingSheet.dto.TrackingSheetClientDto;
import com.cosw.councilOfSocialWork.domain.trackingSheet.service.TrackingSheetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping(path = "api/v1/tracking-sheet")
public class TrackingSheetController {

    TrackingSheetService trackingSheetService;

    public TrackingSheetController(TrackingSheetService trackingSheetService) {
        this.trackingSheetService = trackingSheetService;
    }

    @PostMapping(path = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Object> postTrackingSheet(@RequestParam("file") MultipartFile file){
        var trackingSheetProcessed = trackingSheetService.processTrackingSheet(file);
        return ResponseEntity.status(trackingSheetProcessed ? HttpStatus.CREATED : HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @GetMapping
    public ResponseEntity<Page<TrackingSheetClientDto>> getTrackingSheet(
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "registrationYear") String sortBy){
        return new ResponseEntity<>(trackingSheetService.getTrackingSheet(pageNumber, pageSize, sortBy), HttpStatus.OK);
    }

}
