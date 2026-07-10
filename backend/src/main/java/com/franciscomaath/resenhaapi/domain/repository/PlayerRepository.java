package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player,Long> {
    boolean existsByUserId(Long userId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    Optional<Player> findByUserId(Long userId);

    Optional<Player> findByIdAndGroupId(Long id, Long groupId);

    List<Player> findByGroupIdAndUserIsNull(Long groupId);

    java.util.List<Player> findByGroupIdOrderByNameAsc(Long groupId);

    Optional<Player> findByIdAndGroupIdAndDeletedAtIsNull(Long id, Long groupId);

    java.util.List<Player> findByGroupIdAndDeletedAtIsNullOrderByNameAsc(Long groupId);
}
