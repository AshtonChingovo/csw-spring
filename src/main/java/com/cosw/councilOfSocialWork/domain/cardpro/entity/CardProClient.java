package com.cosw.councilOfSocialWork.domain.cardpro.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class CardProClient {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id = null;
    private String name;
    private String surname;
    private String profession;
    private String registrationNumber;
    private String practiceNumber;
    private String email;
    private String attachmentFileName;
    private String attachmentPath;
    private String dateOfExpiry;
    private boolean hasDifferentEmail;
    private boolean noAttachment;
}
