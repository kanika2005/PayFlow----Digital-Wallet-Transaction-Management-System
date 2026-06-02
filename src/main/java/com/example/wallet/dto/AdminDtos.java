package com.example.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record AdminUserResponse(Long id, String fullName, String email, String role, boolean enabled, Instant createdAt) {
    }

    public record AdminWalletResponse(Long id, String ownerEmail, BigDecimal balance, Instant createdAt, Instant updatedAt) {
    }

    public record AdminTransactionLookupResponse(
            String transactionId,
            String sender,
            String receiver,
            BigDecimal amount,
            String type,
            String status,
            String description,
            Instant createdAt) {
    }
}