package com.cosw.councilOfSocialWork.domain.images.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ImageDto{
    Long id;
    UUID cardProClientId;
    String email;
    String attachmentFileName;
    String attachmentPath;
    String url;
    String croppedPath;
    Boolean cropped;
    Boolean deleted;
}
