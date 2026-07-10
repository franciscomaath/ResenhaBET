package com.franciscomaath.resenhaapi.domain.entity;

import java.time.LocalDateTime;

public interface SoftDeletable {
    LocalDateTime getDeletedAt();
    void setDeletedAt(LocalDateTime deletedAt);

    default boolean isDeleted() {
        return getDeletedAt() != null;
    }

    default void softDelete() {
        this.setDeletedAt(LocalDateTime.now());
    }
}
