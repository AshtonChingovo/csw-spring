package com.cosw.councilOfSocialWork.domain.images.dto;

public record ImageDto(
        int id,
        String attachmentFileName,
        String attachmentPath,
        Boolean cropped,
        Boolean deleted
){}
