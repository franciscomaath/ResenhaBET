package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.domain.entity.User;

public interface PinService {

    void savePin(User user, String rawPin);

    boolean verifyPin(User user, String rawPin);
}
