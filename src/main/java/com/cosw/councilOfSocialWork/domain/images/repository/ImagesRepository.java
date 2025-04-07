package com.cosw.councilOfSocialWork.domain.images.repository;

import com.cosw.councilOfSocialWork.domain.images.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImagesRepository extends PagingAndSortingRepository<Image, Long>, JpaRepository<Image, Long> {

    @Query("SELECT COUNT(i) FROM Image i WHERE i.cardProClient.id = :cardProClientId AND i.deleted = false")
    long countByCardProClient_IdAndDeletedFalse(@Param("cardProClientId") UUID cardProClientId);

}
