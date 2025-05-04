package com.cosw.councilOfSocialWork.domain.trackingSheet.dto;

import java.util.UUID;

public record TrackingSheetClientDto(
        UUID id,
        String name,
        String surname,
        String registrationNumber,
        String practiceNumber,
        String email,
        String phoneNumber,
        String registrationDate,
        String registrationYear,
        String membershipStatus
){}
