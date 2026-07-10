package com.franciscomaath.resenhaapi.domain.entity;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Builder
@Table(name = "event")
@AllArgsConstructor
public class Event implements SoftDeletable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @ManyToOne
    @JoinColumn(name = "round_id")
    private TournamentRound round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_home_id")
    private Player playerHome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_away_id")
    private Player playerAway;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_home_id")
    private Team teamHome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_away_id")
    private Team teamAway;

    @Column(name = "external_match_id", length = 20)
    private String externalMatchId;

    @Column(name = "game_datetime")
    private LocalDateTime gameDatetime;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "home_elo_before")
    private BigDecimal homeEloBefore;

    @Column(name = "away_elo_before")
    private BigDecimal awayEloBefore;

    @Column(name = "is_knockout")
    private Boolean isKnockout;

    @Builder.Default
    @Column(name = "is_bye", nullable = false)
    private Boolean isBye = false; // para eventos de bye, onde um jogador avança automaticamente

    @Column(name = "penalties_home")
    private Integer penaltiesHome;

    @Column(name = "penalties_away")
    private Integer penaltiesAway;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_round_event_id")
    private Event nextRoundEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_source_event_id")
    private Event homeSourceEvent; // de qual evento vem o jogador do lado casa

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_source_event_id")
    private Event awaySourceEvent; // de qual evento vem o jogador do lado fora

    @Builder.Default
    @Column(name = "is_third_place_match", nullable = false)
    private boolean thirdPlaceMatch = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public String resolveAwayLabel() {
        if (awaySourceEvent != null) {
            return "Winner of " + awaySourceEvent.getRound().getName() + " Match " + awaySourceEvent.getId();
        }
        if (playerAway != null) return playerAway.getName();
        if (teamAway != null) return teamAway.getName();
        return null;
    }

    public String getHomeName() {
        if (playerHome != null) return playerHome.getName();
        if (teamHome != null) return teamHome.getName();
        return null;
    }

    public String getAwayName() {
        if (playerAway != null) return playerAway.getName();
        if (teamAway != null) return teamAway.getName();
        return null;
    }
}
