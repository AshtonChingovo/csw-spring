package com.cosw.councilOfSocialWork.domain.googleAuth.controller;

import com.cosw.councilOfSocialWork.domain.googleAuth.dto.TokenResponseDTO;
import com.cosw.councilOfSocialWork.domain.googleAuth.service.GoogleOAuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.sheets.v4.SheetsScopes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/oauth2")
@Slf4j
public class GoogleOAuthController {

    GoogleOAuthService googleOAuthService;

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";  // Downloaded from Google Cloud Console
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Arrays.asList(GmailScopes.GMAIL_MODIFY, SheetsScopes.SPREADSHEETS);
    private static final String REDIRECT_URI = "https://cswtest.site/api/v1/oauth2/callback";

    public GoogleOAuthController(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleGoogleCallback(@RequestParam("code") String code) throws Exception {

        googleOAuthService.createAndStoreToken(code);

        return ResponseEntity.ok("Google authorization successful! Tokens stored.");

    }

    @GetMapping("/token-check")
    public ResponseEntity<TokenResponseDTO> checkGoogleToken() throws Exception {
        return ResponseEntity.ok(googleOAuthService.checkTokenExistsOrReturnNewOAuthUrl());
    }

    GoogleAuthorizationCodeFlow getGoogleAuthorizationCodeFlow() throws GeneralSecurityException, IOException {

        HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        InputStream inputStream = getClass().getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(inputStream));

        return new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

    }

}
