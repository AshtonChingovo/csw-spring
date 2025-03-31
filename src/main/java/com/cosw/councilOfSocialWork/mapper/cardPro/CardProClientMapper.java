package com.cosw.councilOfSocialWork.mapper.cardPro;

import com.cosw.councilOfSocialWork.domain.cardpro.dto.CardProSheetClientDto;
import com.cosw.councilOfSocialWork.domain.cardpro.entity.CardProClient;
import com.cosw.councilOfSocialWork.mapper.config.IgnoreUnmappedPropertiesConfig;
import org.mapstruct.Mapper;
import org.springframework.context.annotation.Configuration;

@Configuration
@Mapper(componentModel = "spring", config = IgnoreUnmappedPropertiesConfig.class)
public interface CardProClientMapper {

    CardProSheetClientDto cardProClientToCardProSheetClientDto(CardProClient cardProClient);


}

