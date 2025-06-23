package com.cosw.councilOfSocialWork.domain.cardpro.dto;

import com.cosw.councilOfSocialWork.domain.images.dto.ImageDto;
import java.util.Set;
import java.util.UUID;

public record CardProSheetClientDto(
        UUID id,
        String name,
        String surname,
        String registrationNumber,
        String practiceNumber,
        String registrationYear,
        String sheetYear,
        String email,
        String dateOfExpiry,
        boolean notInTrackingSheet,
        boolean hasDifferentEmail,
        boolean hasNoAttachment,
        Set<ImageDto> images
){}
