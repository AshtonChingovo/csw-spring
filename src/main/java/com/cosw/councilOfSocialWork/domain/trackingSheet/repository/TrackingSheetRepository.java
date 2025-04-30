package com.cosw.councilOfSocialWork.domain.trackingSheet.repository;

import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrackingSheetRepository extends PagingAndSortingRepository<TrackingSheetClient, UUID>, ListCrudRepository<TrackingSheetClient, UUID> {

    Optional<TrackingSheetClient> findFirstByEmailOrderByRegistrationYearDesc(String email);

    Optional<TrackingSheetClient> findFirstByNameAndSurnameOrderByRegistrationYearDesc(String name, String surname);

    @Query("""
            SELECT t FROM TrackingSheetClient t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(t.surname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))""")
    Page<TrackingSheetClient> searchClients(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("""
            SELECT DISTINCT t FROM TrackingSheetClient t WHERE registrationYear = :activeYear AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(t.surname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))""")
    Page<TrackingSheetClient> searchClientsByActiveMembership(@Param("searchTerm") String searchTerm, @Param("activeYear") String activeYear, Pageable pageable);

    @Query("""
            SELECT DISTINCT t FROM TrackingSheetClient t WHERE registrationYear <> :activeYear AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
               OR LOWER(t.surname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(t.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))""")
    Page<TrackingSheetClient> searchClientsByDueRenewal(@Param("searchTerm") String searchTerm, @Param("activeYear") String activeYear, Pageable pageable);

    @Query("SELECT DISTINCT t FROM TrackingSheetClient t WHERE registrationYear = :activeYear")
    Page<TrackingSheetClient> findClientsByActiveMembership(@Param("searchTerm") String searchTerm, @Param("activeYear") String activeYear, Pageable pageable);

    @Query("SELECT DISTINCT t FROM TrackingSheetClient t WHERE registrationYear <> :activeYear ")
    Page<TrackingSheetClient> findClientsByDueRenewal(@Param("searchTerm") String searchTerm, @Param("activeYear") String activeYear, Pageable pageable);

}
