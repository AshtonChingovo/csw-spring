package com.cosw.councilOfSocialWork.domain.cardpro.service;

import com.cosw.councilOfSocialWork.config.EmailConfiguration;
import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProSheetClientDto;
import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import com.cosw.councilOfSocialWork.domain.cardpro.entity.ProcessedCardProClientsStats;
import com.cosw.councilOfSocialWork.domain.cardpro.repository.CardProClientRepository;
import com.cosw.councilOfSocialWork.domain.cardpro.repository.ProcessedCardProClientsStatsRepository;
import com.cosw.councilOfSocialWork.domain.images.entity.Image;
import com.cosw.councilOfSocialWork.domain.images.service.EmailProcessingService;
import com.cosw.councilOfSocialWork.exception.ResourceNotFoundException;
import com.cosw.councilOfSocialWork.mapper.cardPro.CardProClientMapper;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    public boolean sortCardProData(List<CardProClient> cardProClientList){

        String userHome = System.getProperty("user.home");
        String currentYear = String.valueOf(LocalDate.now().getYear());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String dateToday = LocalDate.now().format(formatter);

        String baseFilePath = userHome + File.separator + "Downloads" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator +  "CardPro_Files" + File.separator;

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

    public static void zipDirectory() throws IOException {

        String userHome = System.getProperty("user.home");
        String currentYear = String.valueOf(LocalDate.now().getYear());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String dateToday = LocalDate.now().format(formatter);


        String sourceDir = userHome + File.separator + "Downloads" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator +  "CardPro_Files";
        String zipFile = userHome + File.separator + "Downloads" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator +  "batch.zip";

        Path sourceDirPath = Paths.get(sourceDir);
        Path zipFilePath = Paths.get(zipFile);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            Files.walk(sourceDirPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString().replace("\\", "/"));
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            System.err.println("Failed to zip file: " + path + " - " + e.getMessage());
                        }
                    });
        }
    }

    @Override
    public boolean downloadCardProData() {

        // get list of records with only one valid image
        List<CardProClient> cardProClientList = cardProClientRepository.findAllValidClients();

        sortCardProData(cardProClientList);

        new GenerateThenDownloadCardProSheet(cardProClientList).createFile();

        try {
            zipDirectory();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    @Override
    public ProcessedCardProClientsStats getCardProStats() {
        return processedCardProClientsStatsRepository.findTopByOrderByTransactionIdDesc().orElseThrow(() -> new ResourceNotFoundException("Could not find stats"));
    }

}
