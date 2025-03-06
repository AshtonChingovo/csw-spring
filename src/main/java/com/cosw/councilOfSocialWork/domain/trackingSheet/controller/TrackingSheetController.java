package com.cosw.councilOfSocialWork.domain.trackingSheet.controller;

import com.cosw.councilOfSocialWork.domain.trackingSheet.service.TrackingSheetService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(path = "api/v1/tracking-sheet")
public class TrackingSheetController {

    TrackingSheetService trackingSheetService;

    public TrackingSheetController(TrackingSheetService trackingSheetService) {
        this.trackingSheetService = trackingSheetService;
    }

    @PostMapping(path = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public String postTrackingSheet(@RequestParam("file") MultipartFile file){
        return trackingSheetService.processTrackingSheet(file);
    }

    @GetMapping
    public void getTrackingSheet(){

    }

}
