package com.cosw.councilOfSocialWork.domain.trackingSheet.repository;

import com.cosw.councilOfSocialWork.domain.trackingSheet.entity.TrackingSheetClient;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrackingSheetRepository extends PagingAndSortingRepository<TrackingSheetClient, UUID>, ListCrudRepository<TrackingSheetClient, UUID> {

    Optional<TrackingSheetClient> findFirstByEmailOrderByRegistrationYearDesc(String email);

    Optional<TrackingSheetClient> findFirstByNameAndSurnameOrderByRegistrationYearDesc(String name, String surname);

}
