package com.example.wallet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 100) String password) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record AuthResponse(String token, String tokenType, Instant expiresAt, UserResponse user) {
    }

    public record UserResponse(Long id, String fullName, String email, String role, boolean enabled) {
    }

    public record WalletResponse(Long id, BigDecimal balance, Instant updatedAt) {
    }

    public record BalanceResponse(BigDecimal balance) {
    }

    public record TransactionResponse(
            String transactionId,
            String sender,
            String receiver,
            BigDecimal amount,
            String type,
            String status,
            String description,
            Instant createdAt,
            List<LedgerEntryResponse> ledgerEntries) {
    }

    public record LedgerEntryResponse(String entryType, BigDecimal amount, BigDecimal balanceAfter, String walletOwner) {
    }

    public record AddMoneyRequest(@NotNull BigDecimal amount) {
    }

    public record WithdrawMoneyRequest(@NotNull BigDecimal amount) {
    }

    public record TransferRequest(@NotBlank @Email String receiverEmail, @NotNull BigDecimal amount, String description) {
    }

    public record AdminWalletSummaryResponse(BigDecimal totalSystemBalance, long walletCount) {
    }
}