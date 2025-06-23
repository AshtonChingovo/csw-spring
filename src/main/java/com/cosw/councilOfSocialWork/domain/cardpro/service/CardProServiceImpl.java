package com.cosw.councilOfSocialWork.domain.cardpro.service;

import com.cosw.councilOfSocialWork.config.EmailConfiguration;
import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProSheetClientDto;
import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import com.cosw.councilOfSocialWork.domain.cardpro.entity.ProcessedCardProClientsStats;
import com.cosw.councilOfSocialWork.domain.cardpro.repository.CardProClientRepository;
import com.cosw.councilOfSocialWork.domain.cardpro.repository.ProcessedCardProClientsStatsRepository;
import com.cosw.councilOfSocialWork.domain.images.entity.Image;
import com.cosw.councilOfSocialWork.domain.images.service.EmailProcessingService;
import com.cosw.councilOfSocialWork.exception.PictureFileException;
import com.cosw.councilOfSocialWork.exception.ResourceNotFoundException;
import com.cosw.councilOfSocialWork.exception.ZipFileException;
import com.cosw.councilOfSocialWork.mapper.cardPro.CardProClientMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

    private static String TEST_ENV = "test";
    private static String DEV_ENV = "dev";

    private final String FILTER_BY_ALL = "all";
    private final String FILTER_BY_MULTIPLE_IMAGES = "multiple_images";
    private final String FILTER_BY_NOT_IN_TRACKING_SHEET = "not_in_tracking_sheet";

    @Value("${spring.profiles.active}")
    private String activeProfile;

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
    public Page<CardProSheetClientDto> getCardProClients(int pageNumber, int pageSize, String sortBy, String search, String filter) {

        Pageable page = PageRequest.of(pageNumber, pageSize, Sort.by(sortBy));

        Page<CardProClient> cardProClients;

        if(search.isEmpty() && FILTER_BY_ALL.equals(filter)){
            cardProClients = cardProClientRepository.findAllSorted(page);
        }
        else{
            cardProClients = fetchFilteredResult(page, search, filter);
        }

        return cardProClients.map(client -> {
            //client.getImages().forEach(image -> image.setAttachmentPath(encodeAttachmentFilePath(image.getAttachmentPath())));
            client.getImages().forEach(image -> image.setAttachmentPath(image.getAttachmentPath()));
            return mapper.cardProClientToCardProSheetClientDto(client);
        });
    }

    /*
    * Valid CardPro clients are those with exactly one image with deleted == null OR deleted == false in its Set<Images>
    * */
    @Override
    public Page<CardProSheetClientDto> getValidCardProClients(int pageNumber, int pageSize, String sortBy, String search, String filter) {
        Pageable page = PageRequest.of(pageNumber, pageSize, Sort.by(sortBy));

        Page<CardProClient> cardProClients;

        if(search.isEmpty() && FILTER_BY_ALL.equals(filter))
            cardProClients = cardProClientRepository.findAllSortedClientsPages(page);
        else
            cardProClients = fetchFilteredResult(page, search, filter);

        // remove invalid pictures i.e pictures marked as deleted
        cardProClients.forEach(client -> {
            Set<Image> filteredImages = client.getImages().stream()
                    .filter(img -> Boolean.FALSE.equals(img.getDeleted()) || img.getDeleted() == null) // only keep non-deleted images
                    .collect(Collectors.toSet());
            client.setImages(filteredImages);
        });

        return cardProClients.map(mapper::cardProClientToCardProSheetClientDto);
    }

    public Page<CardProClient> fetchFilteredResult(Pageable page, String searchParam, String filterParam){

        if(!searchParam.isEmpty()){
            return switch (filterParam) {
                case FILTER_BY_ALL -> cardProClientRepository.searchClients(searchParam, page);
                case FILTER_BY_MULTIPLE_IMAGES -> cardProClientRepository.searchClientsWithMultipleImages(searchParam, page);
                case FILTER_BY_NOT_IN_TRACKING_SHEET -> cardProClientRepository.searchClientsNotInTrackingSheet(searchParam, page);
                default -> cardProClientRepository.findAllSorted(page);
            };
        }
        else{
            return switch (filterParam) {
                case FILTER_BY_MULTIPLE_IMAGES -> cardProClientRepository.findAllClientsWithMultipleImages(page);
                case FILTER_BY_NOT_IN_TRACKING_SHEET -> cardProClientRepository.findAllClientsNotInTrackingSheet(page);
                default -> cardProClientRepository.findAllSorted(page);
            };
        }
    }

/*    public String encodeAttachmentFilePath(String filePath){
        String encodedFileName;

        String baseFilePath = "/api" + File.separator;
        encodedFileName = URLEncoder.encode(filePath.substring(filePath.lastIndexOf(File.separator) + 1), StandardCharsets.UTF_8).replace("+", "%20");
        return baseFilePath + encodedFileName;
    }*/

    @Override
    public boolean generateCardProData() {
        return new CardProExcelSheetGeneration(cardProClientRepository.findAll(), activeProfile).createFile();
    }

    @Override
    public Resource downloadCardProData(String batchNumber) {

        Resource resource;
        String userHome = System.getProperty("user.home");

        String currentYear = String.valueOf(LocalDate.now().getYear());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String dateToday = LocalDate.now().format(formatter);

        var createdZipFilePath = "";

        if(TEST_ENV.equals(activeProfile)){
            createdZipFilePath = "csw_files" + File.separator;
        }
        else {
            createdZipFilePath = userHome + File.separator + "Downloads" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator;
        }

        // get list of records with only one valid image
        List<CardProClient> cardProClientList = cardProClientRepository.findAllValidClients();

        renameAndMoveCardProPictures(cardProClientList);

        new CardProExcelSheetGeneration(cardProClientList, activeProfile).createFile();

        try {
            zipCardProDataDirectory(batchNumber);

            Path filePath = Paths.get(createdZipFilePath).resolve("Batch " + batchNumber + ".zip");
            resource = new UrlResource(filePath.toUri());

        }
        catch (MalformedURLException e) {
            log.error("Error: creating Zip file {}", e.toString());
            throw new ZipFileException("Failed to create zip file");
        }
        catch (IOException e) {
            log.error("Error: failed zipping folder {}", e.toString());
            throw new ZipFileException("");
        }

        return resource;
    }

    public boolean renameAndMoveCardProPictures(List<CardProClient> cardProClientList){

        String userHome = System.getProperty("user.home");
        String currentYear = String.valueOf(LocalDate.now().getYear());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String dateToday = LocalDate.now().format(formatter);

        String baseFilePath = "";

        if(TEST_ENV.equals(activeProfile))
            baseFilePath =  "csw_files" + File.separator + "cardpro_files" + File.separator + "images" + File.separator;
        else
            baseFilePath = userHome + File.separator + "Downloads" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator +  "CardPro_Files" + File.separator;

        String finalBaseFilePath = baseFilePath;

        if(deleteImagesInCardProImagesDirectory(finalBaseFilePath)){
            cardProClientList.forEach(cardProClient -> {

                Optional<Image> image = cardProClient.getImages().stream()
                        .filter(it -> it.getDeleted() == null || !it.getDeleted())
                        .findFirst();

                image.ifPresent(clientImage -> {

                    var fileName = createNewAttachmentFileName(cardProClient, clientImage);
                    var newFilePath = finalBaseFilePath + fileName;

                    try {
                        FileUtils.copyFile(FileUtils.getFile(clientImage.getAttachmentPath()), FileUtils.getFile(newFilePath));
                        // FileUtils.moveFile(FileUtils.getFile(clientImage.getAttachmentPath()), FileUtils.getFile(newFilePath));
                    } catch (IOException e) {
                        log.error("Error: moving/renaming image files {}", e.toString());
                        throw new PictureFileException("Failed to move/rename file " + e);
                    }

                });
            });
        }
        else{
            log.error("Failed deleting images folder");
            throw new PictureFileException("Failed to move/rename file ");
        }

        return true;
    }

    boolean deleteImagesInCardProImagesDirectory(String folderPath){

        Path path = Paths.get(folderPath);

        if (!Files.exists(path)) {
            System.out.println("Directory does not exist: " + path);
            return false;
        }

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            log.error("ERROR: deleting current images directory {}", e.toString());
            return false;
            //throw new RuntimeException(e);
        }

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
            log.error("ERROR: creating new attachmentFileName() :: {}", client.getEmail());
            throw new PictureFileException("Failed creating Picture file name");
        }
    }

    public String zipCardProDataDirectory(String fileName) throws IOException {

        String userHome = System.getProperty("user.home");

        String currentYear = String.valueOf(LocalDate.now().getYear());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String dateToday = LocalDate.now().format(formatter);

        String sourceDir = "";
        String zipFilePath = "";

        if(TEST_ENV.equals(activeProfile)){
            sourceDir = "csw_files" + File.separator + "cardpro_files";
            zipFilePath = "csw_files" + File.separator + "Batch " + fileName + ".zip";
        }
        else{
            sourceDir = userHome + File.separator + "Downloads" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator +  "CardPro_Files";
            zipFilePath = userHome + File.separator + "Downloads" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator + "Batch " + fileName + ".zip";
        }

        Path sourceDirPath = Paths.get(sourceDir);
        Path zipFile = Paths.get(zipFilePath);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {

            Files.walk(sourceDirPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString().replace("\\", "/"));
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            log.error("Failed to Zip File");
                            throw new ZipFileException("Failed to zip CardPro folder");
                        }
                    });

        } catch (Exception e) {
            log.error("Failed creating Zip File");
            throw new ZipFileException("Failed to zip CardPro folder");
        }

        return zipFilePath;

    }

    @Override
    public ProcessedCardProClientsStats getCardProStats() {
        return processedCardProClientsStatsRepository.findTopByOrderByTransactionIdDesc().orElseThrow(() -> new ResourceNotFoundException("Could not find stats"));
    }

}
