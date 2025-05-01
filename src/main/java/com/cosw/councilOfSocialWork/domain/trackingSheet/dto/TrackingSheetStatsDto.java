package com.cosw.councilOfSocialWork.domain.trackingSheet.dto;

public record TrackingSheetStatsDto(
        Integer totalClients,
        Integer renewal,
        Integer dueRenewal
){}
