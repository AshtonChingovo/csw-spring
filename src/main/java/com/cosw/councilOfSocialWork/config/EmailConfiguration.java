package com.cosw.councilOfSocialWork.config;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class EmailConfiguration {

    @Value("${email.key}")
    private String key;

    @Value("${email.host}")
    private String host;

    @Value("${email.protocol}")
    private String protocol;

    @Value("${email.address}")
    private String emailAddress;

    @Value("${email.password}")
    private String password;


    public Store createStoreBean() throws MessagingException {

        Properties properties = System.getProperties();
        properties.setProperty(key, protocol);

        Session session = Session.getDefaultInstance(properties, null);

        Store store = session.getStore(protocol);
        store.connect(host, emailAddress, password);

        return store;
    }

}
