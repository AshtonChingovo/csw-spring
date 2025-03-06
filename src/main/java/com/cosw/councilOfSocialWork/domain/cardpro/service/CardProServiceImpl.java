package com.cosw.councilOfSocialWork.domain.cardpro.service;

import com.cosw.councilOfSocialWork.domain.cardpro.repository.CardProClientRepository;
import jakarta.mail.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

@Service
@Slf4j
public class CardProServiceImpl implements CardProService{

    // 917627765041-n2h6dh4emg8gbtrs37mv96sqbu1dn426.apps.googleusercontent.com

    private final CardProClientRepository cardProClientRepository;
    private final EmailProcessingService emailProcessingService;

    public CardProServiceImpl(CardProClientRepository cardProClientRepository, EmailProcessingService emailProcessingService) {
        this.cardProClientRepository = cardProClientRepository;
        this.emailProcessingService = emailProcessingService;
    }

    @Override
    public void fetchEmails() throws IOException, GeneralSecurityException {
        var cardProClientList = emailProcessingService.createClientListAndDownloadImages();
        if(!cardProClientList.isEmpty())
            cardProClientRepository.saveAll(cardProClientList);
    }

    @Override
    public void generateCardProData() {

        try {
            Properties props = System.getProperties();
            props.setProperty("mail.store.protocol", "imaps");

            Session session = Session.getDefaultInstance(props, null);

            Store store = session.getStore("imaps");
            store.connect("imap.googlemail.com", "coswzw@gmail.com", "coswzw2025");

            Folder inbox = store.getFolder("inbox");

            log.info("Inbox size {}", inbox.getMessageCount());
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

    }

}
