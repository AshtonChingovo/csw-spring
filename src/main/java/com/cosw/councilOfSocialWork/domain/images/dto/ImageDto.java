package com.cosw.councilOfSocialWork.domain.images.dto;

import java.util.UUID;

public record ImageDto(
        Long id,
        UUID cardProClientId,
        String attachmentFileName,
        String attachmentPath,
        String croppedPath,
        Boolean cropped,
        Boolean deleted
){}
