package com.cosw.councilOfSocialWork.domain.cardpro.repository;

import com.cosw.councilOfSocialWork.domain.cardpro.entity.ProcessedCardProClientsStats;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ProcessedCardProClientsStatsRepository extends JpaRepository<ProcessedCardProClientsStats, UUID> {

    Optional<ProcessedCardProClientsStats> findTopByOrderByTransactionIdDesc();

}
