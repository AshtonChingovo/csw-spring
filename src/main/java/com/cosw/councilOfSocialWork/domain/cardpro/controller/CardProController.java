package com.cosw.councilOfSocialWork.domain.cardpro.controller;

import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProSheetClientDto;
import com.cosw.councilOfSocialWork.domain.cardpro.entity.ProcessedCardProClientsStats;
import com.cosw.councilOfSocialWork.domain.cardpro.service.CardProService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @GetMapping(path = "/valid")
    public ResponseEntity<Page<CardProSheetClientDto>> getValidClientsCardProSheet(
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "all") String filter){
        return new ResponseEntity<>(cardProService.getValidCardProClients(pageNumber, pageSize, sortBy, search, filter), HttpStatus.OK);
    }

    @GetMapping(path = "")
    public ResponseEntity<Page<CardProSheetClientDto>> getCardProSheet(
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "all") String filter){
        return new ResponseEntity<>(cardProService.getCardProClients(pageNumber, pageSize, sortBy, search, filter), HttpStatus.OK);
    }

    @GetMapping(path = "/download")
    public ResponseEntity<Resource> downloadCardProSheet(@RequestParam(defaultValue = "batch") String batchNumber){
        Resource file = cardProService.downloadCardProData(batchNumber);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    @GetMapping(path = "/stats")
    public ResponseEntity<ProcessedCardProClientsStats> getCardProStats(){
        return new ResponseEntity<>(cardProService.getCardProStats(), HttpStatus.OK);
    }

}
