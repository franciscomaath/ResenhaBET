package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.Outcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.List;

@Repository
public interface OutcomeRepository extends JpaRepository<Outcome, Long> {
    List<Outcome> findByMarketId(Long marketId);

    List<Outcome> findByMarketIdIn(List<Long> marketIds);
}
