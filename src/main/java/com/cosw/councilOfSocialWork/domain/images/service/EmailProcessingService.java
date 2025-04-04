package com.cosw.councilOfSocialWork.domain.images.service;

import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import com.cosw.councilOfSocialWork.domain.cardpro.repository.CardProClientRepository;
import com.cosw.councilOfSocialWork.domain.cardpro.service.CardProServiceImpl;
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
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.io.*;
import java.lang.Thread;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class EmailProcessingService {

    private final CardProClientRepository cardProClientRepository;
    private TrackingSheetRepository trackingSheetRepository;

    private List<CardProClient> cardProClientList = new ArrayList<>();
    private ConcurrentHashMap<String, CardProClient> cardProClientConcurrentHashMap = new ConcurrentHashMap<>();

    private static final String APPLICATION_NAME = "cosw";
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_MODIFY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";  // Downloaded from Google Cloud Console

    public EmailProcessingService(CardProClientRepository cardProClientRepository, TrackingSheetRepository trackingSheetRepository) {
        this.cardProClientRepository = cardProClientRepository;
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

    @Transactional
    public List<CardProClient> createClientListAndDownloadImages() throws IOException, GeneralSecurityException {
        HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getEmailServerCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // ListMessagesResponse messagesResponse = service.users().messages().list("me").execute();

        List<Message> messages = new ArrayList<>();

        String nextPageToken = null;

        // pagination code for gmail
        do {
            // Fetch emails with pagination support
            ListMessagesResponse response = service.users().messages()
                    .list("me")
                    .setQ("is:unread") // Filter only unread emails
                    .setMaxResults(100L) // Maximum: 500 per request
                    .setPageToken(nextPageToken)
                    .execute();

            if (response.getMessages() != null) {
                messages.addAll(response.getMessages());
            }

            // Move to the next page
            nextPageToken = response.getNextPageToken();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.info("ERROR Threads {}", e.getMessage());
                throw new RuntimeException(e);
            }

        } while (nextPageToken != null);


        // Create a thread pool with 3 threads
        ExecutorService executor = Executors.newFixedThreadPool(5);

        var emailCounter = 0;
        var notInTrackingSheetCounter = 0;
        var emailsNoAttachmentCounter = 0;

        for(Message message : messages) {

            var hasDifferentEmail = false;
            var noAttachmentFound = false;

            // Message email = service.users().messages().get("me", message.getId()).execute();
            Message email = service.users().messages().get("me", message.getId()).execute();

            if(email.getPayload() == null)
                continue;

            var clientEmailAddress = extractClientEmailAddress(email);

            if(clientEmailAddress.isEmpty())
                continue;

            if("csw.identificationcards@gmail.com".equals(clientEmailAddress))
                continue;

            // there are multiple records in the tracking sheet with identical email address
            var client = trackingSheetRepository.findFirstByEmailOrderByRegistrationYearDesc(clientEmailAddress);

            if(client.isEmpty()){

                ++notInTrackingSheetCounter;

                var clientName = extractClientNameFromAddress(email).trim();

                var name = "";
                var surname = "";

                var lastIndexOfSpaceChar = clientName.lastIndexOf(" ") > -1 ? clientName.lastIndexOf(" ") : clientName.length();

                if(clientName.lastIndexOf(" ") > -1){
                    name = extractClientNameFromAddress(email).substring(0, lastIndexOfSpaceChar).trim();
                    surname = extractClientNameFromAddress(email).substring(clientName.lastIndexOf(" ")).trim();
                }
                // client does not have a surname
                else{
                    name = extractClientNameFromAddress(email).substring(0, lastIndexOfSpaceChar).trim();
                    surname = "";
                }

                // log.info("Client Not In TS:: {} {} - {}", name, surname, trackingSheetRepository.findFirstByNameAndSurnameOrderByRegistrationYearDesc(name, surname).isPresent());

                // find if a client exits with extracted name & surname since email is not on Tracking Sheet
                if(trackingSheetRepository.findFirstByNameAndSurnameOrderByRegistrationYearDesc(name, surname).isPresent()){
                    client = trackingSheetRepository.findFirstByNameAndSurnameOrderByRegistrationYearDesc(name, surname);
                    hasDifferentEmail = true;
                }
                else{
                    continue;
                }
            }

            try {
                var attachmentFilePath = extractThenDownloadAttachmentAndReturnAttachmentPath(email.getPayload(), message.getId(), service, client.get());

                if(!attachmentFilePath.isEmpty()){
                    cardProClientList.add(
                            CardProClient.builder()
                                    .email(clientEmailAddress)
                                    .name(client.get().getName())
                                    .surname(client.get().getSurname())
                                    .registrationNumber(client.get().getRegistrationNumber())
                                    .practiceNumber(client.get().getPracticeNumber())
                                    .profession("Registered Social Worker")
                                    .dateOfExpiry("31/12/" + LocalDate.now().getYear())
                                    // .attachmentFileName(attachmentFilePath.substring(attachmentFilePath.lastIndexOf(File.separator) + 1))
                                    // .attachmentPath(encodeAttachmentFilePath(attachmentFilePath))
                                    .hasDifferentEmail(hasDifferentEmail)
                                    .hasNoAttachment(false)
                                    .build());

                    ++emailCounter;
                    // log.info("Client Email :: {} - {}", emailCounter, clientEmailAddress);

                }
                else{
                    ++emailsNoAttachmentCounter;
                    if(!client.get().getPracticeNumber().isEmpty() && !client.get().getPracticeNumber().isEmpty())
                        cardProClientList.add(
                                CardProClient.builder()
                                        .email(clientEmailAddress)
                                        .name(client.get().getName())
                                        .surname(client.get().getSurname())
                                        .registrationNumber(client.get().getRegistrationNumber())
                                        .practiceNumber(client.get().getPracticeNumber())
                                        .profession("Registered Social Worker")
                                        .dateOfExpiry("31/12/" + LocalDate.now().getYear())
                                        //.attachmentFileName(attachmentFilePath.substring(attachmentFilePath.lastIndexOf(File.separator) + 1))
                                        //.attachmentPath(encodeAttachmentFilePath(attachmentFilePath))
                                        .hasDifferentEmail(hasDifferentEmail)
                                        .hasNoAttachment(true)
                                        .build());

                    // log.info("Client Email No Attachment:: {} - {}", emailsNoAttachmentCounter, clientEmailAddress);
                }
            } catch (IOException e) {
                log.info("ERROR Client Email :: {}", clientEmailAddress);
                ++emailCounter;
                // throw new RuntimeException(e);
            }

/*
            executor.execute(() -> {
                log.info("EmailProcessing <-> Starting");

                String attachmentFilePath;

                try {
                    attachmentFilePath = extractThenDownloadAttachmentAndReturnAttachmentPath(email.getPayload(), message.getId(), service, client.get());
                } catch (IOException e) {
                    log.info("ERROR {}", e.getMessage());
                    log.info("ERROR Client Email :: {}", clientEmailAddress);
                    throw new RuntimeException(e);
                }

                if(!attachmentFilePath.isEmpty()){
                    cardProClientConcurrentHashMap.put(clientEmailAddress,
                            CardProClient.builder()
                                    .email(clientEmailAddress)
                                    .name(client.get().getName())
                                    .surname(client.get().getSurname())
                                    .registrationNumber(client.get().getRegistrationNumber())
                                    .practiceNumber(client.get().getPracticeNumber())
                                    .profession("Registered Social Worker")
                                    .dateOfExpiry("31/12/" + LocalDate.now().getYear())
                                    .attachmentFileName(attachmentFilePath.substring(attachmentFilePath.lastIndexOf(File.separator) + 1))
                                    .attachmentPath(encodeAttachmentFilePath(attachmentFilePath))
                                    .build());

                    // Mark email as read
*/
/*                    try {
                        markEmailAsRead(service, message.getId());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }*//*
                }
            });
*/
        }

        log.info("EmailProcessing DONE <> emailCounter :{} notInTrackingSheetCounter :{} emailsNoAttachmentCounter:{}", emailCounter, notInTrackingSheetCounter, emailsNoAttachmentCounter);
        log.info("Clients size: {}", cardProClientList.size());

        cardProClientRepository.saveAll(cardProClientList);

        return cardProClientList;

      // Prevent new tasks from being submitted
/*        executor.shutdown();

        try {
            // Wait for all threads to finish (up to 10 minutes)
            if (executor.awaitTermination(10, TimeUnit.MINUTES)) {

                log.info("EmailProcessing <-> DONE");

            } else {
                return null;
            }
        } catch (InterruptedException e) {
            log.info("ERROR {}", e.getMessage());
            return null;
        }*/

        // return new ArrayList<>(cardProClientConcurrentHashMap.values());

    }

    public static void markEmailAsRead(Gmail service, String messageId) throws IOException {
        // Create a request to remove the "UNREAD" label
        ModifyMessageRequest modifyRequest = new ModifyMessageRequest()
                .setRemoveLabelIds(List.of("UNREAD"));

        // Apply the modification
        service.users().messages().modify("me", messageId, modifyRequest).execute();
    }

    public String encodeAttachmentFilePath(String filePath){
        String encodedPath = null;
        String userHome = System.getProperty("user.home");

        String currentYear = String.valueOf(LocalDate.now().getYear());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String dateToday = LocalDate.now().format(formatter);

        String baseUrl = "http://192.168.100.5";
        String baseFilePath = userHome + File.separator + "Downloads" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator +  "Images" + File.separator;

        encodedPath = URLEncoder.encode(filePath.substring(filePath.lastIndexOf(File.separator) + 1), StandardCharsets.UTF_8).replace("+", "%20");
        return baseUrl + baseFilePath + encodedPath;
    }

    public String extractClientEmailAddress(Message message){

/*        var clientEmailAddress = "";

        if (messagePart.getParts() != null) {
            for (MessagePart part : messagePart.getParts()) {
                if(part.getParts() != null){
                    clientEmailAddress = extractEmailFromMessagePart(part);
                }
            }
        }

        return !clientEmailAddress.isEmpty() ? clientEmailAddress : "N/A";*/

        // Extract the "From" header
        for (MessagePartHeader header : message.getPayload().getHeaders()) {
            if ("From".equalsIgnoreCase(header.getName())) {
                return header.getValue().substring(header.getValue().indexOf("<") + 1, header.getValue().indexOf(">")); // Returns sender email (e.g., "John Doe <john@example.com>")
            }
        }

        return "";

    }

    public String extractClientNameFromAddress(Message message){

        // Extract the "From" header
        for (MessagePartHeader header : message.getPayload().getHeaders()) {
            if ("From".equalsIgnoreCase(header.getName())) {
                return header.getValue().substring(0, header.getValue().indexOf("<")).trim(); // Returns sender email (e.g., "John Doe <john@example.com>")
            }
        }

        return "";

    }

    public String extractEmailFromMessagePart(Message message){

        // Extract the "From" header
        for (MessagePartHeader header : message.getPayload().getHeaders()) {
            if ("From".equalsIgnoreCase(header.getName())) {
                return header.getValue(); // Returns sender email (e.g., "John Doe <john@example.com>")
            }
        }

        return "";

    }

    public String extractThenDownloadAttachmentAndReturnAttachmentPath(MessagePart part, String messageId, Gmail service, TrackingSheetClient client) throws IOException {
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

        log.error("ERROR extractThenDownloadAttachmentAndReturnAttachmentPath() :: {}", client.getEmail());
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
            String filePath = userHome + File.separator + "Downloads" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator +  "Images" + File.separator + filename;
            File file = new File(filePath);

            // Ensure directory exists
            file.getParentFile().mkdirs();

            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(fileData);
            }
            catch (Exception e){
                log.error("Error {}", e.getMessage());
                return "";
            }

            return file.getAbsolutePath();
        }

        log.error("ERROR downloadAttachmentAndReturnNewFilePath() :: {}", client.getEmail());

        return "";
    }

    private String createNewAttachmentFileName(TrackingSheetClient client, String fileExtension){
        try {

            String[] usernames = client.getName().split(" ");
            StringBuilder fileName = new StringBuilder();

            for(String name: usernames){
                fileName.append(name).append(" ");
            }

            fileName.append(client.getSurname()).append(fileExtension);

            return fileName.toString();

        } catch (UsernameNotFoundException e) {
            log.error("Client not found");
            log.error("ERROR createNewAttachmentFileName() :: {}", client.getEmail());
            return "";
        }
    }

}
