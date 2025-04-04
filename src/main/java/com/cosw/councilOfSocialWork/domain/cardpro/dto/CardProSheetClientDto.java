package com.cosw.councilOfSocialWork.domain.cardpro.dto;

import com.cosw.councilOfSocialWork.domain.images.dto.ImageDto;

import java.util.Set;

public record CardProSheetClientDto(
        String name,
        String surname,
        String registrationNumber,
        String practiceNumber,
        String email,
        String dateOfExpiry,
        boolean hasDifferentEmail,
        boolean hasNoAttachment,
        Set<ImageDto> images
){}
