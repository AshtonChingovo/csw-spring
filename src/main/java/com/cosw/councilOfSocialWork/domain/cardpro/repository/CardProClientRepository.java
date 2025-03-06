package com.cosw.councilOfSocialWork.domain.cardpro.repository;

import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface CardProClientRepository extends PagingAndSortingRepository<CardProClient, UUID>, JpaRepository<CardProClient, UUID> {
}
