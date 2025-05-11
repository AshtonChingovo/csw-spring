package com.cosw.councilOfSocialWork.domain.trackingSheet.service;

import com.cosw.councilOfSocialWork.domain.googleAuth.service.GoogleOAuthService;
import com.cosw.councilOfSocialWork.domain.trackingSheet.dto.GoogleTrackingSheetRenewalDto;
import com.cosw.councilOfSocialWork.domain.trackingSheet.dto.TrackingSheetClientDto;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetStats;
import com.cosw.councilOfSocialWork.domain.trackingSheet.repository.TrackingSheetRepository;
import com.cosw.councilOfSocialWork.domain.trackingSheet.repository.TrackingSheetStatsRepository;
import com.cosw.councilOfSocialWork.exception.GoogleTrackingSheetException;
import com.cosw.councilOfSocialWork.exception.ResourceNotFoundException;
import com.cosw.councilOfSocialWork.mapper.trackingSheet.TrackingSheetClientMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.Year;
import java.util.Arrays;

@Service
@Slf4j
public class GoogleSheetService {

    private static final String APPLICATION_NAME = "csw";
    private static final String SPREAD_SHEET_ID = "1IKV4deZ8LCMMIU2wKFh6YgisxJjclqVj8UpMYdXuzUI";
    private final String MEMBERSHIP_ACTIVE = "active";

    // google sheet columns
    String NAME_COLUMN = "A";
    String SURNAME_COLUMN = "B";
    String REG_NUMBER_COLUMN = "C";
    String PRAC_NUMBER_COLUMN = "D";
    String EMAIL_COLUMN = "E";
    String PHONE_NUMBER_COLUMN = "F";

    TrackingSheetRepository trackingSheetRepository;
    TrackingSheetStatsRepository trackingSheetStatsRepository;
    GoogleOAuthService googleOAuthService;
    TrackingSheetClientMapper mapper;

    public GoogleSheetService(TrackingSheetRepository trackingSheetRepository, TrackingSheetStatsRepository trackingSheetStatsRepository, GoogleOAuthService googleOAuthService, TrackingSheetClientMapper mapper) {
        this.trackingSheetRepository = trackingSheetRepository;
        this.trackingSheetStatsRepository = trackingSheetStatsRepository;
        this.googleOAuthService = googleOAuthService;
        this.mapper = mapper;
    }

    public Sheets getGoogleSheetsService(){

        Credential credential = googleOAuthService.getEmailServerCredentials_Dev();

        try {
            return new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(), credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (GeneralSecurityException e) {
            log.info("ERROR: getting googleSheetService {}", e.toString());
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.info("ERROR: IO getting googleSheetService {}", e.toString());
            throw new RuntimeException(e);
        }
    }

    public TrackingSheetClientDto renewClientInGoogleTrackingSheet(GoogleTrackingSheetRenewalDto googleTrackingSheetRenewalDto){

        String newClientPracticeNumber;
        var client = trackingSheetRepository
                .findById(googleTrackingSheetRenewalDto.id())
                .orElseThrow(() -> new ResourceNotFoundException("Could not find client in Tracking Sheet"));

        ValueRange body = new ValueRange()
                .setValues(Arrays.asList(
                        Arrays.asList(
                                client.getName(),
                                client.getSurname(),
                                client.getRegistrationNumber(),
                                client.getPracticeNumber(),
                                client.getEmail(),
                                !client.getPhoneNumber().isEmpty() ? client.getPhoneNumber() : ""
                        )
                ));

        AppendValuesResponse result = null;

        String currentYear = String.valueOf(LocalDate.now().getYear());

        try {

            // add new row
            result = getGoogleSheetsService().spreadsheets()
                    .values().append(SPREAD_SHEET_ID, "'" + currentYear + "'!" + NAME_COLUMN + ":" + PHONE_NUMBER_COLUMN, body)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .setIncludeValuesInResponse(true)
                    .execute();

            if(result.getUpdates().getUpdatedRange() == null || result.getUpdates().getUpdatedRows() == 0){
                throw new GoogleTrackingSheetException("Failed to renew client");
            }

            // table range format = "A1:J1864" get value of updated row line number
            var updatedTableRange = result.getTableRange();
            var updatedRow = Integer.valueOf(updatedTableRange.substring(updatedTableRange.lastIndexOf(":") + 2)) + 1;

            // append year to the creation of prac number
            newClientPracticeNumber = updatedRow + "/" + currentYear.substring(2);

            ValueRange practiceNumberUpdateBody = new ValueRange()
                    .setValues(Arrays.asList(Arrays.asList(newClientPracticeNumber)));

            // update client practice number
            // add new row
            UpdateValuesResponse practiceUpdateResult = getGoogleSheetsService().spreadsheets().values()
                    .update(SPREAD_SHEET_ID, "'" + currentYear + "'!" + PRAC_NUMBER_COLUMN + updatedRow, practiceNumberUpdateBody)
                    .setValueInputOption("RAW")
                    .setIncludeValuesInResponse(true)
                    .execute();

            if(practiceUpdateResult.getUpdatedRange() == null || practiceUpdateResult.getUpdatedRows() == 0){
                throw new GoogleTrackingSheetException("Failed to update client practice number");
            }

            // update all records with same email in db
            trackingSheetRepository.updateMembershipStatus(client.getEmail(), MEMBERSHIP_ACTIVE);

            // save new active membership current year client i.e. add client into this year's tracking sheet
            trackingSheetRepository.save(createNewTrackingSheetClient(client, newClientPracticeNumber, currentYear, MEMBERSHIP_ACTIVE));

            // update tracking sheet stats
            updateTrackingSheetStats();

            var updatedClient = trackingSheetRepository.findById(client.getId()).orElseThrow(() -> new ResourceNotFoundException("Could find TrackingSheetClient"));

            log.info("UPDATED CLIENT: {}", updatedClient.getMembershipStatus());
            log.info("UPDATED CLIENT: {}", updatedClient.getMembershipStatus());

            // return updated client
            return mapper.trackingSheetClientToTrackingSheetClientDto(updatedClient);

        } catch (IOException e) {
            log.info("ERROR: writing to google sheet {}", e.toString());
            throw new RuntimeException(e);
        }

    }

    TrackingSheetClient createNewTrackingSheetClient(TrackingSheetClient client, String practiceNumber, String currentYear, String membershipStatus){

        return TrackingSheetClient.builder()
                .name(client.getName())
                .surname(client.getSurname())
                .registrationNumber(client.getRegistrationNumber())
                .practiceNumber(practiceNumber)
                .email(client.getEmail())
                .phoneNumber(client.getPhoneNumber())
                .registrationDate(client.getRegistrationDate())
                .registrationYear(client.getRegistrationYear())
                .sheetYear(currentYear)
                .membershipStatus(membershipStatus)
                .build();
    }

    void updateTrackingSheetStats(){

        String activeYear = String.valueOf(Year.now().getValue());

        TrackingSheetStats trackingSheetStats = trackingSheetStatsRepository.findFirstByOrderByIdAsc().orElseThrow(() -> new ResourceNotFoundException("Failed to get T.S stats"));

        long totalActiveMembers = trackingSheetRepository.countBySheetYear(activeYear);

        trackingSheetStats.setTotalRenewed((int) totalActiveMembers);
        trackingSheetStatsRepository.save(trackingSheetStats);
    }

}
