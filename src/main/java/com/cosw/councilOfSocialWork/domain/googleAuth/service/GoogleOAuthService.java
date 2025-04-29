package com.cosw.councilOfSocialWork.domain.googleAuth.service;

import com.cosw.councilOfSocialWork.domain.googleAuth.dto.TokenResponseDTO;
import com.cosw.councilOfSocialWork.exception.GoogleOAuthException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.GmailScopes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GoogleOAuthService {

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";  // Downloaded from Google Cloud Console
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_MODIFY);
    private static final String REDIRECT_URI = "https://cswtest.site/api/oauth2/callback";

    private static String TEST_ENV = "test";
    private static String DEV_ENV = "dev";

    @Value("${spring.profiles.active}")
    private String activeProfile;

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

}
