package com.franciscomaath.resenhaapi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Table(name = "competition")
public class Competition implements SoftDeletable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private UUID uuid;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 20)
    private String season;

    @Column(name = "api_football_league_id", nullable = false, length = 20)
    private String apiFootballLeagueId;

    @Column(name = "api_football_country_id", nullable = false, length = 20)
    private String apiFootballCountryId;

    @Column(name = "game_forecast_league_id", nullable = false, length = 20)
    private String gameForecastLeagueId;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "competition")
    private List<Tournament> tournaments = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
