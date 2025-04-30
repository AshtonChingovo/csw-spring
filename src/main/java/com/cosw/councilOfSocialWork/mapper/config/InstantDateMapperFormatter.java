package com.cosw.councilOfSocialWork.mapper.config;

import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Configuration
public class InstantDateMapperFormatter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault());

    public String formatInstant(Instant instant) {
        return instant != null ? FORMATTER.format(instant) : null;
    }

}
