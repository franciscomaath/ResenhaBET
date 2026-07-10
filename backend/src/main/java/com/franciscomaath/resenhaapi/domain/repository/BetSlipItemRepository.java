package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.BetSlipItem;
import com.franciscomaath.resenhaapi.domain.enums.BetSlipItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BetSlipItemRepository extends JpaRepository<BetSlipItem, Long> {

    List<BetSlipItem> findByEventIdAndStatus(Long eventId, BetSlipItemStatus status);

    List<BetSlipItem> findByEventId(Long eventId);

    List<BetSlipItem> findByBetSlipId(Long betSlipId);
}
