package com.franciscomaath.resenhaapi.domain.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Table(name = "outcome")
public class Outcome {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "market_id")
    private Market market;

    private String name;

    private BigDecimal odd;
}
