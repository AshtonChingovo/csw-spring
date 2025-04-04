package com.cosw.councilOfSocialWork.domain.cardpro.controller;

import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProSheetClientDto;
import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProStatsDto;
import com.cosw.councilOfSocialWork.domain.cardpro.service.CardProService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/cardpro")
public class CardProController {

    CardProService cardProService;

    public CardProController(CardProService cardProService) {
        this.cardProService = cardProService;
    }

    @GetMapping(path = "")
    public ResponseEntity<Page<CardProSheetClientDto>> getCardProSheet(
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy){
        return new ResponseEntity<>(cardProService.getCardProClients(pageNumber, pageSize, sortBy), HttpStatus.OK);
    }

    @GetMapping(path = "/download")
    public ResponseEntity<?> downloadCardProSheet(){
        cardProService.generateCardProData();
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping(path = "/stats")
    public ResponseEntity<CardProStatsDto> getCardProStats(){
        return new ResponseEntity<>(cardProService.getCardProStats(), HttpStatus.OK);
    }

}
