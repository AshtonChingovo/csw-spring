package com.cosw.councilOfSocialWork.domain.trackingSheet.service;

import com.cosw.councilOfSocialWork.domain.trackingSheet.dto.TrackingSheetClientDto;
import com.cosw.councilOfSocialWork.domain.trackingSheet.dto.TrackingSheetStatsDto;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetStats;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface TrackingSheetService {

    boolean processTrackingSheet(MultipartFile file);

    Page<TrackingSheetClientDto> getTrackingSheet(int pageNumber, int pageSize, String sortBy, String search, String filter);

    TrackingSheetStatsDto getTrackingSheetStats();

    boolean processTrackingSheet();
}
