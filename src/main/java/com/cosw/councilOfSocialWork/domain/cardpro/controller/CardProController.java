package com.cosw.councilOfSocialWork.domain.cardpro.controller;

import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProSheetClientDto;
import com.cosw.councilOfSocialWork.domain.cardpro.service.CardProService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RestController
@RequestMapping("api/v1/cardpro")
public class CardProController {

    CardProService cardProService;

    public CardProController(CardProService cardProService) {
        this.cardProService = cardProService;
    }

    @GetMapping
    public ResponseEntity<Object> generateCardProData(){
        try {
            var cardProSheetProcessed = cardProService.createCardProData();
            return ResponseEntity.status(cardProSheetProcessed ? HttpStatus.CREATED : HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping(path = "/sheet")
    public ResponseEntity<Page<CardProSheetClientDto>> getCardProSheet(
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy){
        return new ResponseEntity<>(cardProService.getCardProSheet(pageNumber, pageSize, sortBy), HttpStatus.OK);
    }

    @GetMapping(path = "/download")
    public void generateThenDownloadCardProFile(){
        cardProService.generateThenDownloadCardProFile();
    }

}
