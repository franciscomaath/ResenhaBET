package com.franciscomaath.resenhaapi.domain.entity;

import com.franciscomaath.resenhaapi.domain.enums.GenerationMode;
import com.franciscomaath.resenhaapi.domain.enums.TournamentStatus;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import com.franciscomaath.resenhaapi.domain.enums.TournamentFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Table(name = "tournament")
public class Tournament implements SoftDeletable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(unique = true, nullable = false)
    private UUID uuid;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private TournamentType type;

    @Enumerated(EnumType.STRING)
    private TournamentFormat format;

    @Enumerated(EnumType.STRING)
    private TournamentStatus status;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_mode", nullable = false)
    @Builder.Default
    private GenerationMode generationMode = GenerationMode.MANUAL;

    @Column(name = "has_third_place_match", nullable = false)
    @Builder.Default
    private Boolean hasThirdPlaceMatch = false;

    @Column(name = "number_of_groups", nullable = false)
    @Builder.Default
    private Integer numberOfGroups = 1;

    @Column(name = "players_advancing_per_group", nullable = false)
    @Builder.Default
    private Integer playersAdvancingPerGroup = 2;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentPlayer> tournamentPlayers = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    private List<TournamentRound> rounds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id")
    private Competition competition;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
