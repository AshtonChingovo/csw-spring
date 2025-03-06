package com.cosw.councilOfSocialWork.domain.cardpro.service;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface CardProService {

    void generateCardProData();

    void fetchEmails() throws IOException, GeneralSecurityException;

}
