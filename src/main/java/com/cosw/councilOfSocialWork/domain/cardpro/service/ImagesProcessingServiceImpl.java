package com.cosw.councilOfSocialWork.domain.cardpro.service;

import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import com.cosw.councilOfSocialWork.exception.ProcessingFileException;
import com.cosw.councilOfSocialWork.util.stringUtil.ProfileNamesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.cosw.councilOfSocialWork.util.stringUtil.NamesUtil.*;

@Slf4j
public class CardProExcelSheetGenerationHelper {

    List<CardProClient> cardProClientsList;
    CellStyle style;

    private String activeProfile;

    public CardProExcelSheetGenerationHelper(List<CardProClient> cardProClientsList, String activeProfile) {
        this.cardProClientsList = cardProClientsList;
        this.activeProfile = activeProfile;
    }

    boolean createFile(){
        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet(SOCIAL_WORKERS_FILENAME);
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
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex()); // Light Yellow
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        row.createCell(0).setCellValue(cardProClient.getProfession());
/*        if(cardProClient.isHasDifferentEmail()){
            style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex()); // Light Yellow
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            row.getCell(0).setCellStyle(style);
        }
        if(cardProClient.isHasNoAttachment()){
            style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            row.getCell(0).setCellStyle(style);
        }*/

        row.createCell(1).setCellValue(cardProClient.getName() + " " + cardProClient.getSurname());
        row.createCell(2).setCellValue(cardProClient.getRegistrationNumber());
        row.createCell(3).setCellValue(cardProClient.getPracticeNumber());
        row.createCell(4).setCellValue(cardProClient.getDateOfExpiry());
        row.createCell(5).setCellValue(getAttachmentFileName(cardProClient));

    }

    private String getAttachmentFileName(CardProClient client){
        try {

            String[] usernames = client.getName().split(" ");
            StringBuilder fileName = new StringBuilder();

            for(String name: usernames){
                fileName.append(name).append(" ");
            }

            fileName.append(client.getSurname());

            return fileName.toString();

        } catch (UsernameNotFoundException e) {
            return "";
        }
    }

    void createThenSaveFile(Workbook workbook){

        String filename = CARDPRO_FILENAME;

        String currentYear = String.valueOf(LocalDate.now().getYear());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String dateToday = LocalDate.now().format(formatter);

        String userHome = "";
        String filePath = "";

        if(ProfileNamesUtil.TEST_ENV.equals(activeProfile)){
            filePath =  CSW_FOLDER + File.separator + CARDPRO_FOLDER + File.separator + filename;
        }
        else{
            userHome = System.getProperty(HOME_SERVER_FOLDER);
            filePath = userHome + File.separator + DOWNLOADS_SERVER_FOLDER + File.separator + CSW_SERVER_FOLDER + File.separator + currentYear + File.separator + dateToday + File.separator + CARDPRO_SERVER_FOLDER + File.separator + filename;
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
