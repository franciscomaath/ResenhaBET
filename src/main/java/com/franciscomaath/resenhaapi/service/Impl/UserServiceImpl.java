package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.UserRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.UserPatchRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.UserResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.enums.UserType;
import com.franciscomaath.resenhaapi.domain.exception.DuplicateResourceException;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.exception.UnauthorizedException;
import com.franciscomaath.resenhaapi.domain.exception.ValidationException;
import com.franciscomaath.resenhaapi.domain.repository.UserRepository;
import com.franciscomaath.resenhaapi.mapper.UserMapper;
import com.franciscomaath.resenhaapi.service.UserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CurrentUserContext currentUserContext;

    @Override
    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {
        String name = dto.resolvedName();
        if (name == null || name.isBlank()) {
            throw new ValidationException("O nome do usuario e obrigatorio.");
        }

        name = name.trim();
        if (userRepository.existsByName(name)) {
            throw new DuplicateResourceException("Ja existe um usuario com esse nome.");
        }

        User user = User.builder()
                .name(name)
                .firstLogin(true)
                .pinHash(null)
                .salt(null)
                .userType(UserType.USER)
                .build();

        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    public List<UserResponseDTO> findAll() {
        return userRepository.findAllByOrderByNameAsc().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void resetPin(Long id) {
        currentUserContext.requireAdmin();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com id: " + id));

        user.setPinHash(null);
        user.setSalt(null);
        user.setFirstLogin(true);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        User currentUser = currentUserContext.getRequiredUser();
        if (currentUser.getUserType() != UserType.ADMIN && !currentUser.getId().equals(id)) {
            throw new UnauthorizedException("Você não tem permissão para ver este usuário.");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com id: " + id));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponseDTO editUser(Long id, UserPatchRequestDTO dto) {
        User currentUser = currentUserContext.getRequiredUser();
        if (currentUser.getUserType() != UserType.ADMIN && !currentUser.getId().equals(id)) {
            throw new UnauthorizedException("Você não tem permissão para editar este usuário.");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com id: " + id));
        
        user.setName(dto.getName());
        userRepository.save(user);
        return userMapper.toResponse(user);
    }
}
