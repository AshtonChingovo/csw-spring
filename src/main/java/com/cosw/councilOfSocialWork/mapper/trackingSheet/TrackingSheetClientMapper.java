package com.cosw.councilOfSocialWork.mapper.trackingSheet;

import com.cosw.councilOfSocialWork.domain.trackingSheet.dto.TrackingSheetClientDto;
import com.cosw.councilOfSocialWork.domain.trackingSheet.dto.TrackingSheetStatsDto;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetStats;
import com.cosw.councilOfSocialWork.mapper.config.IgnoreUnmappedPropertiesConfig;
import org.mapstruct.Mapper;
import org.springframework.context.annotation.Configuration;

@Configuration
@Mapper(componentModel = "spring", config = IgnoreUnmappedPropertiesConfig.class)
public interface TrackingSheetClientMapper {

    TrackingSheetClientDto trackingSheetClientToTrackingSheetClientDto(TrackingSheetClient trackingSheetClient);

    TrackingSheetStatsDto trackingSheetStatsToTrackingSheetStatsDto(TrackingSheetStats trackingSheetStats);

}

