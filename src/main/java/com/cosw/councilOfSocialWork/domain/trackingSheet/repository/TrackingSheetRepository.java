package com.cosw.councilOfSocialWork.domain.trackingSheet.repository;

import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrackingSheetRepository extends PagingAndSortingRepository<TrackingSheetClient, UUID>, ListCrudRepository<TrackingSheetClient, UUID> {

    Optional<TrackingSheetClient> findFirstByEmailOrderBySheetYearDesc(String email);

    Optional<TrackingSheetClient> findFirstByNameAndSurnameOrderBySheetYearDesc(String name, String surname);

    List<TrackingSheetClient> findBySheetYear(String sheetYear);

    long countBySheetYear(String sheetYear);

    @Query("SELECT c FROM TrackingSheetClient c WHERE c.registrationNumber NOT LIKE %:suffix AND c.sheetYear = :sheetYear")
    List<TrackingSheetClient> findAllWithSheetYear(@Param("suffix") String suffix, @Param("sheetYear") String sheetYear);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE TrackingSheetClient t SET t.membershipStatus = :membershipStatus WHERE t.email = :email")
    void updateMembershipStatus(@Param("email") String email, @Param("membershipStatus") String membershipStatus);

    @Query("""
            SELECT t FROM TrackingSheetClient t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(t.surname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY t.sheetYear DESC, t.name ASC""")
    Page<TrackingSheetClient> searchClients(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("""
            SELECT DISTINCT t FROM TrackingSheetClient t WHERE sheetYear = :activeYear AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(t.surname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ORDER BY t.sheetYear DESC, t.name ASC""")
    Page<TrackingSheetClient> searchClientsByActiveMembership(@Param("searchTerm") String searchTerm, @Param("activeYear") String activeYear, Pageable pageable);

    @Query("""
            SELECT DISTINCT t FROM TrackingSheetClient t WHERE t.membershipStatus = :membershipStatus AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(t.surname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ORDER BY t.sheetYear DESC, t.name ASC""")
    Page<TrackingSheetClient> searchClientsByDueRenewal(@Param("searchTerm") String searchTerm, @Param("membershipStatus") String membershipStatus, @Param("activeYear") String activeYear, Pageable pageable);

    @Query("SELECT DISTINCT t FROM TrackingSheetClient t WHERE sheetYear = :activeYear ORDER BY t.sheetYear DESC, t.name ASC")
    Page<TrackingSheetClient> findClientsByActiveMembership(@Param("activeYear") String activeYear, Pageable pageable);

    @Query("""
        SELECT t
        FROM TrackingSheetClient t
        WHERE t.membershipStatus = :membershipStatus
        AND t.membershipStatus <> :activeYear
          AND t.id IN (
            SELECT MAX(t2.id)
            FROM TrackingSheetClient t2
            WHERE t2.sheetYear <> :activeYear
            GROUP BY t2.email
          )
        ORDER BY t.sheetYear DESC, t.name ASC
        """)
    Page<TrackingSheetClient> findClientsByDueRenewal(@Param("membershipStatus") String membershipStatus, @Param("activeYear") String activeYear, Pageable pageable);

}
