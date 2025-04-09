package com.cosw.councilOfSocialWork.domain.cardpro.dto;

public record CardProStatsDto(
        Long totalClients,
        Integer totalReady,
        Integer totalHasDifferentEmail,
        Integer totalNoAttachmentFound
){}
