package com.fintech.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.account.dto.TransactionRequestDTO;
import com.fintech.account.dto.TransactionResponseDTO;
import com.fintech.account.exception.AccountNotFoundException;
import com.fintech.account.exception.BusinessRuleException;
import com.fintech.account.exception.InsufficientBalanceException;
import com.fintech.account.model.TransactionStatus;
import com.fintech.account.model.TransactionType;
import com.fintech.account.service.TransactionService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    private TransactionResponseDTO buildTransactionResponse(TransactionType type) {
        TransactionResponseDTO response = new TransactionResponseDTO();
        response.setId(1L);
        response.setAccountNumber("ACC001");
        response.setTransactionType(type);
        response.setAmount(BigDecimal.valueOf(500));
        response.setBalanceBefore(BigDecimal.valueOf(1000));
        response.setBalanceAfter(type == TransactionType.CREDIT
                ? BigDecimal.valueOf(1500)
                : BigDecimal.valueOf(500));
        response.setDescription("Test transaction");
        response.setStatus(TransactionStatus.COMPLETED);
        response.setTransactionDate(LocalDateTime.now());
        return response;
    }

    @Test
    @DisplayName("POST /api/v1/transactions/credit - should credit account")
    void shouldCreditAccount() throws Exception {
        TransactionRequestDTO request = new TransactionRequestDTO();
        request.setAccountNumber("ACC001");
        request.setAmount(BigDecimal.valueOf(500));
        request.setDescription("Deposit");

        TransactionResponseDTO response = buildTransactionResponse(TransactionType.CREDIT);

        when(transactionService.creditAccount(any(TransactionRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/transactions/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("CREDIT"))
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/transactions/credit - should return 400 for missing fields")
    void shouldReturn400ForInvalidCreditRequest() throws Exception {
        TransactionRequestDTO request = new TransactionRequestDTO();
        // missing accountNumber and amount

        mockMvc.perform(post("/api/v1/transactions/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/transactions/credit - should return 404 when account not found")
    void shouldReturn404WhenAccountNotFoundOnCredit() throws Exception {
        TransactionRequestDTO request = new TransactionRequestDTO();
        request.setAccountNumber("INVALID");
        request.setAmount(BigDecimal.valueOf(100));

        when(transactionService.creditAccount(any(TransactionRequestDTO.class)))
                .thenThrow(new AccountNotFoundException("Account not found: INVALID"));

        mockMvc.perform(post("/api/v1/transactions/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/transactions/debit - should debit account")
    void shouldDebitAccount() throws Exception {
        TransactionRequestDTO request = new TransactionRequestDTO();
        request.setAccountNumber("ACC001");
        request.setAmount(BigDecimal.valueOf(200));
        request.setDescription("Withdrawal");

        TransactionResponseDTO response = buildTransactionResponse(TransactionType.DEBIT);

        when(transactionService.debitAccount(any(TransactionRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/transactions/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionType").value("DEBIT"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/transactions/debit - should return 400 for insufficient balance")
    void shouldReturn400ForInsufficientBalance() throws Exception {
        TransactionRequestDTO request = new TransactionRequestDTO();
        request.setAccountNumber("ACC001");
        request.setAmount(BigDecimal.valueOf(99999));

        when(transactionService.debitAccount(any(TransactionRequestDTO.class)))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));

        mockMvc.perform(post("/api/v1/transactions/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/transactions/debit - should return 409 for inactive account")
    void shouldReturn409ForInactiveAccount() throws Exception {
        TransactionRequestDTO request = new TransactionRequestDTO();
        request.setAccountNumber("ACC001");
        request.setAmount(BigDecimal.valueOf(100));

        when(transactionService.debitAccount(any(TransactionRequestDTO.class)))
                .thenThrow(new BusinessRuleException("Account is not ACTIVE"));

        mockMvc.perform(post("/api/v1/transactions/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{accountNumber} - should return transaction history")
    void shouldReturnTransactionHistory() throws Exception {
        TransactionResponseDTO tx1 = buildTransactionResponse(TransactionType.CREDIT);
        TransactionResponseDTO tx2 = buildTransactionResponse(TransactionType.DEBIT);
        tx2.setId(2L);

        when(transactionService.getTransactionHistory("ACC001")).thenReturn(List.of(tx1, tx2));

        mockMvc.perform(get("/api/v1/transactions/ACC001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].transactionType").value("CREDIT"))
                .andExpect(jsonPath("$[1].transactionType").value("DEBIT"));
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{accountNumber} - should return empty list")
    void shouldReturnEmptyHistoryForNewAccount() throws Exception {
        when(transactionService.getTransactionHistory("ACC002")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/transactions/ACC002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/transactions/details/{transactionId} - should return transaction")
    void shouldReturnTransactionById() throws Exception {
        TransactionResponseDTO response = buildTransactionResponse(TransactionType.CREDIT);
        when(transactionService.getTransactionById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/transactions/details/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountNumber").value("ACC001"));
    }

    @Test
    @DisplayName("GET /api/v1/transactions/details/{transactionId} - should return 404 when not found")
    void shouldReturn404WhenTransactionNotFound() throws Exception {
        when(transactionService.getTransactionById(999L))
                .thenThrow(new AccountNotFoundException("Transaction not found with ID: 999"));

        mockMvc.perform(get("/api/v1/transactions/details/999"))
                .andExpect(status().isNotFound());
    }
}
