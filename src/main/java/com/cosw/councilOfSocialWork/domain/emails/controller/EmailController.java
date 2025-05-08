package com.cosw.councilOfSocialWork.domain.emails.controller;

import com.cosw.councilOfSocialWork.domain.emails.dto.EmailHeadShotRequestDto;
import com.cosw.councilOfSocialWork.domain.emails.service.EmailService;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "api/v1/mail")
public class EmailController {

    EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping
    ResponseEntity<?> sendEmail(@RequestBody EmailHeadShotRequestDto emailHeadShotRequestDto){
        TrackingSheetClient trackingSheetClient = emailService.sendEmail(emailHeadShotRequestDto.id());
        return new ResponseEntity<>(trackingSheetClient, trackingSheetClient != null ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }

}
