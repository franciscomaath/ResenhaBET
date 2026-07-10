package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.request.UserRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.UserPatchRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.UserResponseDTO;
import com.franciscomaath.resenhaapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Lista usuarios", description = "Lista usuarios para selecao no login.")
    public ResponseEntity<List<UserResponseDTO>> getUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @PostMapping
    @Operation(summary = "Criacao de usuario", description = "Cria um novo usuario.")
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody UserRequestDTO dto) {
        return ResponseEntity.ok(userService.create(dto));
    }

    @PatchMapping("/{id}/reset-pin")
    @Operation(summary = "Reset de PIN", description = "Reseta o PIN de um usuario. Admin only.")
    public ResponseEntity<Void> resetPin(@PathVariable Long id) {
        userService.resetPin(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do usuario", description = "Retorna detalhes de um usuario especifico.")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Editar usuario", description = "Edita nome ou dados basicos do usuario.")
    public ResponseEntity<UserResponseDTO> editUser(
            @PathVariable Long id,
            @RequestBody @Valid UserPatchRequestDTO dto
    ) {
        return ResponseEntity.ok(userService.editUser(id, dto));
    }
}
