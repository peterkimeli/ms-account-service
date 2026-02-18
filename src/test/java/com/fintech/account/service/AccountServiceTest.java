package com.fintech.account.service;

import com.fintech.account.dto.AccountRequestDTO;
import com.fintech.account.dto.AccountResponseDTO;
import com.fintech.account.dto.BalanceResponseDTO;
import com.fintech.account.exception.AccountNotFoundException;
import com.fintech.account.exception.BusinessRuleException;
import com.fintech.account.exception.CustomerNotFoundException;
import com.fintech.account.model.*;
import com.fintech.account.repository.AccountRepository;
import com.fintech.account.repository.CustomerRepository;
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
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private AccountService accountService;

    private Customer verifiedCustomer;
    private Customer unverifiedCustomer;
    private Account activeAccount;
    private AccountRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        verifiedCustomer = new Customer();
        verifiedCustomer.setId(1L);
        verifiedCustomer.setFirstName("John");
        verifiedCustomer.setLastName("Doe");
        verifiedCustomer.setEmail("john@example.com");
        verifiedCustomer.setPhone("+254712345678");
        verifiedCustomer.setKycStatus(KycStatus.VERIFIED);
        verifiedCustomer.setCreatedAt(LocalDateTime.now());

        unverifiedCustomer = new Customer();
        unverifiedCustomer.setId(2L);
        unverifiedCustomer.setFirstName("Jane");
        unverifiedCustomer.setLastName("Doe");
        unverifiedCustomer.setEmail("jane@example.com");
        unverifiedCustomer.setPhone("+254787654321");
        unverifiedCustomer.setKycStatus(KycStatus.PENDING);
        unverifiedCustomer.setCreatedAt(LocalDateTime.now());

        activeAccount = new Account();
        activeAccount.setId(1L);
        activeAccount.setAccountNumber("ACC2402191234561234");
        activeAccount.setCustomer(verifiedCustomer);
        activeAccount.setAccountType(AccountType.SAVINGS);
        activeAccount.setBalance(new BigDecimal("1000.00"));
        activeAccount.setCurrency("USD");
        activeAccount.setStatus(AccountStatus.ACTIVE);
        activeAccount.setCreatedAt(LocalDateTime.now());
        activeAccount.setUpdatedAt(LocalDateTime.now());

        validRequest = new AccountRequestDTO();
        validRequest.setCustomerId(1L);
        validRequest.setAccountType(AccountType.SAVINGS);
        validRequest.setCurrency("USD");
        validRequest.setInitialDeposit(new BigDecimal("500.00"));
    }

    @Nested
    @DisplayName("createAccount")
    class CreateAccount {

        @Test
        @DisplayName("should create account for verified customer")
        void shouldCreateAccountForVerifiedCustomer() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(verifiedCustomer));
            when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);

            AccountResponseDTO response = accountService.createAccount(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.getAccountNumber()).isEqualTo("ACC2402191234561234");
            assertThat(response.getAccountType()).isEqualTo(AccountType.SAVINGS);
            assertThat(response.getStatus()).isEqualTo(AccountStatus.ACTIVE);

            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should throw exception when customer not found")
        void shouldThrowExceptionWhenCustomerNotFound() {
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            validRequest.setCustomerId(999L);

            assertThatThrownBy(() -> accountService.createAccount(validRequest))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining("Customer not found");

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when customer KYC not verified")
        void shouldThrowExceptionWhenKycNotVerified() {
            when(customerRepository.findById(2L)).thenReturn(Optional.of(unverifiedCustomer));

            validRequest.setCustomerId(2L);

            assertThatThrownBy(() -> accountService.createAccount(validRequest))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("KYC must be verified");

            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should record initial deposit transaction when amount > 0")
        void shouldRecordInitialDepositTransaction() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(verifiedCustomer));
            when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);

            accountService.createAccount(validRequest);

            verify(transactionService).creditAccount(any());
        }

        @Test
        @DisplayName("should not record transaction when initial deposit is zero")
        void shouldNotRecordTransactionWhenInitialDepositIsZero() {
            validRequest.setInitialDeposit(BigDecimal.ZERO);
            when(customerRepository.findById(1L)).thenReturn(Optional.of(verifiedCustomer));
            when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);

            accountService.createAccount(validRequest);

            verify(transactionService, never()).creditAccount(any());
        }
    }

    @Nested
    @DisplayName("getAccountByNumber")
    class GetAccountByNumber {

        @Test
        @DisplayName("should return account when found")
        void shouldReturnAccountWhenFound() {
            when(accountRepository.findByAccountNumber("ACC2402191234561234"))
                    .thenReturn(Optional.of(activeAccount));

            AccountResponseDTO response = accountService.getAccountByNumber("ACC2402191234561234");

            assertThat(response).isNotNull();
            assertThat(response.getAccountNumber()).isEqualTo("ACC2402191234561234");
            assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("should throw exception when account not found")
        void shouldThrowExceptionWhenAccountNotFound() {
            when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getAccountByNumber("INVALID"))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("Account not found");
        }
    }

    @Nested
    @DisplayName("getAccountsByCustomerId")
    class GetAccountsByCustomerId {

        @Test
        @DisplayName("should return list of accounts for customer")
        void shouldReturnListOfAccountsForCustomer() {
            when(accountRepository.findByCustomerId(1L)).thenReturn(List.of(activeAccount));

            List<AccountResponseDTO> response = accountService.getAccountsByCustomerId(1L);

            assertThat(response).hasSize(1);
            assertThat(response.get(0).getCustomerId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should return empty list when no accounts")
        void shouldReturnEmptyListWhenNoAccounts() {
            when(accountRepository.findByCustomerId(999L)).thenReturn(List.of());

            List<AccountResponseDTO> response = accountService.getAccountsByCustomerId(999L);

            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateAccountStatus")
    class UpdateAccountStatus {

        @Test
        @DisplayName("should update account status successfully")
        void shouldUpdateAccountStatusSuccessfully() {
            when(accountRepository.findByAccountNumber("ACC2402191234561234"))
                    .thenReturn(Optional.of(activeAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);

            AccountResponseDTO response = accountService.updateAccountStatus(
                    "ACC2402191234561234", AccountStatus.SUSPENDED);

            assertThat(response).isNotNull();
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should throw exception when account not found")
        void shouldThrowExceptionWhenAccountNotFound() {
            when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.updateAccountStatus("INVALID", AccountStatus.SUSPENDED))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getBalance")
    class GetBalance {

        @Test
        @DisplayName("should return balance for account")
        void shouldReturnBalanceForAccount() {
            when(accountRepository.findByAccountNumber("ACC2402191234561234"))
                    .thenReturn(Optional.of(activeAccount));

            BalanceResponseDTO response = accountService.getBalance("ACC2402191234561234");

            assertThat(response.getAccountNumber()).isEqualTo("ACC2402191234561234");
            assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(response.getCurrency()).isEqualTo("USD");
        }
    }
}
