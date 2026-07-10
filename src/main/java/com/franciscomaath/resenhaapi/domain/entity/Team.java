package com.franciscomaath.resenhaapi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Table(name = "team")
public class Team implements SoftDeletable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false, length = 4)
    private String abbreviation;

    @Column(name = "api_football_team_id", unique = true, length = 20)
    private String apiFootballTeamId;

    @Column(name = "game_forecast_team_id", unique = true, length = 20)
    private String gameForecastTeamId;

    @Column(name = "badge_url")
    private String badgeUrl;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
