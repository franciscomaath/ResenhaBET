package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.service.dto.H2HRecord;
import com.franciscomaath.resenhaapi.service.dto.OddsResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public interface OddsCalculatorService {
    OddsResult calculate(BigDecimal eloHome, BigDecimal eloAway, H2HRecord h2hRecord);

    OddsResult calculateNoDraw(BigDecimal eloHome, BigDecimal eloAway, H2HRecord h2hRecord);
}
