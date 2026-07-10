package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, Long> {
     Optional<Session> findByToken(UUID token);

     @Query("select s from Session s join fetch s.user left join fetch s.currentGroup where s.token = :token")
     Optional<Session> findByTokenWithUser(@Param("token") UUID token);

     void deleteByToken(UUID token);
}
