package com.cosw.councilOfSocialWork.domain.googleAuth.service;

import com.cosw.councilOfSocialWork.domain.googleAuth.dto.TokenResponseDTO;
import com.cosw.councilOfSocialWork.domain.images.service.ForwardedEmailProcessingService;
import com.cosw.councilOfSocialWork.exception.GoogleOAuthException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.DataStoreCredentialRefreshListener;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.sheets.v4.SheetsScopes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GoogleOAuthService {

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";  // Downloaded from Google Cloud Console
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Arrays.asList(GmailScopes.GMAIL_MODIFY, SheetsScopes.SPREADSHEETS);
    private static final String REDIRECT_URI = "https://cswtest.site/api/api/v1/oauth2/callback";

    private static final String CREDENTIALS_FILE_PATH_TEST = "/credentials.json";  // Downloaded from Google Cloud Console
    private static final String CREDENTIALS_FILE_PATH_DEV = "/credentials_dev.json";  // Downloaded from Google Cloud Console

    private static String redirectUri = "https://cswtest.site/api/api/v1/oauth2/callback";

    private static String TEST_ENV = "test";
    private static String DEV_ENV = "dev";

    @Value("${spring.profiles.active}")
    private static String activeProfile;

    public TokenResponseDTO checkToken(){

        // skip oauth during test
        if(activeProfile.equals(DEV_ENV))
            return TokenResponseDTO.builder()
                    .redirectUrl("Success")
                    .build();

        try{
            Credential credential = getGoogleAuthorizationCodeFlow().loadCredential("user");

            if(credential == null || credential.getAccessToken() == null){

                GoogleAuthorizationCodeFlow flow = getGoogleAuthorizationCodeFlow();

                var authURL = flow.newAuthorizationUrl()
                        .setRedirectUri(REDIRECT_URI)
                        .build();

                // auth URL
                return TokenResponseDTO.builder()
                        .redirectUrl(authURL)
                        .build();
            }

            return TokenResponseDTO.builder()
                    .redirectUrl("Success")
                    .build();
        }
         catch (GeneralSecurityException e) {
            log.info("ERROR: creating/loading Credential {}", e.toString());
            throw new GoogleOAuthException("Failed to authenticate with email account");
        }
        catch (IOException io){
            log.info("ERROR: creating/loading/requesting new auth with GoogleAuth {}", io.toString());
            throw new GoogleOAuthException("Failed to find Google Token");
        }

    }

    public boolean getToken(String code){

        try{

            GoogleAuthorizationCodeFlow flow = getGoogleAuthorizationCodeFlow();

            GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri(REDIRECT_URI)
                    .execute();

            flow.createAndStoreCredential(tokenResponse, "user");

            Credential credential = flow.loadCredential("user");

            if(credential != null)
                return true;
            else
                return false;
        }
        catch (Exception e) {
            log.info("ERROR: GoogleAuthorizationCodeFlow getting token {}", e.toString());
            throw new GoogleOAuthException("Failed to authenticate with email account");
        }
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
    
/*
    public Credential getGoogleCredentials(){

        try {
            HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            if(activeProfile.equals(TEST_ENV))
                return getEmailServerCredentials_Test(HTTP_TRANSPORT);
            else
                return getEmailServerCredentials_Dev(HTTP_TRANSPORT);

        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public static Credential getEmailServerCredentials_Test(final HttpTransport HTTP_TRANSPORT) throws IOException {

        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH));
        InputStream inputStream = ForwardedEmailProcessingService.class.getResourceAsStream(CREDENTIALS_FILE_PATH_TEST);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(inputStream));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .addRefreshListener(new DataStoreCredentialRefreshListener("user", dataStoreFactory))
                .build();

        Credential credential = flow.loadCredential("user");

        if (credential == null || credential.getAccessToken() == null) {

            // This is the important part: manually initiate auth URL with custom redirect
            String authUrl = flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .build();

            log.info("🔐 Please authorize the app by visiting:\n {}", authUrl);

            throw new IllegalStateException("Authorization required. Visit the URL above.");

        }
        else{
            log.info("Access Token: {}", credential.getAccessToken());
            log.info("Refresh Token: {}", credential.getRefreshToken());
        }

        return credential;
    }

    public static Credential getEmailServerCredentials_Dev(final HttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream inputStream = ForwardedEmailProcessingService.class.getResourceAsStream(CREDENTIALS_FILE_PATH_DEV);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(inputStream));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }*/


}
