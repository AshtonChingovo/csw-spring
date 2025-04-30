package com.cosw.councilOfSocialWork.domain.trackingSheet.controller;

import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProSheetClientDto;
import com.cosw.councilOfSocialWork.domain.googleAuth.service.GoogleOAuthService;
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
    GoogleOAuthService googleOAuthService;

    private static String TEST_ENV = "test";
    private static String DEV_ENV = "dev";

    @Value("${spring.profiles.active}")
    private String activeProfile;

    public TrackingSheetController(TrackingSheetService trackingSheetService, GoogleOAuthService googleOAuthService) {
        this.trackingSheetService = trackingSheetService;
        this.googleOAuthService = googleOAuthService;
    }

    @PostMapping(path = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Object> postTrackingSheet(@RequestParam("file") MultipartFile file){

        // check if OAuth token is there & valid first
        if(activeProfile.equals(TEST_ENV)){
            var authTokenCheck = googleOAuthService.checkToken();
            if(!authTokenCheck.getRedirectUrl().equals("Success"))
                return new ResponseEntity<>(authTokenCheck, HttpStatus.OK);
        }

        var trackingSheetProcessed = trackingSheetService.processTrackingSheet(file);
        return ResponseEntity.status(trackingSheetProcessed ? HttpStatus.CREATED : HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @GetMapping
    public ResponseEntity<Page<TrackingSheetClientDto>> getTrackingSheet(
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "registrationYear") String sortBy,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "all") String filter){
        return new ResponseEntity<>(trackingSheetService.getTrackingSheet(pageNumber, pageSize, sortBy, search, filter), HttpStatus.OK);
    }

}
