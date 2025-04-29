package com.cosw.councilOfSocialWork.domain.transactionHistory.controller;

import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProSheetClientDto;
import com.cosw.councilOfSocialWork.domain.cardpro.entity.ProcessedCardProClientsStats;
import com.cosw.councilOfSocialWork.domain.cardpro.service.CardProService;
import com.cosw.councilOfSocialWork.domain.transactionHistory.dto.CardProTransactionDto;
import com.cosw.councilOfSocialWork.domain.transactionHistory.service.CardProTransactionService;
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
@RequestMapping("api/v1/transactions")
public class CardProTransactionsController {

    CardProTransactionService cardProTransactionService;

    public CardProTransactionsController(CardProTransactionService cardProTransactionService) {
        this.cardProTransactionService = cardProTransactionService;
    }

    @GetMapping(path = "")
    public ResponseEntity<Page<CardProTransactionDto>> getCardProSheet(
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "all") String filter) {
        return new ResponseEntity<>(cardProTransactionService.getCardProClients(pageNumber, pageSize, sortBy, search, filter), HttpStatus.OK);
    }

}
