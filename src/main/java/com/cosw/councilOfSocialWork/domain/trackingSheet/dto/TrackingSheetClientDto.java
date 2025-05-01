package com.cosw.councilOfSocialWork.domain.trackingSheet.dto;

public record TrackingSheetClientDto(
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
