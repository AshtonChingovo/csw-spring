package com.cosw.councilOfSocialWork.domain.cardpro.dto;

public record CardProSheetClientDto(
        String name,
        String surname,
        String registrationNumber,
        String practiceNumber,
        String attachmentFileName,
        String attachmentPath,
        String dateOfExpiry
){}
