package com.fintech.account.service;

import com.fintech.account.dto.TransactionRequestDTO;
import com.fintech.account.dto.TransactionResponseDTO;
import com.fintech.account.exception.AccountNotFoundException;
import com.fintech.account.exception.BusinessRuleException;
import com.fintech.account.exception.InsufficientBalanceException;
import com.fintech.account.model.*;
import com.fintech.account.repository.AccountRepository;
import com.fintech.account.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account activeAccount;
    private Account inactiveAccount;
    private TransactionRequestDTO validRequest;
    private Transaction savedTransaction;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john@example.com");
        customer.setPhone("+254712345678");
        customer.setKycStatus(KycStatus.VERIFIED);

        activeAccount = new Account();
        activeAccount.setId(1L);
        activeAccount.setAccountNumber("ACC001");
        activeAccount.setCustomer(customer);
        activeAccount.setAccountType(AccountType.SAVINGS);
        activeAccount.setBalance(new BigDecimal("1000.00"));
        activeAccount.setCurrency("USD");
        activeAccount.setStatus(AccountStatus.ACTIVE);

        inactiveAccount = new Account();
        inactiveAccount.setId(2L);
        inactiveAccount.setAccountNumber("ACC002");
        inactiveAccount.setCustomer(customer);
        inactiveAccount.setAccountType(AccountType.SAVINGS);
        inactiveAccount.setBalance(new BigDecimal("500.00"));
        inactiveAccount.setCurrency("USD");
        inactiveAccount.setStatus(AccountStatus.SUSPENDED);

        validRequest = new TransactionRequestDTO();
        validRequest.setAccountNumber("ACC001");
        validRequest.setAmount(new BigDecimal("200.00"));
        validRequest.setDescription("Test transaction");

        savedTransaction = new Transaction();
        savedTransaction.setId(1L);
        savedTransaction.setAccount(activeAccount);
        savedTransaction.setTransactionType(TransactionType.CREDIT);
        savedTransaction.setAmount(new BigDecimal("200.00"));
        savedTransaction.setBalanceBefore(new BigDecimal("1000.00"));
        savedTransaction.setBalanceAfter(new BigDecimal("1200.00"));
        savedTransaction.setDescription("Test transaction");
        savedTransaction.setStatus(TransactionStatus.COMPLETED);
        savedTransaction.setTransactionDate(LocalDateTime.now());
    }

    @Nested
    @DisplayName("creditAccount")
    class CreditAccount {

        @Test
        @DisplayName("should credit account successfully")
        void shouldCreditAccountSuccessfully() {
            when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(activeAccount));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
            when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);

            TransactionResponseDTO response = transactionService.creditAccount(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.getTransactionType()).isEqualTo(TransactionType.CREDIT);
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

            verify(transactionRepository).save(any(Transaction.class));
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should throw exception when account not found")
        void shouldThrowExceptionWhenAccountNotFound() {
            when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

            validRequest.setAccountNumber("INVALID");

            assertThatThrownBy(() -> transactionService.creditAccount(validRequest))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("Account not found");
        }

        @Test
        @DisplayName("should throw exception when account is inactive")
        void shouldThrowExceptionWhenAccountIsInactive() {
            when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(inactiveAccount));

            validRequest.setAccountNumber("ACC002");

            assertThatThrownBy(() -> transactionService.creditAccount(validRequest))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot credit inactive account");
        }
    }

    @Nested
    @DisplayName("debitAccount")
    class DebitAccount {

        @Test
        @DisplayName("should debit account successfully")
        void shouldDebitAccountSuccessfully() {
            Transaction debitTx = new Transaction();
            debitTx.setId(2L);
            debitTx.setAccount(activeAccount);
            debitTx.setTransactionType(TransactionType.DEBIT);
            debitTx.setAmount(new BigDecimal("200.00"));
            debitTx.setBalanceBefore(new BigDecimal("1000.00"));
            debitTx.setBalanceAfter(new BigDecimal("800.00"));
            debitTx.setDescription("Test debit");
            debitTx.setStatus(TransactionStatus.COMPLETED);
            debitTx.setTransactionDate(LocalDateTime.now());

            when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(activeAccount));
            when(transactionRepository.save(any(Transaction.class))).thenReturn(debitTx);
            when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);

            TransactionResponseDTO response = transactionService.debitAccount(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.getTransactionType()).isEqualTo(TransactionType.DEBIT);
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));

            verify(transactionRepository).save(any(Transaction.class));
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should throw exception when insufficient balance")
        void shouldThrowExceptionWhenInsufficientBalance() {
            when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(activeAccount));

            validRequest.setAmount(new BigDecimal("5000.00")); // more than 1000 balance

            assertThatThrownBy(() -> transactionService.debitAccount(validRequest))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessageContaining("Insufficient balance");

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when account is inactive")
        void shouldThrowExceptionWhenAccountIsInactive() {
            when(accountRepository.findByAccountNumber("ACC002")).thenReturn(Optional.of(inactiveAccount));

            validRequest.setAccountNumber("ACC002");

            assertThatThrownBy(() -> transactionService.debitAccount(validRequest))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot debit inactive account");
        }

        @Test
        @DisplayName("should throw exception when account not found")
        void shouldThrowExceptionWhenAccountNotFound() {
            when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

            validRequest.setAccountNumber("INVALID");

            assertThatThrownBy(() -> transactionService.debitAccount(validRequest))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getTransactionHistory")
    class GetTransactionHistory {

        @Test
        @DisplayName("should return transaction history")
        void shouldReturnTransactionHistory() {
            when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(activeAccount));
            when(transactionRepository.findByAccountAccountNumberOrderByTransactionDateDesc("ACC001"))
                    .thenReturn(List.of(savedTransaction));

            List<TransactionResponseDTO> history = transactionService.getTransactionHistory("ACC001");

            assertThat(history).hasSize(1);
            assertThat(history.get(0).getAccountNumber()).isEqualTo("ACC001");
        }

        @Test
        @DisplayName("should throw exception when account not found")
        void shouldThrowExceptionWhenAccountNotFound() {
            when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.getTransactionHistory("INVALID"))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getTransactionById")
    class GetTransactionById {

        @Test
        @DisplayName("should return transaction when found")
        void shouldReturnTransactionWhenFound() {
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(savedTransaction));

            TransactionResponseDTO response = transactionService.getTransactionById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw exception when transaction not found")
        void shouldThrowExceptionWhenTransactionNotFound() {
            when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.getTransactionById(999L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Transaction not found");
        }
    }
}
