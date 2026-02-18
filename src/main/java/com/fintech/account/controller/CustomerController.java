package com.fintech.account.controller;

import com.fintech.account.dto.CustomerRequestDTO;
import com.fintech.account.dto.CustomerResponseDTO;
import com.fintech.account.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "APIs for managing customers")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Create a new customer")
    public ResponseEntity<CustomerResponseDTO> createCustomer(@Valid @RequestBody CustomerRequestDTO request) {
        CustomerResponseDTO response = customerService.createCustomer(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<CustomerResponseDTO> getCustomer(@PathVariable Long customerId) {
        CustomerResponseDTO response = customerService.getCustomerById(customerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "Update customer details")
    public ResponseEntity<CustomerResponseDTO> updateCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerRequestDTO request) {
        CustomerResponseDTO response = customerService.updateCustomer(customerId, request);
        return ResponseEntity.ok(response);
    }
}