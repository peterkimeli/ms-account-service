package com.fintech.account.service;

import com.fintech.account.dto.*;
import com.fintech.account.exception.*;
import com.fintech.account.model.*;
import com.fintech.account.repository.AccountRepository;
import com.fintech.account.repository.CustomerRepository;
import com.fintech.account.util.AccountNumberGenerator;
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
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionService transactionService;

    @Transactional
    public AccountResponseDTO createAccount(AccountRequestDTO request) {
        log.info("Creating account for customer ID: {}", request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + request.getCustomerId()));

        if (customer.getKycStatus() != KycStatus.VERIFIED) {
            throw new BusinessRuleException("Customer KYC must be verified before creating an account");
        }

        Account account = new Account();
        account.setAccountNumber(AccountNumberGenerator.generate());
        account.setCustomer(customer);
        account.setAccountType(request.getAccountType());
        account.setCurrency(request.getCurrency());
        account.setBalance(request.getInitialDeposit());
        account.setStatus(AccountStatus.ACTIVE);

        Account savedAccount = accountRepository.save(account);

        // Record initial deposit transaction if any
        if (request.getInitialDeposit().compareTo(BigDecimal.ZERO) > 0) {
            TransactionRequestDTO transactionRequest = new TransactionRequestDTO();
            transactionRequest.setAccountNumber(savedAccount.getAccountNumber());
            transactionRequest.setAmount(request.getInitialDeposit());
            transactionRequest.setDescription("Initial deposit");
            transactionService.creditAccount(transactionRequest);
        }

        log.info("Account created successfully: {}", savedAccount.getAccountNumber());
        return mapToResponseDTO(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponseDTO getAccountByNumber(String accountNumber) {
        log.info("Fetching account: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return mapToResponseDTO(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponseDTO> getAccountsByCustomerId(Long customerId) {
        log.info("Fetching accounts for customer ID: {}", customerId);
        List<Account> accounts = accountRepository.findByCustomerId(customerId);
        return accounts.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponseDTO updateAccount(String accountNumber, AccountRequestDTO request) {
        log.info("Updating account: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        account.setAccountType(request.getAccountType());
        account.setCurrency(request.getCurrency());

        Account updatedAccount = accountRepository.save(account);
        log.info("Account updated successfully: {}", accountNumber);
        return mapToResponseDTO(updatedAccount);
    }

    @Transactional
    public AccountResponseDTO updateAccountStatus(String accountNumber, AccountStatus status) {
        log.info("Updating account status for {}: {}", accountNumber, status);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        account.setStatus(status);
        Account updatedAccount = accountRepository.save(account);
        log.info("Account status updated successfully: {}", accountNumber);
        return mapToResponseDTO(updatedAccount);
    }

    @Transactional(readOnly = true)
    public BalanceResponseDTO getBalance(String accountNumber) {
        log.info("Fetching balance for account: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        return new BalanceResponseDTO(
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency()
        );
    }

    private AccountResponseDTO mapToResponseDTO(Account account) {
        AccountResponseDTO dto = new AccountResponseDTO();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setCustomerId(account.getCustomer().getId());
        dto.setCustomerName(account.getCustomer().getFirstName() + " " + account.getCustomer().getLastName());
        dto.setAccountType(account.getAccountType());
        dto.setBalance(account.getBalance());
        dto.setCurrency(account.getCurrency());
        dto.setStatus(account.getStatus());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        return dto;
    }
}