package com.cosw.councilOfSocialWork.domain.transactionHistory.entity;

import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import com.cosw.councilOfSocialWork.util.BaseModel;
import jakarta.persistence.*;
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
public class CardProTransaction extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    Integer totalEmails;

}
