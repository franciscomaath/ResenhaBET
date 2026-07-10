package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.request.WalletDepositRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.WalletResponseDTO;
import com.franciscomaath.resenhaapi.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<WalletResponseDTO> me(
            @RequestParam Long userId,
            @RequestParam Long tournamentId
    ){
        return ResponseEntity.ok(walletService.me(userId, tournamentId));
    }

    @PostMapping("/deposit")
    @Operation(summary = "Deposit in a wallet", description = "Deposits balance into a user's wallet. Admin only.")
    public ResponseEntity<BigDecimal> deposit(
            @RequestBody @Valid WalletDepositRequestDTO dto
    ) {
        return ResponseEntity.ok(walletService.deposit(dto));
    }

    @PostMapping("/deposit-all")
    @Operation(summary = "Deposit in a wallet", description = "Deposits balance into a user's wallet. Admin only.")
    public ResponseEntity<Void> depositAll(
            @RequestParam Long tournamentId,
            @RequestParam @Valid BigDecimal amount
    ) {
        walletService.depositAll(tournamentId, amount);
        return ResponseEntity.ok().build();
    }

}
