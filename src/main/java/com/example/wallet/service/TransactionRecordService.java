package com.example.wallet.service;

import com.example.wallet.entity.TransactionStatus;
import com.example.wallet.entity.TransactionType;
import com.example.wallet.entity.User;
import com.example.wallet.entity.WalletTransaction;
import com.example.wallet.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionRecordService {

    private final WalletTransactionRepository walletTransactionRepository;

    public TransactionRecordService(WalletTransactionRepository walletTransactionRepository) {
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional
    public WalletTransaction create(User sender, User receiver, BigDecimal amount, TransactionType type, TransactionStatus status, String description) {
        WalletTransaction transaction = new WalletTransaction(sender, receiver, amount, type, status, description);
        return walletTransactionRepository.save(transaction);
    }
}