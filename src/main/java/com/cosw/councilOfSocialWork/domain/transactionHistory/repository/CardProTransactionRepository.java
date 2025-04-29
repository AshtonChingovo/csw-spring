package com.cosw.councilOfSocialWork.domain.transactionHistory.repository;

import com.cosw.councilOfSocialWork.domain.images.entity.Image;
import com.cosw.councilOfSocialWork.domain.transactionHistory.entity.CardProTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CardProTransactionRepository extends PagingAndSortingRepository<CardProTransaction, UUID>, JpaRepository<CardProTransaction, UUID> {


}
