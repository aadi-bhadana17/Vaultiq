package com.kilgore.vaultiq.betting.controller;

import com.kilgore.vaultiq.betting.dto.TransactionResponse;
import com.kilgore.vaultiq.betting.dto.WalletRequest;
import com.kilgore.vaultiq.betting.dto.WalletResponse;
import com.kilgore.vaultiq.betting.service.WalletService;
import com.kilgore.vaultiq.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<WalletResponse>> deposit(@Valid @RequestBody WalletRequest request) {
        WalletResponse response = walletService.deposit(request.getAmount());
        return ResponseEntity.ok(ApiResponse.success("Deposit successful", response));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WalletResponse>> withdraw(@Valid @RequestBody WalletRequest request) {
        WalletResponse response = walletService.withdraw(request.getAmount());
        return ResponseEntity.ok(ApiResponse.success("Withdrawal successful", response));
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<WalletResponse>> getBalance() {
        WalletResponse response = walletService.getBalance();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<TransactionResponse> transactions = walletService.getTransactionHistory(page, size);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }
}
