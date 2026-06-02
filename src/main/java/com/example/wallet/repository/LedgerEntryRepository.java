package com.example.wallet.repository;

import com.example.wallet.entity.LedgerEntry;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    @EntityGraph(attributePaths = {"transaction", "wallet", "wallet.user"})
    List<LedgerEntry> findByTransaction_IdOrderByIdAsc(java.util.UUID transactionId);
}