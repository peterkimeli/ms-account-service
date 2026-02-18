package com.fintech.account.service;

import com.fintech.account.dto.TransactionRequestDTO;
import com.fintech.account.dto.TransactionResponseDTO;
import com.fintech.account.exception.AccountNotFoundException;
import com.fintech.account.exception.BusinessRuleException;
import com.fintech.account.exception.InsufficientBalanceException;
import com.fintech.account.model.*;
import com.fintech.account.repository.AccountRepository;
import com.fintech.account.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public TransactionResponseDTO creditAccount(TransactionRequestDTO request) {
        log.info("Crediting account: {} with amount: {}", request.getAccountNumber(), request.getAmount());

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot credit inactive account: " + request.getAccountNumber());
        }

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(request.getAmount());
        account.setBalance(balanceAfter);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(TransactionType.CREDIT);
        transaction.setAmount(request.getAmount());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(request.getDescription());
        transaction.setStatus(TransactionStatus.COMPLETED);

        Transaction savedTransaction = transactionRepository.save(transaction);
        accountRepository.save(account);

        log.info("Account credited successfully. Transaction ID: {}", savedTransaction.getId());
        return mapToResponseDTO(savedTransaction);
    }

    @Transactional
    public TransactionResponseDTO debitAccount(TransactionRequestDTO request) {
        log.info("Debiting account: {} with amount: {}", request.getAccountNumber(), request.getAmount());

        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + request.getAccountNumber()));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot debit inactive account: " + request.getAccountNumber());
        }

        BigDecimal balanceBefore = account.getBalance();
        if (balanceBefore.compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance. Available: " + balanceBefore + ", Required: " + request.getAmount());
        }

        BigDecimal balanceAfter = balanceBefore.subtract(request.getAmount());
        account.setBalance(balanceAfter);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(TransactionType.DEBIT);
        transaction.setAmount(request.getAmount());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(request.getDescription());
        transaction.setStatus(TransactionStatus.COMPLETED);

        Transaction savedTransaction = transactionRepository.save(transaction);
        accountRepository.save(account);

        log.info("Account debited successfully. Transaction ID: {}", savedTransaction.getId());
        return mapToResponseDTO(savedTransaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponseDTO> getTransactionHistory(String accountNumber) {
        log.info("Fetching transaction history for account: {}", accountNumber);
        
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        List<Transaction> transactions = transactionRepository
                .findByAccountAccountNumberOrderByTransactionDateDesc(accountNumber);

        return transactions.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransactionResponseDTO getTransactionById(Long transactionId) {
        log.info("Fetching transaction with ID: {}", transactionId);
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessRuleException("Transaction not found with ID: " + transactionId));
        return mapToResponseDTO(transaction);
    }

    private TransactionResponseDTO mapToResponseDTO(Transaction transaction) {
        TransactionResponseDTO dto = new TransactionResponseDTO();
        dto.setId(transaction.getId());
        dto.setAccountNumber(transaction.getAccount().getAccountNumber());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setAmount(transaction.getAmount());
        dto.setBalanceBefore(transaction.getBalanceBefore());
        dto.setBalanceAfter(transaction.getBalanceAfter());
        dto.setDescription(transaction.getDescription());
        dto.setStatus(transaction.getStatus());
        dto.setTransactionDate(transaction.getTransactionDate());
        return dto;
    }
}