package com.example.wallet.service;

import com.example.wallet.entity.LedgerEntry;
import com.example.wallet.entity.LedgerEntryType;
import com.example.wallet.entity.Wallet;
import com.example.wallet.entity.WalletTransaction;
import com.example.wallet.repository.LedgerEntryRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionLedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public TransactionLedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public LedgerEntry record(Wallet wallet, WalletTransaction transaction, LedgerEntryType entryType, BigDecimal amount, BigDecimal balanceAfter) {
        LedgerEntry ledgerEntry = new LedgerEntry(wallet, transaction, entryType, amount);
        ledgerEntry.setBalanceAfter(balanceAfter);
        return ledgerEntryRepository.save(ledgerEntry);
    }
}