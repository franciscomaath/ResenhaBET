package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.UserLoginRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.UserUpdatePinRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.UserLoginResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.UserResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Session;
import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.entity.GroupMember;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.exception.UnauthorizedException;
import com.franciscomaath.resenhaapi.domain.repository.SessionRepository;
import com.franciscomaath.resenhaapi.domain.repository.UserRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupMemberRepository;
import com.franciscomaath.resenhaapi.mapper.UserMapper;
import com.franciscomaath.resenhaapi.service.AuthService;
import com.franciscomaath.resenhaapi.service.PinService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final PinService pinService;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final CurrentUserContext currentUserContext;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserLoginResponseDTO login(UserLoginRequestDTO dto) {
        // 1. Find user by name, case-insensitive
        User user = userRepository.findByNameIgnoreCase(dto.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid credentials"));

        // 2. If user has PIN but no PIN was provided → return pinRequired (no session created)
        if (user.getPinHash() != null && (dto.getPin() == null || dto.getPin().isBlank())) {
            UserLoginResponseDTO response = new UserLoginResponseDTO();
            response.setPinRequired(true);
            return response;
        }

        // 3. If user has PIN and PIN was provided → validate via PinService
        if (!pinService.verifyPin(user, dto.getPin())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // 4. Create session
        Session session = new Session();
        UUID token = UUID.randomUUID();
        session.setToken(token);
        session.setUser(user);
        groupMemberRepository.findByUserIdOrderByGroupNameAsc(user.getId()).stream()
                .findFirst()
                .map(GroupMember::getGroup)
                .ifPresent(session::setCurrentGroup);
        session.setExpiresAt(LocalDateTime.now().plusHours(24));
        sessionRepository.save(session);

        boolean wasFirstLogin = user.isFirstLogin();
        if (wasFirstLogin) {
            user.setFirstLogin(false);
            userRepository.save(user);
        }

        UserLoginResponseDTO response = new UserLoginResponseDTO();
        response.setToken(token.toString());
        response.setId(user.getId());
        response.setName(user.getName());
        response.setUserType(user.getUserType());
        if (session.getCurrentGroup() != null) {
            response.setCurrentGroupId(session.getCurrentGroup().getId());
            response.setCurrentGroupName(session.getCurrentGroup().getName());
        }
        response.setFirstLogin(wasFirstLogin);
        response.setHasPin(user.getPinHash() != null);
        return response;
    }

    @Override
    public UserResponseDTO me() {
        return userMapper.toResponse(currentUserContext.getRequiredUser());
    }

    @Override
    @Transactional
    public void logout(UUID token) {
        sessionRepository.deleteByToken(token);
    }

    @Override
    @Transactional
    public void updatePin(UserUpdatePinRequestDTO dto) {
        User user = currentUserContext.getRequiredUser();

        if(user.isFirstLogin()){
            user.setFirstLogin(false);
        }

        pinService.savePin(user, dto.getPin());
    }
}
