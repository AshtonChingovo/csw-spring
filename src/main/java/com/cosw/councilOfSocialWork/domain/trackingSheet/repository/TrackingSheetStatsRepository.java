package com.cosw.councilOfSocialWork.domain.trackingSheet.repository;

import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrackingSheetStatsRepository extends JpaRepository<TrackingSheetStats, Integer> {
    Optional<TrackingSheetStats> findFirstBy();
}
