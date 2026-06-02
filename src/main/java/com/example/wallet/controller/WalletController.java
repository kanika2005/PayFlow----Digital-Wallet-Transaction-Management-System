package com.example.wallet.controller;

import com.example.wallet.dto.AuthDtos;
import com.example.wallet.service.WalletService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    public ResponseEntity<AuthDtos.BalanceResponse> balance() {
        return ResponseEntity.ok(walletService.getBalance());
    }

    @PostMapping("/add-money")
    public ResponseEntity<AuthDtos.WalletResponse> addMoney(@Valid @RequestBody AuthDtos.AddMoneyRequest request) {
        return ResponseEntity.ok(walletService.addMoney(request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<AuthDtos.WalletResponse> withdraw(@Valid @RequestBody AuthDtos.WithdrawMoneyRequest request) {
        return ResponseEntity.ok(walletService.withdrawMoney(request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<AuthDtos.TransactionResponse> transfer(@Valid @RequestBody AuthDtos.TransferRequest request) {
        return ResponseEntity.ok(walletService.transferMoney(request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<AuthDtos.TransactionResponse>> transactions() {
        return ResponseEntity.ok(walletService.getMyTransactions());
    }
}