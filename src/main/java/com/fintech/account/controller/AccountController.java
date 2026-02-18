package com.fintech.account.controller;

import com.fintech.account.dto.AccountRequestDTO;
import com.fintech.account.dto.AccountResponseDTO;
import com.fintech.account.dto.BalanceResponseDTO;
import com.fintech.account.model.AccountStatus;
import com.fintech.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "APIs for managing customer accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Create a new account")
    public ResponseEntity<AccountResponseDTO> createAccount(@Valid @RequestBody AccountRequestDTO request) {
        AccountResponseDTO response = accountService.createAccount(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get account by account number")
    public ResponseEntity<AccountResponseDTO> getAccount(@PathVariable String accountNumber) {
        AccountResponseDTO response = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all accounts for a customer")
    public ResponseEntity<List<AccountResponseDTO>> getAccountsByCustomer(@PathVariable Long customerId) {
        List<AccountResponseDTO> response = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{accountNumber}")
    @Operation(summary = "Update account details")
    public ResponseEntity<AccountResponseDTO> updateAccount(
            @PathVariable String accountNumber,
            @Valid @RequestBody AccountRequestDTO request) {
        AccountResponseDTO response = accountService.updateAccount(accountNumber, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{accountNumber}/status")
    @Operation(summary = "Update account status")
    public ResponseEntity<AccountResponseDTO> updateAccountStatus(
            @PathVariable String accountNumber,
            @RequestParam AccountStatus status) {
        AccountResponseDTO response = accountService.updateAccountStatus(accountNumber, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNumber}/balance")
    @Operation(summary = "Get account balance")
    public ResponseEntity<BalanceResponseDTO> getBalance(@PathVariable String accountNumber) {
        BalanceResponseDTO response = accountService.getBalance(accountNumber);
        return ResponseEntity.ok(response);
    }
}