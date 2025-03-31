package com.cosw.councilOfSocialWork.domain.cardpro.entity;

import com.cosw.councilOfSocialWork.domain.images.entity.Image;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
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
    private String dateOfExpiry;
    private boolean hasDifferentEmail;
    private boolean noAttachment;
    @OneToMany(mappedBy = "cardProClient", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Image> images;
}
