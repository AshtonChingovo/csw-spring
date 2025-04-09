package com.cosw.councilOfSocialWork.domain.cardpro.entity;

import com.cosw.councilOfSocialWork.util.BaseModel;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedCardProClientsStats extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID transactionId;
    private Integer totalEmails;
    private Integer processedEmails;
    private Integer notInTrackingSheet;
    private Integer emailsNoAttachment;
    private Integer hasDifferentEmail;
    private Integer emptyEmails;
    private Integer emptyPayloadEmails;
    private Integer fromCSWEmailAddress;
    // pictures details
    private Integer totalEmailsWithMultipleImages;
}
