package com.cosw.councilOfSocialWork.domain.trackingSheet.service;

import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import com.cosw.councilOfSocialWork.exception.ProcessingFileException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class FeatureRequestTrackingSheetExcelSheetGeneration {

    Environment environment;

    List<TrackingSheetClient> trackingSheetClientsList;
    CellStyle style;

    private static String TEST_ENV = "test";
    private static String DEV_ENV = "dev";
    private String activeProfile;

    public FeatureRequestTrackingSheetExcelSheetGeneration(List<TrackingSheetClient> trackingSheetClientsList) {
        this.trackingSheetClientsList = trackingSheetClientsList;
    }

    boolean createFile(){
        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Social Workers");
        style = workbook.createCellStyle();

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

        header.createCell(0).setCellValue("NAME");
        header.createCell(1).setCellValue("PHONE No.");
        header.createCell(2).setCellValue("REG NO");

    }

    void createRows(Sheet sheet){

        int i = 1;

        for(TrackingSheetClient trackingSheet: trackingSheetClientsList){

            populateCells(sheet.createRow(i), trackingSheet);
            i++;
        }

    }

    void populateCells(Row row, TrackingSheetClient trackingSheetClient){
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex()); // Light Yellow
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        row.createCell(0).setCellValue(trackingSheetClient.getName() + " " + trackingSheetClient.getSurname());
        row.createCell(1).setCellValue(trackingSheetClient.getPhoneNumber());
        row.createCell(2).setCellValue(trackingSheetClient.getRegistrationNumber());

    }

    void createThenSaveFile(Workbook workbook){

        String filename = "cardpro_renewals.xlsx";

        String currentYear = String.valueOf(LocalDate.now().getYear());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String dateToday = LocalDate.now().format(formatter);

        String userHome = "";
        String filePath = "";

        if(TEST_ENV.equals(activeProfile)){
            filePath =  "csw_files" + File.separator + "cardpro_files" + File.separator + filename;
        }
        else{
            userHome = System.getProperty("user.home");
            filePath = userHome + File.separator + "Downloads" + File.separator + "CWS Files" + File.separator + filename;
        }

        File file = new File(filePath);

        // Ensure directory exists
        file.getParentFile().mkdirs();

        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            workbook.close();
        } catch (IOException e) {
            log.error("ERROR creating/saving file {}", e.getMessage());
            throw new ProcessingFileException("Failed creating/saving CardPro excel file");
        }

    }

}
