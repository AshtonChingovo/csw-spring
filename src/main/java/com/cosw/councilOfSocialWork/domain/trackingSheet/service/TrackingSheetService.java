package com.cosw.councilOfSocialWork.domain.trackingSheet.service;

import org.springframework.web.multipart.MultipartFile;

public interface TrackingSheetService {

    String processTrackingSheet(MultipartFile file);

    void getTrackingSheet(int pageNumber, int pageSize, String sortBy);

}
