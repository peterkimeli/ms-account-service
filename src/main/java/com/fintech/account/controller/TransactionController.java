package com.fintech.account.controller;

import com.fintech.account.dto.TransactionRequestDTO;
import com.fintech.account.dto.TransactionResponseDTO;
import com.fintech.account.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "APIs for managing account transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/credit")
    @Operation(summary = "Credit an account")
    public ResponseEntity<TransactionResponseDTO> creditAccount(@Valid @RequestBody TransactionRequestDTO request) {
        TransactionResponseDTO response = transactionService.creditAccount(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/debit")
    @Operation(summary = "Debit an account")
    public ResponseEntity<TransactionResponseDTO> debitAccount(@Valid @RequestBody TransactionRequestDTO request) {
        TransactionResponseDTO response = transactionService.debitAccount(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get transaction history for an account")
    public ResponseEntity<List<TransactionResponseDTO>> getTransactionHistory(@PathVariable String accountNumber) {
        List<TransactionResponseDTO> response = transactionService.getTransactionHistory(accountNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/details/{transactionId}")
    @Operation(summary = "Get transaction details by ID")
    public ResponseEntity<TransactionResponseDTO> getTransaction(@PathVariable Long transactionId) {
        TransactionResponseDTO response = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(response);
    }
}