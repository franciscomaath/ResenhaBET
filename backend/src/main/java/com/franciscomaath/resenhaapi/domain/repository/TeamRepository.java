package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    boolean existsByName(String name);

    boolean existsByAbbreviation(String abbreviation);

    java.util.Optional<Team> findByName(String name);

    java.util.Optional<Team> findByApiFootballTeamId(String externalApiId);

    java.util.Optional<Team> findByIdAndDeletedAtIsNull(Long id);
}
