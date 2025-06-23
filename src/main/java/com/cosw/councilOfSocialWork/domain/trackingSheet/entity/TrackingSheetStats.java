package com.cosw.councilOfSocialWork.domain.trackingSheet.entity;

import com.cosw.councilOfSocialWork.util.BaseModel;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@Entity
@AllArgsConstructor
@RequiredArgsConstructor
public class TrackingSheetStats extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private Integer totalClients;
    private Integer totalRenewed;
    private Integer dueRenewal;
}
