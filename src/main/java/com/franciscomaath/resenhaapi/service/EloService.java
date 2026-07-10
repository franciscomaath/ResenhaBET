package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.entity.Player;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public interface EloService {

    BigDecimal calculateElo(Player player);

    BigDecimal applyEloForEvent(Event event);

    List<Player> recalculateGroupElos(Long groupId);
}
