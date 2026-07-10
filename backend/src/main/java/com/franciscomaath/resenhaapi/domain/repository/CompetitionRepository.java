package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.Competition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    List<Competition> findByActiveTrue();

    Optional<Competition> findByUuid(UUID uuid);

    List<Competition> findByActiveTrueAndDeletedAtIsNull();
}
