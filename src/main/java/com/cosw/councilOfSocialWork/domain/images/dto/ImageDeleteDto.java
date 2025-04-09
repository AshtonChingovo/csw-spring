package com.cosw.councilOfSocialWork.domain.images.dto;

import java.util.UUID;

public record ImageDeleteDto(
        Long id,
        UUID clientId
){}
