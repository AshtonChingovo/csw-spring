package com.cosw.councilOfSocialWork.domain.cardpro.service;

import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import com.cosw.councilOfSocialWork.domain.trackingSheet.repository.TrackingSheetRepository;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class EmailProcessingService {

    private TrackingSheetRepository trackingSheetRepository;

    private List<CardProClient> cardProClientList = new ArrayList<>();

    private static final String APPLICATION_NAME = "CSW Email Service";
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_MODIFY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";  // Downloaded from Google Cloud Console

    public EmailProcessingService(TrackingSheetRepository trackingSheetRepository) {
        this.trackingSheetRepository = trackingSheetRepository;
    }

    public static Credential getEmailServerCredentials(final HttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream inputStream = CardProServiceImpl.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(inputStream));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public List<CardProClient> createClientListAndDownloadImages() throws IOException, GeneralSecurityException {
        HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getEmailServerCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        ListMessagesResponse messagesResponse = service.users().messages().list("me").execute();
        List<Message> messages = messagesResponse.getMessages();

        for (Message message : messages) {
            Message email = service.users().messages().get("me", message.getId()).execute();

            if(email.getPayload() != null){
                var clientEmailAddress = extractClientEmailAddress(email.getPayload());
                if(!clientEmailAddress.isEmpty()){
                    var client = trackingSheetRepository.findFirstByEmailOrderByRegistrationYearDesc(clientEmailAddress);
                    if(client.isPresent()){
                        var attachmentFilePath = extractAttachmentDownloadAndReturnAttachmentPath(email.getPayload(), message.getId(), service, client.get());
                        if(!attachmentFilePath.isEmpty()){
                            cardProClientList.add(
                                    CardProClient.builder()
                                            .email(clientEmailAddress)
                                            .name(client.get().getName())
                                            .surname(client.get().getSurname())
                                            .registrationNumber(client.get().getRegistrationNumber())
                                            .practiceNumber(client.get().getPracticeNumber())
                                            .attachmentPath(attachmentFilePath)
                                            .build()
                            );
                        }
                    }
                }
            }
        }

        return cardProClientList;

    }

    public String extractClientEmailAddress(MessagePart messagePart){

        var clientEmailAddress = "";

        if (messagePart.getParts() != null) {
            for (MessagePart part : messagePart.getParts()) {
                if(part.getParts() != null){
                    clientEmailAddress = extractEmailFromMessagePart(part);
                }
            }
        }

        return !clientEmailAddress.isEmpty() ? clientEmailAddress : "N/A";

    }

    public String extractEmailFromMessagePart(MessagePart part){

        var email = "";

        for(MessagePart emailMessagePart: part.getParts()){
            if(emailMessagePart.getMimeType().equals("text/plain")){
                var emailBody = new String(Base64.getUrlDecoder().decode(emailMessagePart.getBody().getData()), StandardCharsets.UTF_8);
                email = emailBody.substring(emailBody.indexOf("<") + 1, emailBody.indexOf(">"));
            }
        }

        return email;
    }

    public String extractAttachmentDownloadAndReturnAttachmentPath(MessagePart part, String messageId, Gmail service, TrackingSheetClient client) throws IOException {
        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                if (subPart.getFilename() != null && !subPart.getFilename().isEmpty()) {
                    return downloadAttachmentAndReturnNewFilePath(service, messageId, subPart, client);
                }
            }
        }
        else if (part.getBody() != null && part.getFilename() != null && !part.getFilename().isEmpty()) {
            return downloadAttachmentAndReturnNewFilePath(service, messageId, part, client);
        }

        return "";

    }

    private String downloadAttachmentAndReturnNewFilePath(Gmail service, String messageId, MessagePart part, TrackingSheetClient client) throws IOException {

        String fileExtension = part.getFilename().substring(part.getFilename().lastIndexOf("."));
        String filename = createNewAttachmentFileName(client, fileExtension);
        String attachmentId = part.getBody().getAttachmentId();

        if (attachmentId != null && !filename.isEmpty()) {

            String currentYear = String.valueOf(LocalDate.now().getYear());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
            String dateToday = LocalDate.now().format(formatter);

            MessagePartBody attachment = service.users().messages().attachments()
                    .get("me", messageId, attachmentId)
                    .execute();

            // Get the attachment data
            String base64Data = attachment.getData();

            // If the Base64 data contains URL-safe encoding, replace "_" with "/" and "-" with "+"
            String base64 = base64Data.replace('_', '/').replace('-', '+');

            // Decode the data
            byte[] fileData = Base64.getDecoder().decode(base64);

            String userHome = System.getProperty("user.home");
            String filePath = userHome + File.separator + "Downloads" + File.separator + currentYear + File.separator + dateToday + File.separator + filename;
            File file = new File(filePath);

            // Ensure directory exists
            file.getParentFile().mkdirs();

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(fileData);
            }
            catch (Exception e){
                log.error("Error {}", e.getMessage());
                return "";
            }

            return file.getAbsolutePath();
        }

        return "";
    }

    private String createNewAttachmentFileName(TrackingSheetClient client, String fileExtension){
        try {

            String[] usernames = client.getName().split(" ");
            StringBuilder fileName = new StringBuilder();

            for(String name: usernames){
                fileName.append(name).append("_");
            }

            fileName.append(client.getSurname()).append(fileExtension);

            return fileName.toString();

        } catch (UsernameNotFoundException e) {
            log.error("Client not found");
            return "";
        }
    }

}
