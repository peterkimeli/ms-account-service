package com.fintech.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.account.dto.AccountRequestDTO;
import com.fintech.account.dto.AccountResponseDTO;
import com.fintech.account.dto.BalanceResponseDTO;
import com.fintech.account.exception.AccountNotFoundException;
import com.fintech.account.exception.BusinessRuleException;
import com.fintech.account.exception.CustomerNotFoundException;
import com.fintech.account.model.AccountStatus;
import com.fintech.account.model.AccountType;
import com.fintech.account.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    private AccountResponseDTO buildAccountResponse(String accountNumber) {
        AccountResponseDTO response = new AccountResponseDTO();
        response.setId(1L);
        response.setAccountNumber(accountNumber);
        response.setCustomerId(1L);
        response.setCustomerName("John Doe");
        response.setAccountType(AccountType.SAVINGS);
        response.setBalance(BigDecimal.valueOf(1000));
        response.setCurrency("USD");
        response.setStatus(AccountStatus.ACTIVE);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    @Test
    @DisplayName("POST /api/v1/accounts - should create account")
    void shouldCreateAccount() throws Exception {
        AccountRequestDTO request = new AccountRequestDTO();
        request.setCustomerId(1L);
        request.setAccountType(AccountType.SAVINGS);
        request.setCurrency("USD");
        request.setInitialDeposit(BigDecimal.valueOf(500));

        AccountResponseDTO response = buildAccountResponse("ACC20240101120000001234");

        when(accountService.createAccount(any(AccountRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("ACC20240101120000001234"))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/accounts - should return 400 for missing required fields")
    void shouldReturn400ForInvalidAccountRequest() throws Exception {
        AccountRequestDTO request = new AccountRequestDTO();
        // missing customerId and accountType

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/accounts - should return 404 when customer not found")
    void shouldReturn404WhenCustomerNotFoundOnCreate() throws Exception {
        AccountRequestDTO request = new AccountRequestDTO();
        request.setCustomerId(999L);
        request.setAccountType(AccountType.SAVINGS);
        request.setCurrency("USD");

        when(accountService.createAccount(any(AccountRequestDTO.class)))
                .thenThrow(new CustomerNotFoundException("Customer not found with ID: 999"));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/accounts - should return 409 when KYC not verified")
    void shouldReturn409WhenKycNotVerified() throws Exception {
        AccountRequestDTO request = new AccountRequestDTO();
        request.setCustomerId(1L);
        request.setAccountType(AccountType.SAVINGS);
        request.setCurrency("USD");

        when(accountService.createAccount(any(AccountRequestDTO.class)))
                .thenThrow(new BusinessRuleException("KYC must be VERIFIED"));

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNumber} - should return account")
    void shouldReturnAccount() throws Exception {
        AccountResponseDTO response = buildAccountResponse("ACC20240101120000001234");
        when(accountService.getAccountByNumber("ACC20240101120000001234")).thenReturn(response);

        mockMvc.perform(get("/api/v1/accounts/ACC20240101120000001234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC20240101120000001234"))
                .andExpect(jsonPath("$.balance").value(1000));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNumber} - should return 404 when not found")
    void shouldReturn404WhenAccountNotFound() throws Exception {
        when(accountService.getAccountByNumber("INVALID"))
                .thenThrow(new AccountNotFoundException("Account not found: INVALID"));

        mockMvc.perform(get("/api/v1/accounts/INVALID"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/accounts/customer/{customerId} - should return customer accounts")
    void shouldReturnCustomerAccounts() throws Exception {
        AccountResponseDTO acc1 = buildAccountResponse("ACC001");
        AccountResponseDTO acc2 = buildAccountResponse("ACC002");
        acc2.setAccountNumber("ACC002");
        acc2.setAccountType(AccountType.CHECKING);

        when(accountService.getAccountsByCustomerId(1L)).thenReturn(List.of(acc1, acc2));

        mockMvc.perform(get("/api/v1/accounts/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].accountNumber").value("ACC001"))
                .andExpect(jsonPath("$[1].accountType").value("CHECKING"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/customer/{customerId} - should return empty list")
    void shouldReturnEmptyListForCustomerWithNoAccounts() throws Exception {
        when(accountService.getAccountsByCustomerId(999L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/accounts/customer/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("PATCH /api/v1/accounts/{accountNumber}/status - should update status")
    void shouldUpdateAccountStatus() throws Exception {
        AccountResponseDTO response = buildAccountResponse("ACC001");
        response.setStatus(AccountStatus.SUSPENDED);

        when(accountService.updateAccountStatus("ACC001", AccountStatus.SUSPENDED)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/accounts/ACC001/status")
                        .param("status", "SUSPENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNumber}/balance - should return balance")
    void shouldReturnAccountBalance() throws Exception {
        BalanceResponseDTO balance = new BalanceResponseDTO();
        balance.setAccountNumber("ACC001");
        balance.setBalance(BigDecimal.valueOf(5000));
        balance.setCurrency("USD");

        when(accountService.getBalance("ACC001")).thenReturn(balance);

        mockMvc.perform(get("/api/v1/accounts/ACC001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("ACC001"))
                .andExpect(jsonPath("$.balance").value(5000))
                .andExpect(jsonPath("$.currency").value("USD"));
    }
}
