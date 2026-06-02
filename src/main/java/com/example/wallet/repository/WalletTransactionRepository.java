package com.example.wallet.repository;

import com.example.wallet.entity.WalletTransaction;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    @EntityGraph(attributePaths = {"sender", "receiver"})
    List<WalletTransaction> findAllByOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = {"sender", "receiver"})
    Optional<WalletTransaction> findById(UUID id);
}