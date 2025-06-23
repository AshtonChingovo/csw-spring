package com.cosw.councilOfSocialWork.domain.trackingSheet.service;

import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetStats;
import com.cosw.councilOfSocialWork.domain.trackingSheet.repository.TrackingSheetRepository;
import com.cosw.councilOfSocialWork.domain.trackingSheet.repository.TrackingSheetStatsRepository;
import com.cosw.councilOfSocialWork.exception.ProcessingFileException;
import com.cosw.councilOfSocialWork.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MembershipStatusProcessingService {

    TrackingSheetRepository trackingSheetRepository;
    TrackingSheetStatsRepository trackingSheetStatsRepository;
    List<String> activeMembersEmailList;

    // membership status
    private final String MEMBERSHIP_ACTIVE = "active";

    public MembershipStatusProcessingService(TrackingSheetRepository trackingSheetRepository, TrackingSheetStatsRepository trackingSheetStatsRepository) {
        this.trackingSheetRepository = trackingSheetRepository;
        this.trackingSheetStatsRepository = trackingSheetStatsRepository;
    }

    @Transactional
    boolean processTrackingSheetClientMembershipStatus(){

        try {
            String activeYear = String.valueOf(Year.now().getValue());

            // get all active members for this year
            activeMembersEmailList = trackingSheetRepository.findBySheetYear(activeYear).stream().map(TrackingSheetClient::getEmail).collect(Collectors.toList());

            // set all the emails of active members to ACTIVE MEMBERSHIP
            // activeMembersEmailList.forEach(client -> trackingSheetRepository.updateMembershipStatus(client.getEmail(), MEMBERSHIP_ACTIVE));

            trackingSheetRepository.bulkUpdateMembershipStatus(activeMembersEmailList, MEMBERSHIP_ACTIVE);

            // update tracking sheet stats
            updateTrackingSheetStats();

            return true;

        } catch (Exception e) {
            log.error("ERROR: failed processing tracking sheet membership status {}", e.toString());
            throw new ProcessingFileException("Failed to process tracking sheet membership status doc");
        }
    }

    void updateTrackingSheetStats(){

        String activeYear = String.valueOf(Year.now().getValue());

        TrackingSheetStats trackingSheetStats = trackingSheetStatsRepository.findFirstByOrderByIdAsc().orElseThrow(() -> new ResourceNotFoundException("Failed to get T.S stats"));

        long totalActiveMembers = trackingSheetRepository.countBySheetYear(activeYear);

        trackingSheetStats.setTotalRenewed((int) totalActiveMembers);
        trackingSheetStatsRepository.save(trackingSheetStats);

    }

}
