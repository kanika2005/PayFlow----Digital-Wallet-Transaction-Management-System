package com.example.wallet.service;

import com.example.wallet.dto.AdminDtos;
import com.example.wallet.dto.AuthDtos;
import com.example.wallet.entity.WalletTransaction;
import com.example.wallet.exception.ResourceNotFoundException;
import com.example.wallet.repository.UserRepository;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.repository.WalletTransactionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public AdminService(UserRepository userRepository, WalletRepository walletRepository, WalletTransactionRepository walletTransactionRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    public List<AdminDtos.AdminUserResponse> getUsers() {
        return userRepository.findAllByOrderByIdAsc().stream()
                .map(user -> new AdminDtos.AdminUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().getName().name(), user.isEnabled(), user.getCreatedAt()))
                .toList();
    }

    public List<AdminDtos.AdminWalletResponse> getWallets() {
        return walletRepository.findAll().stream()
                .map(wallet -> new AdminDtos.AdminWalletResponse(wallet.getId(), wallet.getUser().getEmail(), wallet.getBalance(), wallet.getCreatedAt(), wallet.getUpdatedAt()))
                .toList();
    }

    public List<AdminDtos.AdminTransactionLookupResponse> getTransactions() {
        return walletTransactionRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapTransaction).toList();
    }

    public AdminDtos.AdminTransactionLookupResponse getTransactionById(String transactionId) {
        WalletTransaction transaction = walletTransactionRepository.findById(java.util.UUID.fromString(transactionId))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        return mapTransaction(transaction);
    }

    public AuthDtos.AdminWalletSummaryResponse getBalanceSummary() {
        return new AuthDtos.AdminWalletSummaryResponse(walletRepository.sumAllBalances(), walletRepository.countAllWallets());
    }

    private AdminDtos.AdminTransactionLookupResponse mapTransaction(WalletTransaction transaction) {
        return new AdminDtos.AdminTransactionLookupResponse(
                transaction.getId().toString(),
                transaction.getSender() != null ? transaction.getSender().getEmail() : "SYSTEM",
                transaction.getReceiver() != null ? transaction.getReceiver().getEmail() : "SYSTEM",
                transaction.getAmount(),
                transaction.getTransactionType().name(),
                transaction.getStatus().name(),
                transaction.getDescription(),
                transaction.getCreatedAt());
    }
}