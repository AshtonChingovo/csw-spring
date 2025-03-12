package com.cosw.councilOfSocialWork.domain.cardpro.service;

import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class GenerateThenDownloadCardProfile {

    List<CardProClient> cardProClientsList;

    public GenerateThenDownloadCardProfile(List<CardProClient> cardProClientsList) {
        this.cardProClientsList = cardProClientsList;
    }

    boolean createFile(){
        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Social Workers");

        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 4000);
        sheet.setColumnWidth(3, 4000);
        sheet.setColumnWidth(4, 4000);
        sheet.setColumnWidth(5, 8500);

        CellStyle headerStyle = workbook.createCellStyle();
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        setUpHeaders(sheet);
        createRows(sheet);
        // style header row
        sheet.getRow(0).setRowStyle(headerStyle);

        createThenSaveFile(workbook);

        return true;
    }

    void setUpHeaders(Sheet sheet){

        Row header = sheet.createRow(0);

        header.createCell(0).setCellValue("PROFESSION");

        header.createCell(1).setCellValue("NAME");

        header.createCell(2).setCellValue("REG NO");

        header.createCell(3).setCellValue("PRAC NO");

        header.createCell(4).setCellValue("DATE OF EXPIRY");

        header.createCell(5).setCellValue("PHOTO NAME");

    }

    void createRows(Sheet sheet){

        int i = 1;

        for(CardProClient cardProClient: cardProClientsList){
            populateCells(sheet.createRow(i), cardProClient);
            i++;
        }

    }

    void populateCells(Row row, CardProClient cardProClient){

        row.createCell(0).setCellValue(cardProClient.getProfession());

        row.createCell(1).setCellValue(cardProClient.getName() + " " + cardProClient.getSurname());

        row.createCell(2).setCellValue(cardProClient.getRegistrationNumber());

        row.createCell(3).setCellValue(cardProClient.getPracticeNumber());

        row.createCell(4).setCellValue(cardProClient.getDateOfExpiry());

        row.createCell(5).setCellValue(cardProClient.getAttachmentFileName());

    }

    void createThenSaveFile(Workbook workbook){

        String filename = "cardpro.xlsx";

        String currentYear = String.valueOf(LocalDate.now().getYear());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String dateToday = LocalDate.now().format(formatter);

        String userHome = System.getProperty("user.home");
        String filePath = userHome + File.separator + "Downloads" + File.separator  + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator + filename;

        File file = new File(filePath);

        // Ensure directory exists
        file.getParentFile().mkdirs();

        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            workbook.close();
        } catch (IOException e) {
            log.error("ERROR {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

}
