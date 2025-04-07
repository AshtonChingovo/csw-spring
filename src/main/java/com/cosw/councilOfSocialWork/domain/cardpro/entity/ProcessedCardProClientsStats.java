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
    private UUID transactionId;
    private String messageId;
    private Integer emailCounter;
    private Integer notInTrackingSheetCounter;
    private Integer emailsNoAttachmentCounter;
    private Integer emptyEmail;
    private Integer emptyPayload;
    private Integer fromCSWEmailAddress;
}
