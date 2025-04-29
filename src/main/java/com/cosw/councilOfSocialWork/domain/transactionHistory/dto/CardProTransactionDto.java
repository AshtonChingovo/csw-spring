package com.cosw.councilOfSocialWork.domain.transactionHistory.dto;

import java.util.UUID;

public record CardProTransactionDto(
        UUID id,
        String date,
        Integer totalEmails,
        String user
){}
