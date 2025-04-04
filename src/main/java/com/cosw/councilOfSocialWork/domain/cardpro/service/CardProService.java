package com.cosw.councilOfSocialWork.domain.cardpro.service;

import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProSheetClientDto;
import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProStatsDto;
import org.springframework.data.domain.Page;

public interface CardProService {

    Page<CardProSheetClientDto> getCardProClients(int pageNumber, int pageSize, String sortBy);

    boolean generateCardProData();

    CardProStatsDto getCardProStats();

}
