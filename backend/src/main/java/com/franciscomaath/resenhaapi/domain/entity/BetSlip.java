package com.franciscomaath.resenhaapi.domain.entity;

import com.franciscomaath.resenhaapi.domain.enums.BetSlipStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Table(name = "bet_slip")
public class BetSlip implements SoftDeletable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "group_tournament_id")
    private GroupTournament groupTournament;

    private BigDecimal stake;

    @Column(name = "combined_odd")
    private BigDecimal combinedOdd;

    @Column(name = "potential_return")
    private BigDecimal potentialReturn;

    @Enumerated(EnumType.STRING)
    private BetSlipStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "betSlip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BetSlipItem> items = new ArrayList<>();

    @Override
    public void softDelete() {
        SoftDeletable.super.softDelete();
        if (items != null) {
            items.forEach(SoftDeletable::softDelete);
        }
    }
}
