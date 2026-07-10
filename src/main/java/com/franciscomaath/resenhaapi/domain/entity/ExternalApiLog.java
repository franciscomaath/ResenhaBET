package com.franciscomaath.resenhaapi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "external_api_log", schema = "resenha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", length = 30, nullable = false)
    private String provider;

    @Column(name = "endpoint", length = 100, nullable = false)
    private String endpoint;

    @Column(name = "request_key", length = 255, nullable = false)
    private String requestKey;

    @JdbcTypeCode(SqlTypes.JSON)   // Hibernate 6+ já suporta nativo, sem precisar de lib extra
    @Column(columnDefinition = "jsonb", nullable = false)
    private String responseBody;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "fetched_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime fetchedAt;
}
