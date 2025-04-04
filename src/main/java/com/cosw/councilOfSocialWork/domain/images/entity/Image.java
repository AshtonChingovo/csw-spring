package com.cosw.councilOfSocialWork.domain.images.entity;

import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import com.cosw.councilOfSocialWork.util.BaseModel;
import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@ToString
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Image extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String attachmentFileName;
    private String attachmentPath;
    private String croppedPath;
    private Boolean cropped = false;
    private Boolean deleted = false;
    @ManyToOne
    @JoinColumn(name = "card_pro_client_id")
    private CardProClient cardProClient;
}
