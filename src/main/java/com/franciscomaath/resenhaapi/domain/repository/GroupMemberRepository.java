package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    List<GroupMember> findByGroupId(Long groupId);

    List<GroupMember> findByUserIdOrderByGroupNameAsc(Long userId);

    Optional<GroupMember> findByGroupIdAndUserIdAndDeletedAtIsNull(Long groupId, Long userId);

    List<GroupMember> findByUserIdAndDeletedAtIsNullOrderByGroupNameAsc(Long userId);
}
