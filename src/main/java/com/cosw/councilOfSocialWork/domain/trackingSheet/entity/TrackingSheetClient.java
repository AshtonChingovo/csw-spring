package com.cosw.councilOfSocialWork.domain.trackingSheet.entity;

import com.cosw.councilOfSocialWork.util.BaseModel;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class TrackingSheetClient extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @NotNull
    private String name;
    @NotNull
    private String surname;
    private String registrationNumber;
    private String practiceNumber;
    private String email;
    private String phoneNumber;
    private String registrationDate;
    private String registrationYear;
    private String membershipStatus;

}
