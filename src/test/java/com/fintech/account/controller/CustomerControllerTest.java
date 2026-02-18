package com.fintech.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.account.dto.CustomerRequestDTO;
import com.fintech.account.dto.CustomerResponseDTO;
import com.fintech.account.exception.BusinessRuleException;
import com.fintech.account.exception.CustomerNotFoundException;
import com.fintech.account.model.KycStatus;
import com.fintech.account.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @Test
    @DisplayName("POST /api/v1/customers - should create customer")
    void shouldCreateCustomer() throws Exception {
        CustomerRequestDTO request = new CustomerRequestDTO();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setPhone("+254712345678");
        request.setKycStatus(KycStatus.VERIFIED);

        CustomerResponseDTO response = new CustomerResponseDTO();
        response.setId(1L);
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setEmail("john@example.com");
        response.setPhone("+254712345678");
        response.setKycStatus(KycStatus.VERIFIED);
        response.setCreatedAt(LocalDateTime.now());

        when(customerService.createCustomer(any(CustomerRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/customers - should return 400 for invalid request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        CustomerRequestDTO request = new CustomerRequestDTO();
        // missing required fields

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} - should return customer")
    void shouldReturnCustomer() throws Exception {
        CustomerResponseDTO response = new CustomerResponseDTO();
        response.setId(1L);
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setEmail("john@example.com");
        response.setPhone("+254712345678");
        response.setKycStatus(KycStatus.VERIFIED);
        response.setCreatedAt(LocalDateTime.now());

        when(customerService.getCustomerById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} - should return 404 when not found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        when(customerService.getCustomerById(999L))
                .thenThrow(new CustomerNotFoundException("Customer not found with ID: 999"));

        mockMvc.perform(get("/api/v1/customers/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/customers/{id} - should update customer")
    void shouldUpdateCustomer() throws Exception {
        CustomerRequestDTO request = new CustomerRequestDTO();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setEmail("jane@example.com");
        request.setPhone("+254712345678");
        request.setKycStatus(KycStatus.VERIFIED);

        CustomerResponseDTO response = new CustomerResponseDTO();
        response.setId(1L);
        response.setFirstName("Jane");
        response.setLastName("Doe");
        response.setEmail("jane@example.com");
        response.setPhone("+254712345678");
        response.setKycStatus(KycStatus.VERIFIED);
        response.setCreatedAt(LocalDateTime.now());

        when(customerService.updateCustomer(eq(1L), any(CustomerRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/customers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    @DisplayName("POST /api/v1/customers - should return 409 for duplicate email")
    void shouldReturn409ForDuplicateEmail() throws Exception {
        CustomerRequestDTO request = new CustomerRequestDTO();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("existing@example.com");
        request.setPhone("+254712345678");
        request.setKycStatus(KycStatus.VERIFIED);

        when(customerService.createCustomer(any(CustomerRequestDTO.class)))
                .thenThrow(new BusinessRuleException("Customer already exists with email: existing@example.com"));

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
