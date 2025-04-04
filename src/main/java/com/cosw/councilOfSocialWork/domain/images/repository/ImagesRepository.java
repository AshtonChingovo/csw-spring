package com.cosw.councilOfSocialWork.domain.images.repository;

import com.cosw.councilOfSocialWork.domain.images.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImagesRepository extends PagingAndSortingRepository<Image, Long>, JpaRepository<Image, Long> {
}
