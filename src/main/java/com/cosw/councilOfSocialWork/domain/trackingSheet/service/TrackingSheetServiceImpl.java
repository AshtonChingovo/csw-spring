package com.cosw.councilOfSocialWork.domain.trackingSheet.service;

import com.cosw.councilOfSocialWork.domain.cardpro.service.CardProExcelSheetGeneration;
import com.cosw.councilOfSocialWork.domain.trackingSheet.dto.TrackingSheetClientDto;
import com.cosw.councilOfSocialWork.domain.trackingSheet.dto.TrackingSheetStatsDto;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetStats;
import com.cosw.councilOfSocialWork.domain.trackingSheet.repository.TrackingSheetRepository;
import com.cosw.councilOfSocialWork.domain.trackingSheet.repository.TrackingSheetStatsRepository;
import com.cosw.councilOfSocialWork.exception.ProcessingFileException;
import com.cosw.councilOfSocialWork.exception.ResourceNotFoundException;
import com.cosw.councilOfSocialWork.mapper.trackingSheet.TrackingSheetClientMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TrackingSheetServiceImpl implements TrackingSheetService {

    TrackingSheetRepository trackingSheetRepository;
    TrackingSheetProcessingService trackingSheetProcessingService;
    TrackingSheetStatsRepository trackingSheetStatsRepository;
    TrackingSheetClientMapper mapper;

    private final String FILTER_BY_ALL = "all";
    private final String FILTER_BY_ACTIVE_MEMBERSHIP = "active_membership";
    private final String FILTER_BY_RENEWAL_DUE = "renewal_due";

    // default membership
    private String MEMBERSHIP_DUE_RENEWAL = "renewal_due";

    public TrackingSheetServiceImpl(TrackingSheetRepository trackingSheetRepository, TrackingSheetProcessingService trackingSheetProcessingService,
                                    TrackingSheetStatsRepository trackingSheetStatsRepository, TrackingSheetClientMapper mapper) {
        this.trackingSheetRepository = trackingSheetRepository;
        this.trackingSheetProcessingService = trackingSheetProcessingService;
        this.trackingSheetStatsRepository = trackingSheetStatsRepository;
        this.mapper = mapper;
    }

    @Override
    public boolean processTrackingSheet(MultipartFile multipartFile) {

        try {

            var inputStream = convertToFileInputStream(multipartFile);

            if (inputStream != null)
                extractAndSaveUsersFromTrackingSheetExcelFile(inputStream);

            return true;

        } catch (FileNotFoundException e) {
            log.error("ERROR file not found exception");
            return false;
        } catch (IOException e) {
            log.error("ERROR processing tracking sheet file {}", e.toString());
            throw new ProcessingFileException("Failed to process file");
        }

    }

    public FileInputStream convertToFileInputStream(MultipartFile multipartFile) {
        try {

            // create temp file in temp location
            File file = new File(System.getProperty("java.io.tmpdir") + "/" + multipartFile.getOriginalFilename());

            multipartFile.transferTo(file);

            return new FileInputStream(file);

        } catch (FileNotFoundException e) {
            log.error("ERROR file not found exception convertToFileInputStream()");
            return null;
        } catch (IOException e) {
            log.error("ERROR working on tracking sheet file convertToFileInputStream() {}", e.toString());
            throw new ProcessingFileException("Failed to process file");
        }

    }

    @Transactional
    public String extractAndSaveUsersFromTrackingSheetExcelFile(FileInputStream file) throws IOException {

        // Create a thread pool with 3 threads
        ExecutorService executor = Executors.newFixedThreadPool(5);

        Workbook trackingSheet = new XSSFWorkbook(file);
        List<TrackingSheetClient> clientList = new ArrayList<>();

        // iterate through Excel file sheets
        for (Sheet sheet : trackingSheet) {
            executor.execute(() -> {

                int firstRow = sheet.getFirstRowNum();
                int lastRow = sheet.getLastRowNum();

                // iterate through Excel file rows
                for (int i = firstRow; i <= lastRow; i++) {
                    // name is blank skip value
                    if (sheet.getRow(i) == null || sheet.getRow(i).getCell(0) == null || sheet.getRow(i).getCell(0).getStringCellValue().isBlank())
                        continue;

                    var client = convertRowToClient(sheet.getRow(i), sheet.getSheetName());
                    if (client != null)
                        clientList.add(client);

                }
            });
        }

        log.info("PROCESSING");

        // Prevent new tasks from being submitted
        executor.shutdown();

        try {
            // Wait for all threads to finish (up to 10 minutes)
            if (executor.awaitTermination(10, TimeUnit.MINUTES)) {

                // delete current tracking sheet data & persist latest data
                trackingSheetRepository.deleteAll();

                // save client list
                trackingSheetRepository.saveAll(clientList);

                // save stats record
                trackingSheetStatsRepository.deleteAll();
                trackingSheetStatsRepository.save(
                        TrackingSheetStats.builder()
                                .totalClients(countUniqueEmails(clientList))
                                .build()
                );

                log.info("DONE");

                return "COMPLETED";
            } else {
                return "FAILED";
            }
        } catch (InterruptedException e) {
            log.error("ERROR extractAndSaveUsersFromTrackingSheetExcelFile() {}", e.toString());
            throw new ProcessingFileException("Failed to process file");
            // return "FAILED";
        }

    }

    public static int countUniqueEmails(List<TrackingSheetClient> clientList) {
        return clientList.stream()
                .filter(client -> client.getEmail() != null) // filter out null emails
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                TrackingSheetClient::getEmail,  // key: email
                                Function.identity(),            // value: original object
                                (existing, replacement) -> existing // if duplicates, keep the first one
                        ),
                        map -> new ArrayList<>(map.values())
                )).size();
    }

    public TrackingSheetClient convertRowToClient(Row row, String sheetName) {

        try {
            var registrationNumber = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getCellType() == CellType.STRING ? row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue() : String.valueOf(row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getNumericCellValue());
            var practiceNumber = row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getCellType() == CellType.STRING ? row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue() : String.valueOf(row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getNumericCellValue());
            var email = row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getCellType() == CellType.STRING ? row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue() : String.valueOf(row.getCell(4, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getRichStringCellValue());
            var phoneNumber = row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getCellType() == CellType.STRING ? row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue() : String.valueOf(row.getCell(5, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getNumericCellValue());
            var registrationDate = row.getCell(6, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getCellType() == CellType.STRING ? row.getCell(6, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue() : String.valueOf(row.getCell(6, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getNumericCellValue());

            if (email.isEmpty())
                email = "N/A";

            if (registrationNumber.equals("0.0"))
                registrationNumber = "N/A";

            if (practiceNumber.equals("0.0"))
                practiceNumber = "N/A";

            return TrackingSheetClient.builder()
                    .name(row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue())
                    .surname(row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue())
                    .registrationNumber(registrationNumber)
                    .practiceNumber(practiceNumber)
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .registrationDate(registrationDate)
                    .registrationYear(sheetName)
                    .membershipStatus(MEMBERSHIP_DUE_RENEWAL)
                    .build();

        } catch (IllegalStateException e) {
            log.error("ERROR tracking sheet convertRowToClient() {} {} <-> {}", sheetName, row.getRowNum(), e.toString());
            return null;
        }
    }

    @Override
    public Page<TrackingSheetClientDto> getTrackingSheet(int pageNumber, int pageSize, String sortBy, String search, String filter) {
        Pageable page = PageRequest.of(pageNumber, pageSize, Sort.by(sortBy).descending());

        Page<TrackingSheetClient> trackingSheetClients;

        if(search.isEmpty() && FILTER_BY_ALL.equals(filter))
            trackingSheetClients = trackingSheetRepository.findAll(page);
        else
            trackingSheetClients = fetchFilteredResult(page, search, filter);

        return trackingSheetClients.map(mapper::trackingSheetClientToTrackingSheetClientDto);
    }

    @Override
    public TrackingSheetStatsDto getTrackingSheetStats() {
        return trackingSheetStatsRepository
                .findFirstBy()
                .map(mapper::trackingSheetStatsToTrackingSheetStatsDto)
                .orElseThrow(() -> new ResourceNotFoundException("Could not find any stats"));
    }

    @Override
    public boolean processTrackingSheet() {
        return trackingSheetProcessingService.processTrackingSheet();
    }

    @Override
    public boolean generatePhoneNumberCardProSheet() {

        new TrackingSheetExcelSheetGeneration(trackingSheetRepository.findAllWithRegistrationYear("/25", "2025")).createFile();

        return true;
    }

    public Page<TrackingSheetClient> fetchFilteredResult(Pageable page, String searchParam, String filterParam){

        String activeYear = String.valueOf(Year.now().getValue());

        if(!searchParam.isEmpty()){
            return switch (filterParam) {
                case FILTER_BY_ALL -> trackingSheetRepository.searchClients(searchParam, page);
                case FILTER_BY_ACTIVE_MEMBERSHIP -> trackingSheetRepository.searchClientsByActiveMembership(searchParam, activeYear, page);
                case FILTER_BY_RENEWAL_DUE -> trackingSheetRepository.searchClientsByDueRenewal(searchParam, activeYear, page);
                default -> trackingSheetRepository.findAll(page);
            };
        }
        else{
            return switch (filterParam) {
                case FILTER_BY_ALL -> trackingSheetRepository.searchClients(searchParam, page);
                case FILTER_BY_ACTIVE_MEMBERSHIP -> trackingSheetRepository.findClientsByActiveMembership(activeYear, page);
                case FILTER_BY_RENEWAL_DUE -> trackingSheetRepository.findClientsByDueRenewal(MEMBERSHIP_DUE_RENEWAL, activeYear, page);
                default -> trackingSheetRepository.findAll(page);
            };
        }
    }

}
