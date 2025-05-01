package com.cosw.councilOfSocialWork.domain.trackingSheet.service;

import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import com.cosw.councilOfSocialWork.domain.trackingSheet.repository.TrackingSheetRepository;
import com.cosw.councilOfSocialWork.exception.ProcessingFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;

@Service
@Slf4j
public class TrackingSheetProcessingService {

    TrackingSheetRepository trackingSheetRepository;
    List<TrackingSheetClient> trackingSheetClientList;

    // membership status
    private final String MEMBERSHIP_ACTIVE = "active";

    public TrackingSheetProcessingService(TrackingSheetRepository trackingSheetRepository) {
        this.trackingSheetRepository = trackingSheetRepository;
    }

    boolean processTrackingSheet(){

        try {
            String activeYear = String.valueOf(Year.now().getValue());

            // get all active members for this year
            trackingSheetClientList = trackingSheetRepository.findByRegistrationYear(activeYear);

            log.info("Tracking sheet size: {}", trackingSheetClientList.size());

            // set all the emails of active members to ACTIVE MEMBERSHIP
            trackingSheetClientList.forEach( client -> trackingSheetRepository.updateMembershipStatus(client.getEmail(), MEMBERSHIP_ACTIVE));

            return true;

        } catch (Exception e) {
            log.error("ERROR: failed processing tracking sheet membership status {}", e.toString());
            throw new ProcessingFileException("Failed to process tracking sheet membership status doc");
        }
    }

}
