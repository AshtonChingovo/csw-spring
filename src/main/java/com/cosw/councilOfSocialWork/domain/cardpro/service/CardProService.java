package com.cosw.councilOfSocialWork.domain.cardpro.service;

import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProSheetClientDto;
import com.cosw.councilOfSocialWork.domain.cardpro.entity.ProcessedCardProClientsStats;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;

public interface CardProService {

    Page<CardProSheetClientDto> getCardProClients(int pageNumber, int pageSize, String sortBy, String search, String filter);

    boolean generateCardProData();

    Resource downloadCardProData(String batchNumber);

    ProcessedCardProClientsStats getCardProStats();

}
