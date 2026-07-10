package com.franciscomaath.resenhaapi.domain.entity;

import com.franciscomaath.resenhaapi.domain.enums.BetSlipItemStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Table(name = "bet_slip_item")
public class BetSlipItem implements SoftDeletable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bet_slip_id")
    private BetSlip betSlip;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne
    @JoinColumn(name = "outcome_id")
    private Outcome outcome;

    @Column(name = "odd_snapshot")
    private BigDecimal oddSnapshot;

    @Enumerated(EnumType.STRING)
    private BetSlipItemStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
