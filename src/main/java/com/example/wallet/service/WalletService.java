package com.example.wallet.service;

import com.example.wallet.dto.AuthDtos;
import com.example.wallet.entity.LedgerEntry;
import com.example.wallet.entity.LedgerEntryType;
import com.example.wallet.entity.TransactionStatus;
import com.example.wallet.entity.TransactionType;
import com.example.wallet.entity.User;
import com.example.wallet.entity.Wallet;
import com.example.wallet.entity.WalletTransaction;
import com.example.wallet.exception.BadRequestException;
import com.example.wallet.exception.InsufficientBalanceException;
import com.example.wallet.exception.ResourceNotFoundException;
import com.example.wallet.repository.LedgerEntryRepository;
import com.example.wallet.repository.UserRepository;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionRecordService transactionRecordService;
    private final TransactionLedgerService transactionLedgerService;

    public WalletService(CurrentUserService currentUserService, UserRepository userRepository, WalletRepository walletRepository,
                         WalletTransactionRepository walletTransactionRepository, LedgerEntryRepository ledgerEntryRepository,
                         TransactionRecordService transactionRecordService, TransactionLedgerService transactionLedgerService) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.transactionRecordService = transactionRecordService;
        this.transactionLedgerService = transactionLedgerService;
    }

    public AuthDtos.BalanceResponse getBalance() {
        return new AuthDtos.BalanceResponse(getCurrentUserWallet().getBalance());
    }

    @Transactional
    public AuthDtos.WalletResponse addMoney(AuthDtos.AddMoneyRequest request) {
        validateAmount(request.amount());
        User currentUser = currentUserService.getCurrentUser();
        Wallet wallet = walletRepository.findByUserIdForUpdate(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        wallet.setBalance(normalize(wallet.getBalance().add(request.amount())));
        wallet = walletRepository.save(wallet);

        WalletTransaction transaction = transactionRecordService.create(null, currentUser, request.amount(), TransactionType.ADD_MONEY, TransactionStatus.SUCCESS, "Wallet top-up");
        transactionLedgerService.record(wallet, transaction, LedgerEntryType.CREDIT, request.amount(), wallet.getBalance());
        return toWalletResponse(wallet);
    }

    @Transactional
    public AuthDtos.WalletResponse withdrawMoney(AuthDtos.WithdrawMoneyRequest request) {
        validateAmount(request.amount());
        User currentUser = currentUserService.getCurrentUser();
        Wallet wallet = walletRepository.findByUserIdForUpdate(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (wallet.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient wallet balance");
        }
        wallet.setBalance(normalize(wallet.getBalance().subtract(request.amount())));
        wallet = walletRepository.save(wallet);

        WalletTransaction transaction = transactionRecordService.create(currentUser, null, request.amount(), TransactionType.WITHDRAW, TransactionStatus.SUCCESS, "Wallet withdrawal");
        transactionLedgerService.record(wallet, transaction, LedgerEntryType.DEBIT, request.amount(), wallet.getBalance());
        return toWalletResponse(wallet);
    }

    @Transactional
    public AuthDtos.TransactionResponse transferMoney(AuthDtos.TransferRequest request) {
        validateAmount(request.amount());
        User sender = currentUserService.getCurrentUser();
        User receiver = userRepository.findByEmailIgnoreCase(request.receiverEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver user not found"));
        if (sender.getId().equals(receiver.getId())) {
            throw new BadRequestException("Sender and receiver cannot be the same user");
        }

        Wallet senderWallet;
        Wallet receiverWallet;
        if (sender.getId() < receiver.getId()) {
            senderWallet = walletRepository.findByUserIdForUpdate(sender.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));
            receiverWallet = walletRepository.findByUserIdForUpdate(receiver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found"));
        } else {
            receiverWallet = walletRepository.findByUserIdForUpdate(receiver.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found"));
            senderWallet = walletRepository.findByUserIdForUpdate(sender.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));
        }

        if (senderWallet.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientBalanceException("Insufficient wallet balance for transfer");
        }

        senderWallet.setBalance(normalize(senderWallet.getBalance().subtract(request.amount())));
        receiverWallet.setBalance(normalize(receiverWallet.getBalance().add(request.amount())));
        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        WalletTransaction transaction = transactionRecordService.create(sender, receiver, request.amount(), TransactionType.TRANSFER, TransactionStatus.SUCCESS, request.description() == null ? "User transfer" : request.description());
        transactionLedgerService.record(senderWallet, transaction, LedgerEntryType.DEBIT, request.amount(), senderWallet.getBalance());
        transactionLedgerService.record(receiverWallet, transaction, LedgerEntryType.CREDIT, request.amount(), receiverWallet.getBalance());
        return toTransactionResponse(transaction);
    }

    public List<AuthDtos.TransactionResponse> getMyTransactions() {
        User currentUser = currentUserService.getCurrentUser();
        return walletTransactionRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(transaction -> isParticipant(transaction, currentUser))
                .map(this::toTransactionResponse)
                .toList();
    }

    private Wallet getCurrentUserWallet() {
        return walletRepository.findByUser_Id(currentUserService.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private boolean isParticipant(WalletTransaction transaction, User user) {
        return (transaction.getSender() != null && transaction.getSender().getId().equals(user.getId()))
                || (transaction.getReceiver() != null && transaction.getReceiver().getId().equals(user.getId()));
    }

    private AuthDtos.TransactionResponse toTransactionResponse(WalletTransaction transaction) {
        List<AuthDtos.LedgerEntryResponse> ledgerEntries = ledgerEntryRepository.findByTransaction_IdOrderByIdAsc(transaction.getId()).stream()
                .map(entry -> new AuthDtos.LedgerEntryResponse(
                        entry.getEntryType().name(),
                        entry.getAmount(),
                    entry.getBalanceAfter(),
                        entry.getWallet().getUser().getEmail()))
                .toList();
        return new AuthDtos.TransactionResponse(
                transaction.getId().toString(),
                transaction.getSender() != null ? transaction.getSender().getEmail() : "SYSTEM",
                transaction.getReceiver() != null ? transaction.getReceiver().getEmail() : "SYSTEM",
                transaction.getAmount(),
                transaction.getTransactionType().name(),
                transaction.getStatus().name(),
                transaction.getDescription(),
                transaction.getCreatedAt(),
                ledgerEntries);
    }

    private AuthDtos.WalletResponse toWalletResponse(Wallet wallet) {
        return new AuthDtos.WalletResponse(wallet.getId(), wallet.getBalance(), wallet.getUpdatedAt());
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
    }

    private BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}