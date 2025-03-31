package com.cosw.councilOfSocialWork.domain.cardpro.service;

import com.cosw.councilOfSocialWork.config.EmailConfiguration;
import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProSheetClientDto;
import com.cosw.councilOfSocialWork.domain.cardpro.repository.CardProClientRepository;
import com.cosw.councilOfSocialWork.domain.images.service.EmailProcessingService;
import com.cosw.councilOfSocialWork.mapper.cardPro.CardProClientMapper;
import jakarta.mail.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@Slf4j
public class CardProServiceImpl implements CardProService{

    private final CardProClientRepository cardProClientRepository;
    private final EmailProcessingService emailProcessingService;
    private final EmailConfiguration emailConfiguration;
    private final CardProClientMapper mapper;

    public CardProServiceImpl(CardProClientRepository cardProClientRepository, EmailProcessingService emailProcessingService, EmailConfiguration emailConfiguration, CardProClientMapper mapper) {
        this.cardProClientRepository = cardProClientRepository;
        this.emailProcessingService = emailProcessingService;
        this.emailConfiguration = emailConfiguration;
        this.mapper = mapper;
    }

    @Override
    public boolean createCardProData() throws IOException, GeneralSecurityException {
        // delete all current data & save new generated data
        // cardProClientRepository.deleteAll();

        var cardProClientList = emailProcessingService.createClientListAndDownloadImages();

        if(!cardProClientList.isEmpty()){
            cardProClientRepository.saveAll(cardProClientList);
            return true;
        }

        return false;
    }

    @Override
    public Page<CardProSheetClientDto> getCardProSheet(int pageNumber, int pageSize, String sortBy) {
        Pageable page = PageRequest.of(pageNumber, pageSize, Sort.by(sortBy));
        return cardProClientRepository.findAll(page).map(mapper::cardProClientToCardProSheetClientDto);
    }

    @Override
    public void generateThenDownloadCardProFile() {
        new GenerateThenDownloadCardProfile(cardProClientRepository.findAll()).createFile();
    }

    @Override
    public void generateCardProData() {

        try {

            Store store = emailConfiguration.createStoreBean();

            Folder inbox = store.getFolder("inbox");

            log.info("Inbox size {}", inbox.getMessageCount());
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

    }

}
