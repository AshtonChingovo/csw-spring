package com.cosw.councilOfSocialWork.domain.cardpro.repository;

import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CardProClientRepository extends PagingAndSortingRepository<CardProClient, UUID>, JpaRepository<CardProClient, UUID> {

    @Query("SELECT c FROM CardProClient c LEFT JOIN c.images i GROUP BY c ORDER BY COUNT(i) DESC, c.name ASC")
    Page<CardProClient> findAllSorted(Pageable pageable);

    @Query("SELECT c FROM CardProClient c JOIN c.images i WHERE i.deleted IS NULL OR i.deleted = false GROUP BY c HAVING COUNT(i) = 1")
    List<CardProClient> findAllValidClients();

    // Queries with filter options
    @Query("SELECT c FROM CardProClient c WHERE c.notInTrackingSheet = true")
    Page<CardProClient> findAllClientsNotInTrackingSheet(Pageable pageable);

    @Query("SELECT c FROM CardProClient c JOIN c.images i WHERE i.deleted IS NULL OR i.deleted = false GROUP BY c HAVING COUNT(i) > 1 ORDER BY c.name ASC")
    Page<CardProClient> findAllClientsWithMultipleImages(Pageable pageable);

    // Queries with Search Only or Search & Filter
    @Query("""
    SELECT c FROM CardProClient c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
       OR LOWER(c.surname) LIKE LOWER(CONCAT('%', :searchTerm, '%'))""")
    Page<CardProClient> searchClients( @Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("""
    SELECT c FROM CardProClient c
    JOIN c.images i
    WHERE (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
         LOWER(c.surname) LIKE LOWER(CONCAT('%', :search, '%')))
      AND (i.deleted IS NULL OR i.deleted = false)
    GROUP BY c
    HAVING COUNT(i) > 1""")
    Page<CardProClient> searchClientsWithMultipleImages(@Param("search") String search, Pageable pageable);

    // email OR LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    @Query("""
    SELECT c FROM CardProClient c
    WHERE (
        LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
        LOWER(c.surname) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
    )
    AND c.notInTrackingSheet = true""")
    Page<CardProClient> searchClientsNotInTrackingSheet(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Modifying
    @Query("DELETE FROM CardProClient")
    void deleteAllEntries();

    @Query("SELECT COUNT(c) FROM CardProClient c WHERE c.hasDifferentEmail = false AND c.hasNoAttachment = false AND SIZE(c.images) = 1")
    int countTotalReady();

    @Query("SELECT COUNT(c) FROM CardProClient c WHERE c.hasDifferentEmail = true")
    int countByHasDifferentEmail();

    @Query("SELECT COUNT(c) FROM CardProClient c WHERE c.hasNoAttachment = true")
    int countByHasNoAttachment();

}
