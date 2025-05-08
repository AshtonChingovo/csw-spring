package com.cosw.councilOfSocialWork.domain.emails.service;

import com.cosw.councilOfSocialWork.domain.googleAuth.service.GoogleOAuthService;
import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import com.cosw.councilOfSocialWork.domain.trackingSheet.repository.TrackingSheetRepository;
import com.cosw.councilOfSocialWork.exception.GoogleOAuthException;
import com.cosw.councilOfSocialWork.exception.ResourceNotFoundException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.UUID;

@Service
@Slf4j
public class EmailService {

    private static String TEST_ENV = "test";
    private static String DEV_ENV = "dev";
    private static final String APPLICATION_NAME = "csw";
    private static final String APPLICATION_NAME_DEV = "csw_dev";

    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    String receipientAddress = "coswzw@gmail.com";
    String senderAddress = "coswzw@gmail.com";
    String subject = "Headshot photo request for Practice License";
    String emailBody = "Dear Client,\n\nI hope this message finds you well. Could you kindly provide a headshot photo at your earliest convenience? We need it to complete the processing of your practice license.\n\nPlease let me know if you have any questions or need assistance with the submission.\n\nBest regards,\n Council of Social Workers Zimbabwe";

    private GoogleOAuthService googleOAuthService;
    private TrackingSheetRepository trackingSheetRepository;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    public EmailService(GoogleOAuthService googleOAuthService, TrackingSheetRepository trackingSheetRepository) {
        this.googleOAuthService = googleOAuthService;
        this.trackingSheetRepository = trackingSheetRepository;
    }

    public MimeMessage createEmail() throws MessagingException {

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(senderAddress));
        email.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(receipientAddress));
        email.setSubject(subject);

        // text part
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(emailBody, "text/plain");
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        // attachment file part
        mimeBodyPart = new MimeBodyPart();
        DataSource source = loadImage();
        mimeBodyPart.setDataHandler(new DataHandler(source));
        mimeBodyPart.setFileName("headshot example");
        multipart.addBodyPart(mimeBodyPart);
        email.setContent(multipart);

        return email;
    }

    public DataSource loadImage() {
        ClassPathResource imgFile = new ClassPathResource("static/images/headshot.jpg");
        try (InputStream inputStream = imgFile.getInputStream()) {
            return new ByteArrayDataSource(inputStream, "image/jpeg");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Message createEncodedMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    public Gmail createGmailClient(){

        try {

            HttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Credential credential = TEST_ENV.equals(activeProfile) ? googleOAuthService.getEmailServerCredentials_Test() : googleOAuthService.getEmailServerCredentials_Dev();
            String applicationName = TEST_ENV.equals(activeProfile) ? APPLICATION_NAME : APPLICATION_NAME_DEV;

            return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(applicationName)
                    .build();

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

    }

    public TrackingSheetClient sendEmail(UUID clientId){

        try {

            TrackingSheetClient client = trackingSheetRepository.findById(clientId).orElseThrow(() -> new ResourceNotFoundException("Failed to find Client"));

            // set receipient email
            // receipientAddress = client.getEmail();

            MimeMessage mimeMessage = createEmail();
            Message message = createEncodedMessageWithEmail(mimeMessage);
            Gmail service = createGmailClient();

            // Create send message
            message = service.users().messages().send("me", message).execute();
            log.info("Message id: {}",message.getId());
            log.info("Message id: {}", message.toPrettyString());
            return createNewTrackingSheetClient(client);
        } catch (GoogleJsonResponseException e) {
            GoogleJsonError error = e.getDetails();
            if (error.getCode() == 403) {
                log.error("ERROR unable to send message: {}",e.getDetails());
                return null;
            } else {
                throw new RuntimeException();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    TrackingSheetClient createNewTrackingSheetClient(TrackingSheetClient client){

        return TrackingSheetClient.builder()
                .name(client.getName())
                .surname(client.getSurname())
                .registrationNumber(client.getRegistrationNumber())
                .practiceNumber(client.getPracticeNumber())
                .email(client.getEmail())
                .phoneNumber(client.getPhoneNumber())
                .registrationDate(client.getRegistrationDate())
                .registrationYear(client.getRegistrationYear())
                .sheetYear(client.getSheetYear())
                .membershipStatus(client.getMembershipStatus())
                .headshotRequestEmailSent(true)
                .build();
    }

}
