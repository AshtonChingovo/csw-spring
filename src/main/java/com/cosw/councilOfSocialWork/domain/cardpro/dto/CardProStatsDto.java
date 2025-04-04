package com.cosw.councilOfSocialWork.domain.cardpro.dto;

import lombok.Builder;
import lombok.Data;

public record CardProStatsDto(
        Long totalClients,
        Integer totalReady,
        Integer totalHasDifferentEmail,
        Integer totalNoAttachmentFound
){}
