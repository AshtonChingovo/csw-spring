package com.cosw.councilOfSocialWork.domain.cardpro.entity;

import com.cosw.councilOfSocialWork.domain.images.entity.Image;
import com.cosw.councilOfSocialWork.domain.transactionHistory.entity.CardProTransaction;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CardProClient {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id = null;
    private String name;
    private String surname;
    private String profession;
    private String registrationNumber;
    private String practiceNumber;
    String registrationYear;
    String sheetYear;
    private String email;
    private String phoneNumber;
    private String dateOfExpiry;
    private boolean notInTrackingSheet;
    private boolean hasDifferentEmail;
    private boolean hasNoAttachment;
    private String messageId;
    @ManyToOne()
    private CardProTransaction transactionId;
    @OneToMany(mappedBy = "cardProClient", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Image> images;
}
