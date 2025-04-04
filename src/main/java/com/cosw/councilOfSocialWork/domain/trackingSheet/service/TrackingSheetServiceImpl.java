package com.cosw.councilOfSocialWork.domain.trackingSheet.service;

import com.cosw.councilOfSocialWork.domain.trackingSheet.dto.TrackingSheetClientDto;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import com.cosw.councilOfSocialWork.domain.trackingSheet.repository.TrackingSheetRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TrackingSheetServiceImpl implements TrackingSheetService {

    TrackingSheetRepository trackingSheetRepository;
    TrackingSheetClientMapper mapper;

    public TrackingSheetServiceImpl(TrackingSheetRepository trackingSheetRepository, TrackingSheetClientMapper mapper) {
        this.trackingSheetRepository = trackingSheetRepository;
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
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public FileInputStream convertToFileInputStream(MultipartFile multipartFile) {
        try {

            // create temp file in temp location
            File file = new File(System.getProperty("java.io.tmpdir") + "/" + multipartFile.getOriginalFilename());

            multipartFile.transferTo(file);

            return new FileInputStream(file);

        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
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

                log.info("DONE");

                return "COMPLETED";
            } else {
                return "FAILED";
            }
        } catch (InterruptedException e) {
            return "FAILED";
        }

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
                    .build();
        } catch (IllegalStateException e) {
            log.error("Sheet {}" + sheetName, row.getRowNum());
            return null;
            // throw new RuntimeException(e);
        }
    }

    @Override
    public Page<TrackingSheetClientDto> getTrackingSheet(int pageNumber, int pageSize, String sortBy) {
        Pageable page = PageRequest.of(pageNumber, pageSize, Sort.by(sortBy).descending());
        return trackingSheetRepository.findAll(page).map(mapper::trackingSheetClientToTrackingSheetClientDto);
    }

}
