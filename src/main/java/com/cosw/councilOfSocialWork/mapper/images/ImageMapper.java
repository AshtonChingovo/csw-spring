package com.cosw.councilOfSocialWork.mapper.images;

import com.cosw.councilOfSocialWork.domain.images.dto.ImageDto;
import com.cosw.councilOfSocialWork.domain.images.entity.Image;
import com.cosw.councilOfSocialWork.mapper.config.IgnoreUnmappedPropertiesConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.context.annotation.Configuration;

@Configuration
@Mapper(componentModel = "spring", config = IgnoreUnmappedPropertiesConfig.class)
public interface ImageMapper {

    @Mapping(target = "cardProClientId", source = "cardProClient.id")
    @Mapping(target = "email", source = "cardProClient.email")
    ImageDto imageToImageDto(Image image);

}

