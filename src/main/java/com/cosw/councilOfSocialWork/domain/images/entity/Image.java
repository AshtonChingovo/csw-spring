package com.cosw.councilOfSocialWork.domain.images.entity;

import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import com.cosw.councilOfSocialWork.util.BaseModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Image extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private String attachmentFileName;
    private String attachmentPath;
    private Boolean cropped = false;
    private Boolean deleted = false;
    @ManyToOne
    @JoinColumn(name = "card_pro_client_id")
    private CardProClient cardProClient;
}
