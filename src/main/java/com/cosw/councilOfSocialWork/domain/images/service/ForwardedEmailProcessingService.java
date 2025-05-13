package com.cosw.councilOfSocialWork.domain.images.service;

import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import com.cosw.councilOfSocialWork.domain.cardpro.entity.ProcessedCardProClientsStats;
import com.cosw.councilOfSocialWork.domain.cardpro.repository.CardProClientRepository;
import com.cosw.councilOfSocialWork.domain.cardpro.repository.ProcessedCardProClientsStatsRepository;
import com.cosw.councilOfSocialWork.domain.googleAuth.service.GoogleOAuthService;
import com.cosw.councilOfSocialWork.domain.images.dto.ImageDeleteDto;
import com.cosw.councilOfSocialWork.domain.images.dto.ImageDto;
import com.cosw.councilOfSocialWork.domain.images.entity.Image;
import com.cosw.councilOfSocialWork.domain.images.repository.ImagesRepository;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import com.cosw.councilOfSocialWork.domain.trackingSheet.repository.TrackingSheetRepository;
import com.cosw.councilOfSocialWork.domain.transactionHistory.entity.CardProTransaction;
import com.cosw.councilOfSocialWork.domain.transactionHistory.repository.CardProTransactionRepository;
import com.cosw.councilOfSocialWork.exception.EmailsProcessingException;
import com.cosw.councilOfSocialWork.exception.GoogleOAuthException;
import com.cosw.councilOfSocialWork.exception.PictureCannotBeDeletedException;
import com.cosw.councilOfSocialWork.exception.ResourceNotFoundException;
import com.cosw.councilOfSocialWork.mapper.images.ImageMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.DataStoreCredentialRefreshListener;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.services.sheets.v4.SheetsScopes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

@Service
@Slf4j
public class ForwardedEmailProcessingService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ImagesRepository imagesRepository;
    private final CardProClientRepository cardProClientRepository;
    private TrackingSheetRepository trackingSheetRepository;
    private ProcessedCardProClientsStatsRepository processedCardProClientsStatsRepository;
    private CardProTransactionRepository cardProTransactionRepository;
    private GoogleOAuthService googleOAuthService;

    private ImageMapper mapper;

    private List<CardProClient> cardProClientList = new ArrayList<>();
    private ConcurrentHashMap<String, CardProClient> cardProClientConcurrentHashMap = new ConcurrentHashMap<>();

    private static final String APPLICATION_NAME = "csw";
    private static final String APPLICATION_NAME_DEV = "csw_dev";
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Arrays.asList(GmailScopes.GMAIL_MODIFY, SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH_TEST = "/credentials.json";  // Downloaded from Google Cloud Console
    private static final String CREDENTIALS_FILE_PATH_DEV = "/credentials_dev.json";  // Downloaded from Google Cloud Console

    private static String redirectUri = "https://cswtest.site/api/v1/oauth2/callback";

    private static String TEST_ENV = "test";
    private static String DEV_ENV = "dev";

    @Value("${spring.profiles.active}")
    private String activeProfile;

    public ForwardedEmailProcessingService(
            ImagesRepository imagesRepository,
            CardProClientRepository cardProClientRepository,
            TrackingSheetRepository trackingSheetRepository,
            ProcessedCardProClientsStatsRepository processedCardProClientsStatsRepository,
            CardProTransactionRepository cardProTransactionRepository,
            GoogleOAuthService googleOAuthService,
            ImageMapper mapper) {
        this.imagesRepository = imagesRepository;
        this.cardProClientRepository = cardProClientRepository;
        this.trackingSheetRepository = trackingSheetRepository;
        this.processedCardProClientsStatsRepository = processedCardProClientsStatsRepository;
        this.cardProTransactionRepository = cardProTransactionRepository;
        this.googleOAuthService = googleOAuthService;
        this.mapper = mapper;
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
    }

    public void testGoogleOAuthException(){
        throw new GoogleOAuthException("GoogleOAuth Authentication Failed");
    }

    public boolean createClientListAndDownloadImages(){

        Gmail service;
        ListMessagesResponse response = null;

        try {

            HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Credential credential = TEST_ENV.equals(activeProfile) ? googleOAuthService.getEmailServerCredentials_Test() : googleOAuthService.getEmailServerCredentials_Dev();
            String applicationName = TEST_ENV.equals(activeProfile) ? APPLICATION_NAME : APPLICATION_NAME_DEV;

            service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(applicationName)
                    .build();

            // log.info("GMAIL RESPONSE: {}", service.);

        } catch (IllegalStateException e) {
            log.error("ERROR: NULL Google OAuth token {}", e.toString());
            throw new GoogleOAuthException("Failed to Authenticate with gmail mailbox");
        } catch (GeneralSecurityException e) {
            log.error("ERROR: GeneralSecurityException createClientListAndDownloadImages() {}", e.toString());
            throw new GoogleOAuthException("Failed to Authenticate with gmail mailbox");
        } catch (IOException e) {
            log.error("ERROR: IOException createClientListAndDownloadImages() {}", e.toString());
            throw new GoogleOAuthException("Failed to Authenticate with gmail mailbox");
        }

        // ListMessagesResponse messagesResponse = service.users().messages().list("me").execute();

        List<Message> messages = new ArrayList<>();

        String nextPageToken = null;

        // pagination code for gmail
        do {
            // Fetch emails with pagination support
            try {
                response = service.users().messages()
                        .list("me")
                        .setQ("is:unread") // Filter only unread emails
                        .setMaxResults(100L) // Maximum: 500 per request
                        .setPageToken(nextPageToken)
                        .execute();

            }
            catch (GoogleJsonResponseException e){
                log.error("ERROR: GoogleJsonResponseException {}", e.getStatusCode());
                throw new GoogleOAuthException("Failed to Authenticate with gmail mailbox");
            }
            catch (Exception e) {
                log.error("ERROR: Exception paginating through emails {}", e.toString());
                throw new EmailsProcessingException("Failed to Authenticate with gmail mailbox");
            }

            if (response.getMessages() != null) {
                messages.addAll(response.getMessages());
            }

            // Move to the next page
            nextPageToken = response.getNextPageToken();

            // sleep to allow pagination to progress smoothly
            // without it some emails are skipped
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


        for(Message message : messages) {

            var isNotInTrackingSheet = false;
            var hasDifferentEmail = false;
            var noAttachmentFound = false;

            Message email = null;
            try {
                email = service.users().messages().get("me", message.getId()).execute();
            } catch (IOException e) {
                log.info("ERROR getting messages {}", e.getMessage());
                throw new RuntimeException(e);
            }


            if(email.getPayload() == null || email.getPayload().getParts() == null){
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
            var client = trackingSheetRepository.findFirstByEmailOrderBySheetYearDesc(clientEmailAddress);

            // try & find client via name & surname in the tracking sheet
            if(client.isEmpty()){

                var clientName = extractClientNameFromAddress(email).trim();

                var name = "";
                var surname = "";

                var lastIndexOfSpaceChar = clientName.lastIndexOf(" ") > -1 ? clientName.lastIndexOf(" ") : clientName.length();

                // client has name & surname
                if(clientName.lastIndexOf(" ") > -1){
                    name = extractClientNameFromAddress(email).substring(0, lastIndexOfSpaceChar).trim();
                    surname = extractClientNameFromAddress(email).substring(clientName.lastIndexOf(" ")).trim();
                }
                // client does not have a surname
                else{
                    name = extractClientNameFromAddress(email).substring(0, lastIndexOfSpaceChar).trim();
                    surname = "";
                }

                // find if a client exists with extracted name & surname since email is not in Tracking Sheet
                if(trackingSheetRepository.findFirstByNameAndSurnameOrderBySheetYearDesc(name, surname).isPresent()){

                    client = trackingSheetRepository.findFirstByNameAndSurnameOrderBySheetYearDesc(name, surname);
                    hasDifferentEmail = true;

                    hasDifferentEmailList.add(clientEmailAddress);

                    ++hasDifferentEmailCounter;

                }
                else{

                    ++notInTrackingSheetCounter;

                    notInTrackingSheetEmailList.add(clientEmailAddress);

                    var clientObj = CardProClient.builder()
                            .id(null)
                            .email(clientEmailAddress)
                            .name(name.isEmpty() ? "N/A" : name)
                            .surname(surname.isEmpty() ? "N/A" : surname)
                            .registrationNumber("N/A")
                            .practiceNumber("N/A")
                            .profession("N/A")
                            .registrationYear("N/A")
                            .sheetYear("N/A")
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
                        .registrationYear(client.get().getRegistrationYear())
                        .sheetYear(client.get().getSheetYear())
                        .profession("Registered Social Worker")
                        .dateOfExpiry("31/12/" + LocalDate.now().getYear())
                        .hasDifferentEmail(hasDifferentEmail)
                        .messageId(message.getId())
                        .build();

                if(!attachmentsSet.isEmpty()){

                    attachmentsSet.forEach(it -> it.setCardProClient(clientObj));

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
                log.info("ERROR Client Email :: {} <-> {}", clientEmailAddress, e.toString());
                return false;
            }
        }

        log.info("Image Processing Done");

        // create audit cardpro transaction object
        CardProTransaction cardProTransaction = CardProTransaction.builder()
                .totalEmails(messages.size())
                .build();

        log.info("P11");


        // record stats for this transaction
        var multipleImagesList = cardProClientList.stream().filter(it -> it.getImages().size() > 1).map(CardProClient::getEmail).toList();
        // remove current stats
        processedCardProClientsStatsRepository.deleteAll();
        processedCardProClientsStatsRepository.save(
                ProcessedCardProClientsStats.builder()
                        .totalEmails(response.getMessages().size())
                        .processedEmails(goodEmailCounter)
                        .notInTrackingSheet(notInTrackingSheetCounter)
                        .notInTrackingSheetEmailList(notInTrackingSheetEmailList)
                        .emailsNoAttachment(emailsNoAttachmentCounter)
                        .hasDifferentEmail(hasDifferentEmailCounter)
                        .hasDifferentEmailList(hasDifferentEmailList)
                        .emptyEmails(emptyEmail)
                        .emptyPayloadEmails(emptyPayload)
                        .emptyPayloadEmailsList(emptyPayloadEmailsList)
                        .totalEmailsWithMultipleImages(multipleImagesList.size())
                        .totalEmailWithMultipleImagesList(multipleImagesList)
                        .build()
        );

        // add transaction ids to all records
        cardProClientList.forEach(it -> it.setTransactionId(cardProTransaction));

        // clear current data
        cardProClientRepository.deleteAll();

        // save transaction data
        cardProTransactionRepository.save(cardProTransaction);

        cardProClientRepository.saveAll(cardProClientList);
        cardProClientList.clear();

        // clear out fields
        messages.clear();

        log.info("Data persisted");

        return true;

    }

    public static void markEmailAsRead(Gmail service, String messageId) throws IOException {
        // Create a request to remove the "UNREAD" label
        ModifyMessageRequest modifyRequest = new ModifyMessageRequest()
                .setRemoveLabelIds(List.of("UNREAD"));

        // Apply the modification
        service.users().messages().modify("me", messageId, modifyRequest).execute();
    }

    public String extractClientEmailAddress(Message message){
        // Extract the ---------- Forwarded message --------- section
        var email = "";

        log.info("Part size {}", message.getPayload().getParts().size());
        log.info("Part data {}", message.getPayload().getParts());

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
                                    log.error("ERROR extracting client email address {}", e.toString());
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

        // log.info("GmailParts {} {}", client.getEmail(), part);

        // log.info("L1");
        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {

                if (subPart.getFilename() != null && !subPart.getFilename().isEmpty() && (subPart.getFilename().contains("jpeg") || subPart.getFilename().contains("png") || subPart.getFilename().contains("jpg") || subPart.getFilename().contains("heic") || subPart.getFilename().contains("heif"))) {

                    var filePath = downloadAttachmentAndReturnNewFilePath(service, messageId, subPart, client);
                    var fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath.lastIndexOf("."));

                    if(!filePath.isEmpty() && !fileName.isEmpty()){
                        images.add(
                                Image.builder()
                                        .id(null)
                                        .attachmentPath(filePath)
                                        .url(encodeAttachmentFilePath(filePath))
                                        .attachmentFileName(fileName)
                                        .build()
                        );
                    }
                }
            }
        }
        else if (part.getBody() != null && part.getFilename() != null && !part.getFilename().isEmpty() && (part.getFilename().contains("jpeg") || part.getFilename().contains("png") || part.getFilename().contains("jpg") || part.getFilename().contains("heic") || part.getFilename().contains("heif"))) {

            var filePath = downloadAttachmentAndReturnNewFilePath(service, messageId, part, client);

            if(!filePath.isEmpty()){
                images.add(
                        Image.builder()
                                .id(null)
                                .attachmentPath(filePath)
                                .url(encodeAttachmentFilePath(filePath))
                                .attachmentFileName(filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath.lastIndexOf(".")))
                                .build()
                );
            }
        }

        // log.info("L2 ----");
        return images;
    }

    public String encodeAttachmentFilePath(String filePath){
        String encodedFileName;

        String baseFilePath = "/api" + File.separator;
        encodedFileName = URLEncoder.encode(filePath.substring(filePath.lastIndexOf(File.separator) + 1), StandardCharsets.UTF_8).replace("+", "%20");
        return baseFilePath + encodedFileName;
    }

    private String downloadAttachmentAndReturnNewFilePath(Gmail service, String messageId, MessagePart part, TrackingSheetClient client) throws IOException {

        var partId = part.getPartId();
        String fileExtension = part.getFilename().substring(part.getFilename().lastIndexOf("."));
        String filename = createNewAttachmentFileName(client, partId, fileExtension);
        String attachmentId = part.getBody().getAttachmentId();

        String filePath = "";

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

            String userHome = System.getProperty("user.home");
            // String filePath = userHome + File.separator + "media" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator +  "Images" + File.separator + filename;

            if(TEST_ENV.equals(activeProfile)){
               filePath =  "csw_files" + File.separator + "images" + File.separator + filename;
            }
            else{
                filePath =  userHome + File.separator + "Downloads" + File.separator + "CWS Files" + File.separator + currentYear + File.separator + dateToday + File.separator +  "Images" + File.separator + filename;
            }

            File file = new File(filePath);

            // Ensure directory exists
            file.getParentFile().mkdirs();

            // Decode the data
            byte[] fileData = Base64.getDecoder().decode(base64);

            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(fileData);
            }
            catch (Exception e){
                log.error("ERROR creating image extracted from email {} <-> {}", client.getEmail(),  e.getMessage());
                return "";
            }

            return file.getAbsolutePath();
        }

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
            log.error("ERROR Client not found createNewAttachmentFileName() :: {}", client.getEmail());
            return "";
        }
    }

    public Page<ImageDto> getImages(int pageNumber, int pageSize, String sortBy){
        Pageable page = PageRequest.of(pageNumber, pageSize, Sort.by(sortBy));
        return imagesRepository
                .findAll(page)
                .map(image -> {
                    image.setAttachmentPath(encodeAttachmentFilePath(image.getAttachmentPath()));
                    return mapper.imageToImageDto(image);
                });
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
