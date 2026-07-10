package com.franciscomaath.resenhaapi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import com.franciscomaath.resenhaapi.domain.enums.UserType;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "pin_hash")
    private String pinHash;

    @Column
    private String salt;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Column(name = "is_first_login",nullable = false)
    private boolean firstLogin;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
}
