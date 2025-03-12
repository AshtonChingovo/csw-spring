package com.cosw.councilOfSocialWork.domain.cardpro.service;

import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProSheetClientDto;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface CardProService {

    void generateCardProData();

    boolean createCardProData() throws IOException, GeneralSecurityException;

    Page<CardProSheetClientDto> getCardProSheet(int pageNumber, int pageSize, String sortBy);

    void generateThenDownloadCardProFile();

}
