package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    boolean existsByNameIgnoreCase(String name);

    Optional<Group> findByName(String name);

    Optional<Group> findByIdAndDeletedAtIsNull(Long id);

    Optional<Group> findByGroupCode(String groupCode);
}
