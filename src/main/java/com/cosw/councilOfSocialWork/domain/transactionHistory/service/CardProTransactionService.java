package com.cosw.councilOfSocialWork.domain.transactionHistory.service;

import com.cosw.councilOfSocialWork.domain.transactionHistory.dto.CardProTransactionDto;
import com.cosw.councilOfSocialWork.domain.transactionHistory.repository.CardProTransactionRepository;
import com.cosw.councilOfSocialWork.mapper.cardProTransactionHistory.CardProTransactionHistoryMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CardProTransactionService {

    CardProTransactionRepository cardProTransactionRepository;
    CardProTransactionHistoryMapper mapper;

    public CardProTransactionService(CardProTransactionRepository cardProTransactionRepository, CardProTransactionHistoryMapper mapper) {
        this.cardProTransactionRepository = cardProTransactionRepository;
        this.mapper = mapper;
    }

    public Page<CardProTransactionDto> getCardProClients(int pageNumber, int pageSize, String sortBy, String search, String filter) {

        Pageable page = PageRequest.of(pageNumber, pageSize, Sort.by(sortBy));

        return cardProTransactionRepository.findAll(page).map(mapper::cardProTransactionToCardProTransactionDto);
    }


}
