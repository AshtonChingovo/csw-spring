package com.cosw.councilOfSocialWork.domain.cardpro.entity;

import com.cosw.councilOfSocialWork.util.BaseModel;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedCardProClientsStats extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private UUID transactionId;
    private Integer totalEmails;
    private Integer processedEmails;
    private Integer notInTrackingSheet;
    private List<String> notInTrackingSheetEmailList;
    private Integer emailsNoAttachment;
    private Integer hasDifferentEmail;
    private List<String> hasDifferentEmailList;
    private Integer emptyEmails;
    private Integer emptyPayloadEmails;
    private List<String> emptyPayloadEmailsList;
    private Integer fromCSWEmailAddress;
    // pictures details
    private Integer totalEmailsWithMultipleImages;
    private List<String> totalEmailWithMultipleImagesList;
}
