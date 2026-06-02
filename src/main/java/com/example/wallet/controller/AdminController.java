package com.example.wallet.controller;

import com.example.wallet.dto.AdminDtos;
import com.example.wallet.dto.AuthDtos;
import com.example.wallet.service.AdminService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminDtos.AdminUserResponse>> users() {
        return ResponseEntity.ok(adminService.getUsers());
    }

    @GetMapping("/wallets")
    public ResponseEntity<List<AdminDtos.AdminWalletResponse>> wallets() {
        return ResponseEntity.ok(adminService.getWallets());
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<AdminDtos.AdminTransactionLookupResponse>> transactions() {
        return ResponseEntity.ok(adminService.getTransactions());
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<AdminDtos.AdminTransactionLookupResponse> transactionById(@PathVariable String transactionId) {
        return ResponseEntity.ok(adminService.getTransactionById(transactionId));
    }

    @GetMapping("/balances/summary")
    public ResponseEntity<AuthDtos.AdminWalletSummaryResponse> summary() {
        return ResponseEntity.ok(adminService.getBalanceSummary());
    }
}