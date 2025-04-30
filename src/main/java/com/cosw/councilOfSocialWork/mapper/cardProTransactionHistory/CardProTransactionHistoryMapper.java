package com.cosw.councilOfSocialWork.mapper.cardProTransactionHistory;

import com.cosw.councilOfSocialWork.domain.transactionHistory.dto.CardProTransactionDto;
import com.cosw.councilOfSocialWork.domain.transactionHistory.entity.CardProTransaction;
import com.cosw.councilOfSocialWork.mapper.config.IgnoreUnmappedPropertiesConfig;
import com.cosw.councilOfSocialWork.mapper.config.InstantDateMapperFormatter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.context.annotation.Configuration;

@Configuration
@Mapper(componentModel = "spring", config = IgnoreUnmappedPropertiesConfig.class, uses = InstantDateMapperFormatter.class)
public interface CardProTransactionHistoryMapper {

    @Mapping(target = "user", source = "createdBy")
    @Mapping(target = "date", source = "createdDate")
    CardProTransactionDto cardProTransactionToCardProTransactionDto(CardProTransaction cardProTransaction);

}


