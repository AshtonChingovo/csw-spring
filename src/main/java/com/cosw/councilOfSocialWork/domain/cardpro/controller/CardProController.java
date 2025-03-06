package com.cosw.councilOfSocialWork.domain.cardpro.controller;

import com.cosw.councilOfSocialWork.domain.cardpro.service.CardProService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RestController
@RequestMapping("api/va/cardpro")
public class CardProController {

    CardProService cardProService;

    public CardProController(CardProService cardProService) {
        this.cardProService = cardProService;
    }

    @GetMapping
    public void generateCardProData(){
        try {
            cardProService.fetchEmails();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

}
