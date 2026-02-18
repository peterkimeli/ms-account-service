package com.fintech.account.service;

import com.fintech.account.dto.CustomerRequestDTO;
import com.fintech.account.dto.CustomerResponseDTO;
import com.fintech.account.exception.BusinessRuleException;
import com.fintech.account.exception.CustomerNotFoundException;
import com.fintech.account.model.Customer;
import com.fintech.account.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponseDTO createCustomer(CustomerRequestDTO request) {
        log.info("Creating customer: {}", request.getEmail());

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Customer already exists with email: " + request.getEmail());
        }

        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setKycStatus(request.getKycStatus());

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer created successfully with ID: {}", savedCustomer.getId());
        return mapToResponseDTO(savedCustomer);
    }

    @Transactional(readOnly = true)
    public CustomerResponseDTO getCustomerById(Long customerId) {
        log.info("Fetching customer with ID: {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));
        return mapToResponseDTO(customer);
    }

    @Transactional
    public CustomerResponseDTO updateCustomer(Long customerId, CustomerRequestDTO request) {
        log.info("Updating customer with ID: {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));

        // Check if email is being changed and if it's already taken
        if (!customer.getEmail().equals(request.getEmail()) && 
            customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email already in use: " + request.getEmail());
        }

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setKycStatus(request.getKycStatus());

        Customer updatedCustomer = customerRepository.save(customer);
        log.info("Customer updated successfully: {}", customerId);
        return mapToResponseDTO(updatedCustomer);
    }

    private CustomerResponseDTO mapToResponseDTO(Customer customer) {
        CustomerResponseDTO dto = new CustomerResponseDTO();
        dto.setId(customer.getId());
        dto.setFirstName(customer.getFirstName());
        dto.setLastName(customer.getLastName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setKycStatus(customer.getKycStatus());
        dto.setCreatedAt(customer.getCreatedAt());
        return dto;
    }
}