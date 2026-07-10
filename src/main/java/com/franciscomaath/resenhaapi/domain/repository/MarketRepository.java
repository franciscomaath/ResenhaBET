package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.Market;
import com.franciscomaath.resenhaapi.domain.enums.MarketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MarketRepository extends JpaRepository<Market, Long> {
    List<Market> findByEventIdIn(List<Long> eventIds);

    List<Market> findAllByEventId(Long eventId);

    boolean existsByEventId(Long eventId);

    List<Market> findAllByEventIdAndMarketTypeIn(Long eventId, Collection<MarketType> marketTypes);

    Optional<Market> findByEventIdAndMarketType(Long eventId, com.franciscomaath.resenhaapi.domain.enums.MarketType marketType);
}
