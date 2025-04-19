package com.cosw.councilOfSocialWork.domain.images.service;

import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import com.cosw.councilOfSocialWork.domain.cardpro.entity.ProcessedCardProClientsStats;
import com.cosw.councilOfSocialWork.domain.cardpro.repository.CardProClientRepository;
import com.cosw.councilOfSocialWork.domain.cardpro.repository.ProcessedCardProClientsStatsRepository;
import com.cosw.councilOfSocialWork.domain.cardpro.service.CardProServiceImpl;
import com.cosw.councilOfSocialWork.domain.images.dto.ImageDeleteDto;
import com.cosw.councilOfSocialWork.domain.images.dto.ImageDto;
import com.cosw.councilOfSocialWork.domain.images.entity.Image;
import com.cosw.councilOfSocialWork.domain.images.repository.ImagesRepository;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import com.cosw.councilOfSocialWork.domain.trackingSheet.repository.TrackingSheetRepository;
import com.cosw.councilOfSocialWork.exception.PictureCannotBeDeletedException;
import com.cosw.councilOfSocialWork.exception.ResourceNotFoundException;
import com.cosw.councilOfSocialWork.mapper.images.ImageMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.stream.Collectors;

@Service
@Slf4j
public class ForwardedEmailProcessingService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ImagesRepository imagesRepository;
    private final CardProClientRepository cardProClientRepository;
    private TrackingSheetRepository trackingSheetRepository;
    private ProcessedCardProClientsStatsRepository processedCardProClientsStatsRepository;

    private ImageMapper mapper;

    private List<CardProClient> cardProClientList = new ArrayList<>();
    private ConcurrentHashMap<String, CardProClient> cardProClientConcurrentHashMap = new ConcurrentHashMap<>();

    private static final String APPLICATION_NAME = "csw";
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_MODIFY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";  // Downloaded from Google Cloud Console

    private static String redirectUri = "https://cswtest.site/api/oauth2/callback";

    public ForwardedEmailProcessingService(
            ImagesRepository imagesRepository,
            CardProClientRepository cardProClientRepository,
            TrackingSheetRepository trackingSheetRepository,
            ProcessedCardProClientsStatsRepository processedCardProClientsStatsRepository,
            ImageMapper mapper) {
        this.imagesRepository = imagesRepository;
        this.cardProClientRepository = cardProClientRepository;
        this.trackingSheetRepository = trackingSheetRepository;
        this.processedCardProClientsStatsRepository = processedCardProClientsStatsRepository;
        this.mapper = mapper;
    }

    public static Credential getEmailServerCredentials(final HttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream inputStream = CardProServiceImpl.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(inputStream));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
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

    public boolean createClientListAndDownloadImages() throws IOException, GeneralSecurityException {

        //clear current data
        // cardProClientRepository.deleteAll();

        HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Gmail service = null;
        try {
            service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getEmailServerCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (IllegalStateException e) {
            log.info("ERROR: null Google OAuth token");
            return false;
        }

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
        // ExecutorService executor = Executors.newFixedThreadPool(5);

        var goodEmailCounter = 0;
        var notInTrackingSheetCounter = 0;
        var emailsNoAttachmentCounter = 0;
        var hasDifferentEmailCounter = 0;
        var emptyEmail = 0;
        var emptyPayload = 0;
        var fromCSW = 0;

        // email lists
        var notInTrackingSheetEmailList = new ArrayList<String>();
        var hasDifferentEmailList = new ArrayList<String>();
        var emptyPayloadEmailsList = new ArrayList<String>();
        var totalEmailWithMultipleImagesList = new ArrayList<String>();

        for(Message message : messages) {

            var isNotInTrackingSheet = false;
            var hasDifferentEmail = false;
            var noAttachmentFound = false;

            Message email = service.users().messages().get("me", message.getId()).execute();

            if(email.getPayload() == null){
                ++emptyPayload;
                continue;
            }

            var clientEmailAddress = extractClientEmailAddress(email);

            if(clientEmailAddress.isEmpty()){
                ++emptyEmail;
                continue;
            }

            if("csw.identificationcards@gmail.com".equals(clientEmailAddress)){
                ++fromCSW;
                continue;
            }

            // there are multiple records in the tracking sheet with identical email address
            var client = trackingSheetRepository.findFirstByEmailOrderByRegistrationYearDesc(clientEmailAddress);

            // try & find client via name & surname in the tracking sheet
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

                    hasDifferentEmailList.add(clientEmailAddress);

                    ++hasDifferentEmailCounter;
                }
                else{

                    notInTrackingSheetEmailList.add(clientEmailAddress);

                    ++notInTrackingSheetCounter;

                    // if name cannot be extracted continue
                    if(name.isEmpty() || surname.isEmpty())
                        continue;

                    var clientObj = CardProClient.builder()
                            .id(null)
                            .email(clientEmailAddress)
                            .name(name.isEmpty() ? "N/A" : name)
                            .surname(surname.isEmpty() ? "N/A" : surname)
                            .registrationNumber("N/A")
                            .practiceNumber("N/A")
                            .profession("N/A")
                            .dateOfExpiry("N/A")
                            .hasDifferentEmail(false)
                            .notInTrackingSheet(true)
                            .messageId(message.getId())
                            .images(new HashSet<>())
                            .build();

                    cardProClientList.add(clientObj);

                    continue;
                }
            }

            try {
                var attachmentsSet = extractThenDownloadAttachmentAndReturnAttachmentPath(email.getPayload(), message.getId(), service, client.get());

                var clientObj = CardProClient.builder()
                        .id(null)
                        .email(clientEmailAddress)
                        .name(client.get().getName())
                        .surname(client.get().getSurname())
                        .registrationNumber(client.get().getRegistrationNumber())
                        .practiceNumber(client.get().getPracticeNumber())
                        .profession("Registered Social Worker")
                        .dateOfExpiry("31/12/" + LocalDate.now().getYear())
                        .hasDifferentEmail(hasDifferentEmail)
                        .messageId(message.getId())
                        .build();

                attachmentsSet.forEach(it -> it.setCardProClient(clientObj));

                if(!attachmentsSet.isEmpty()){

                    ++goodEmailCounter;

                    clientObj.setImages(attachmentsSet);
                    clientObj.setHasNoAttachment(false);

                }
                else{

                    emptyPayloadEmailsList.add(clientEmailAddress);

                    ++emailsNoAttachmentCounter;

                    clientObj.setImages(new HashSet<>());
                    clientObj.setHasNoAttachment(true);
                }

                cardProClientList.add(clientObj);


            } catch (IOException e) {
                log.info("ERROR Client Email :: {}", clientEmailAddress);
                return false;
            }
        }

        log.info("Done");

/*        log.info("Total Messages: {}", messages.size());
        log.info("Total +ve email processed: {}", emailCounter);
        log.info("Total cardPro records: {}", cardProClientList.size());
        log.info("Total Not in TS: {}", notInTrackingSheetCounter);
        log.info("Total No Attachment: {}", emailsNoAttachmentCounter);
        log.info("Total empty payload: {}", emptyPayload);
        log.info("Total empty email: {}", emptyEmail);*/

        ProcessedCardProClientsStats cardProTransaction = processedCardProClientsStatsRepository.save(
                ProcessedCardProClientsStats.builder()
                        .totalEmails(messages.size())
                        .processedEmails(goodEmailCounter)
                        .notInTrackingSheet(notInTrackingSheetCounter)
                        .notInTrackingSheetEmailList(notInTrackingSheetEmailList)
                        .emailsNoAttachment(emailsNoAttachmentCounter)
                        .hasDifferentEmail(hasDifferentEmailCounter)
                        .hasDifferentEmailList(hasDifferentEmailList)
                        .emptyEmails(emptyEmail)
                        .emptyPayloadEmails(emptyPayload)
                        .emptyPayloadEmailsList(emptyPayloadEmailsList)
                        .totalEmailsWithMultipleImages(cardProClientList.stream().filter(it -> it.getImages().size() > 1).collect(Collectors.toSet()).size())
                        .totalEmailWithMultipleImagesList(cardProClientList.stream().filter(it -> it.getImages().size() > 1).map(it -> it.getEmail()).toList())
                        .build()
        );

        // add transaction ids to all records
        cardProClientList.forEach(it -> it.setTransactionId(cardProTransaction.getTransactionId()));

        cardProClientRepository.deleteAll();

        cardProClientRepository.saveAll(cardProClientList);
        cardProClientList.clear();

        return true;

    }

    public static void markEmailAsRead(Gmail service, String messageId) throws IOException {
        // Create a request to remove the "UNREAD" label
        ModifyMessageRequest modifyRequest = new ModifyMessageRequest()
                .setRemoveLabelIds(List.of("UNREAD"));

        // Apply the modification
        service.users().messages().modify("me", messageId, modifyRequest).execute();
    }

    public String encodeAttachmentFilePath(String filePath){
        String encodedPath;
        String userHome = System.getProperty("user.home");

        String currentYear = String.valueOf(LocalDate.now().getYear());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        String dateToday = LocalDate.now().format(formatter);

        // String baseUrl = "http://192.168.100.5";
        String baseUrl = "http://68.183.91.109:8080";
        // String baseFilePath = userHome + File.separator + "Downloads" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator +  "Images" + File.separator;
        String baseFilePath = userHome + File.separator + "media" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator +  "Images" + File.separator;

        encodedPath = URLEncoder.encode(filePath.substring(filePath.lastIndexOf(File.separator) + 1), StandardCharsets.UTF_8).replace("+", "%20");
        return baseUrl + baseFilePath + encodedPath;
        // return baseFilePath + filePath;
    }

    public String extractClientEmailAddress(Message message){
        // Extract the ---------- Forwarded message --------- section
        var email = "";

        for(MessagePart emailMessagePart: message.getPayload().getParts()){
            if(emailMessagePart.getParts() != null){
                for (MessagePart subPart : emailMessagePart.getParts()) {
                    if(subPart.getMimeType().equals("text/plain")){
                        var emailBody = new String(Base64.getUrlDecoder().decode(subPart.getBody().getData()), StandardCharsets.UTF_8);

                        if(emailBody.contains("<"))
                            email = emailBody.substring(emailBody.indexOf("<") + 1, emailBody.indexOf(">"));
                    } else if (subPart.getMimeType().equals("multipart/alternative")) {
                        for(MessagePart innerPart: subPart.getParts()){
                            if(innerPart.getMimeType().equals("text/plain")){
                                try{
                                    var emailBody = new String(Base64.getUrlDecoder().decode(innerPart.getBody().getData()), StandardCharsets.UTF_8);

                                    if(emailBody.contains("<"))
                                        email = emailBody.substring(emailBody.indexOf("<") + 1, emailBody.indexOf(">"));
                                } catch (Exception e) {
                                    return email;
                                }
                            }
                        }
                    }
                }
            }
        }

/*        for(MessagePart emailMessagePart: message.getPayload().getParts()){
            if(emailMessagePart.getMimeType().equals("text/plain")){
                var emailBody = new String(Base64.getUrlDecoder().decode(emailMessagePart.getBody().getData()), StandardCharsets.UTF_8);
                email = emailBody.substring(emailBody.indexOf("<") + 1, emailBody.indexOf(">"));
            }
        }*/

        return email;

    }

    public String extractClientNameFromAddress(Message message){

        var clientName = "";

        // Extract the "From" header
        for (MessagePart emailMessagePart: message.getPayload().getParts()) {
            if(emailMessagePart.getMimeType().equals("text/plain")){
                var emailBody = new String(Base64.getUrlDecoder().decode(emailMessagePart.getBody().getData()), StandardCharsets.UTF_8);
                clientName = emailBody.substring(0, emailBody.indexOf("<")).trim();
                break;
            }
        }

        // log.info("Client Name {}", clientName);

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

    public Set<Image> extractThenDownloadAttachmentAndReturnAttachmentPath(MessagePart part, String messageId, Gmail service, TrackingSheetClient client) throws IOException {

        Set<Image> images = new HashSet<>(); // Create a Set

        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                if (subPart.getFilename() != null && !subPart.getFilename().isEmpty() && (subPart.getFilename().contains("jpeg") || subPart.getFilename().contains("png") || subPart.getFilename().contains("jpg"))) {

                    var filePath = downloadAttachmentAndReturnNewFilePath(service, messageId, subPart, client);

                    images.add(
                            Image.builder()
                                    .id(null)
                                    //.attachmentPath(encodeAttachmentFilePath(filePath))
                                    .attachmentPath(filePath)
                                    .attachmentFileName(filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath.lastIndexOf(".")))
                                    .build()
                    );
                }
            }
        }
        else if (part.getBody() != null && part.getFilename() != null && !part.getFilename().isEmpty() && (part.getFilename().contains("jpeg") || part.getFilename().contains("png") || part.getFilename().contains("jpg"))) {

            var filePath = downloadAttachmentAndReturnNewFilePath(service, messageId, part, client);
            images.add(
                    Image.builder()
                            .id(null)
                            //.attachmentPath(encodeAttachmentFilePath(filePath))
                            .attachmentPath(filePath)
                            .attachmentFileName(filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath.lastIndexOf(".")))
                            .build()
            );
        }

        return images;

    }

    private String downloadAttachmentAndReturnNewFilePath(Gmail service, String messageId, MessagePart part, TrackingSheetClient client) throws IOException {

        var partId = part.getPartId();
        String fileExtension = part.getFilename().substring(part.getFilename().lastIndexOf("."));
        String filename = createNewAttachmentFileName(client, partId, fileExtension);
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
            // String filePath = userHome + File.separator + "media" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator +  "Images" + File.separator + filename;
            String filePath =  userHome + File.separator + "Downloads" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator +  "Images" + File.separator + filename;
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

    private String createNewAttachmentFileName(TrackingSheetClient client, String partId, String fileExtension){
        try {

            String[] usernames = client.getName().split(" ");
            StringBuilder fileName = new StringBuilder();

            for(String name: usernames){
                fileName.append(name).append(" ");
            }

            fileName.append(client.getSurname());

            // partId > 1 means there are multiple attachments
            if(Integer.valueOf(partId) > 1){
                fileName.append("_").append(Integer.valueOf(partId));
            }

            fileName.append(fileExtension);

            return fileName.toString();

        } catch (UsernameNotFoundException e) {
            log.error("Client not found");
            log.error("ERROR createNewAttachmentFileName() :: {}", client.getEmail());
            return "";
        }
    }

    public Page<ImageDto> getImages(int pageNumber, int pageSize, String sortBy){
        Pageable page = PageRequest.of(pageNumber, pageSize, Sort.by(sortBy));
        return imagesRepository.findAll(page).map(mapper::imageToImageDto);
    }

    public ImageDto softDeleteImage(ImageDeleteDto imageDeleteDto){

        var clientImagesSize = imagesRepository.countByCardProClient_IdAndDeletedFalse(imageDeleteDto.clientId());

        if(clientImagesSize > 1){

            var image = imagesRepository.findById(imageDeleteDto.id()).orElseThrow(() -> new ResourceNotFoundException("Picture not found"));

            image.setDeleted(true);
            image = imagesRepository.save(image);

            return mapper.imageToImageDto(image);

        }
        else{

            log.error("Error: image size {} {}", clientImagesSize, imageDeleteDto.clientId());

            throw new PictureCannotBeDeletedException("Cannot delete, Client only has one picture");
        }

    }

    public ImageDto undoDeleteImage(ImageDeleteDto imageDeleteDto){

        var image = imagesRepository.findById(imageDeleteDto.id()).orElseThrow(() -> new ResourceNotFoundException("Picture not found"));

        image.setDeleted(false);

        image = imagesRepository.save(image);

        return mapper.imageToImageDto(image);

    }

}
