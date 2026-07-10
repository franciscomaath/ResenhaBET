package com.franciscomaath.resenhaapi.domain.entity;
import com.franciscomaath.resenhaapi.domain.enums.PhaseType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Builder
@AllArgsConstructor
@Table(name = "tournament_round")
public class TournamentRound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    private String name;

    private BigDecimal multiplier;

    @Column(name = "round_order")
    private Integer roundOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase_type", nullable = false)
    @Builder.Default
    private PhaseType phaseType = PhaseType.GROUP_STAGE;

    @Column(name = "group_number")
    private Integer groupNumber; // tournamentGroup
}
