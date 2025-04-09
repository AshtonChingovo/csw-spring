package com.cosw.councilOfSocialWork.domain.cardpro.service;

import com.cosw.councilOfSocialWork.config.EmailConfiguration;
import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProSheetClientDto;
import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProStatsDto;
import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import com.cosw.councilOfSocialWork.domain.cardpro.entity.ProcessedCardProClientsStats;
import com.cosw.councilOfSocialWork.domain.cardpro.repository.CardProClientRepository;
import com.cosw.councilOfSocialWork.domain.cardpro.repository.ProcessedCardProClientsStatsRepository;
import com.cosw.councilOfSocialWork.domain.images.entity.Image;
import com.cosw.councilOfSocialWork.domain.images.service.EmailProcessingService;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import com.cosw.councilOfSocialWork.exception.ResourceNotFoundException;
import com.cosw.councilOfSocialWork.mapper.cardPro.CardProClientMapper;
import jakarta.mail.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CardProServiceImpl implements CardProService{

    private final CardProClientRepository cardProClientRepository;
    private final ProcessedCardProClientsStatsRepository processedCardProClientsStatsRepository;
    private final EmailProcessingService emailProcessingService;
    private final EmailConfiguration emailConfiguration;
    private final CardProClientMapper mapper;

    public CardProServiceImpl(
            CardProClientRepository cardProClientRepository,
            ProcessedCardProClientsStatsRepository processedCardProClientsStatsRepository,
            EmailProcessingService emailProcessingService,
            EmailConfiguration emailConfiguration,
            CardProClientMapper mapper) {
        this.cardProClientRepository = cardProClientRepository;
        this.processedCardProClientsStatsRepository = processedCardProClientsStatsRepository;
        this.emailProcessingService = emailProcessingService;
        this.emailConfiguration = emailConfiguration;
        this.mapper = mapper;
    }

    @Override
    public Page<CardProSheetClientDto> getCardProClients(int pageNumber, int pageSize, String sortBy) {
        Pageable page = PageRequest.of(pageNumber, pageSize, Sort.by(sortBy));
        return cardProClientRepository.findAllSorted(page).map(mapper::cardProClientToCardProSheetClientDto);
    }

    @Override
    public boolean generateCardProData() {
        return new GenerateThenDownloadCardProSheet(cardProClientRepository.findAll()).createFile();
    }

    public boolean sortCardProData(){

        String userHome = System.getProperty("user.home");
        String currentYear = String.valueOf(LocalDate.now().getYear());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String dateToday = LocalDate.now().format(formatter);

        String baseFilePath = userHome + File.separator + "Downloads" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator +  "CardPro_Images" + File.separator;

        // get list of records with only one valid image
        List<CardProClient> cardProClientList = cardProClientRepository.findAllValidClients();

        // get move files to new folder
        cardProClientList.forEach(cardProClient -> {
            Optional<Image> image = cardProClient.getImages().stream()
                    .filter(it -> it.getDeleted() == null || !it.getDeleted())
                    .findFirst();

            image.ifPresent(clientImage -> {

                var fileName = createNewAttachmentFileName(cardProClient, clientImage);
                var newFilePath = baseFilePath + fileName;

                try {
                    FileUtils.moveFile(FileUtils.getFile(clientImage.getAttachmentPath()), FileUtils.getFile(newFilePath));
                } catch (IOException e) {
                    log.error("Error: Images {}", e.toString());
                }

            });
        });

        return true;
    }

    private String createNewAttachmentFileName(CardProClient client, Image image){
        try {

            String[] usernames = client.getName().split(" ");
            StringBuilder fileName = new StringBuilder();

            for(String name: usernames){
                fileName.append(name).append(" ");
            }

            fileName.append(client.getSurname());


            var lastIndexOfDot = image.getAttachmentPath().lastIndexOf(".");
            fileName.append(image.getAttachmentPath().substring(lastIndexOfDot));

            return fileName.toString();

        } catch (UsernameNotFoundException e) {
            log.error("Client not found");
            log.error("ERROR createNewAttachmentFileName() :: {}", client.getEmail());
            return "";
        }
    }

    public boolean zipFolder(){
        return false;
    }

    @Override
    public boolean downloadCardProData() {

        sortCardProData();

        return true;
    }

    @Override
    public ProcessedCardProClientsStats getCardProStats() {
        return processedCardProClientsStatsRepository.findTopByOrderByTransactionIdDesc().orElseThrow(() -> new ResourceNotFoundException("Could not find stats"));
    }

}
