package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);

    Optional<User> findByNameIgnoreCase(String name);

    boolean existsByName(String name);

    boolean existsByUserType(UserType userType);

    Optional<User> findFirstByUserType(UserType userType);

    List<User> findAllByOrderByNameAsc();
}
