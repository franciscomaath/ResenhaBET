package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.controller.dto.request.CompetitionRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.CompetitionResponseDTO;

import java.util.List;

public interface CompetitionService {

    CompetitionResponseDTO create(CompetitionRequestDTO dto);

    List<CompetitionResponseDTO> findAll(Boolean active);

    CompetitionResponseDTO findById(Long id);

    CompetitionResponseDTO toggleActive(Long id);
}
