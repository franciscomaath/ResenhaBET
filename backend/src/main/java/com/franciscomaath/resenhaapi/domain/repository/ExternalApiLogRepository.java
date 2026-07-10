package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.ExternalApiLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExternalApiLogRepository extends JpaRepository<ExternalApiLog, Long> {
    Optional<ExternalApiLog> findTopByProviderAndEndpointAndRequestKeyOrderByFetchedAtDesc(
            String provider, String endpoint, String requestKey);
}
