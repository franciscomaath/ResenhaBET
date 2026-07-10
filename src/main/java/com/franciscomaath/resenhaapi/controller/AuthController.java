package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.UserLoginRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.UserUpdatePinRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.UserLoginResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.UserResponseDTO;
import com.franciscomaath.resenhaapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUserContext currentUserContext;

    @PostMapping("/login")
    @Operation(summary = "Login de usuario", description = "Autentica um usuario e retorna um token de sessao.")
    public ResponseEntity<UserLoginResponseDTO> login(@RequestBody @Valid UserLoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario autenticado", description = "Retorna o usuario autenticado pela sessao atual.")
    public ResponseEntity<UserResponseDTO> me() {
        return ResponseEntity.ok(authService.me());
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalida a sessao atual.")
    public ResponseEntity<Void> logout() {
        authService.logout(currentUserContext.getRequiredToken());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/pin")
    @Operation(summary = "Configuracao de PIN", description = "Configura ou altera o PIN do usuario autenticado.")
    public ResponseEntity<Void> configurePin(@RequestBody @Valid UserUpdatePinRequestDTO dto) {
        authService.updatePin(dto);
        return ResponseEntity.ok().build();
    }
}
