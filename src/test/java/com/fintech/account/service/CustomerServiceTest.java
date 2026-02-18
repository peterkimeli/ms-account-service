package com.fintech.account.service;

import com.fintech.account.dto.CustomerRequestDTO;
import com.fintech.account.dto.CustomerResponseDTO;
import com.fintech.account.exception.BusinessRuleException;
import com.fintech.account.exception.CustomerNotFoundException;
import com.fintech.account.model.Customer;
import com.fintech.account.model.KycStatus;
import com.fintech.account.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private CustomerRequestDTO validRequest;
    private Customer savedCustomer;

    @BeforeEach
    void setUp() {
        validRequest = new CustomerRequestDTO();
        validRequest.setFirstName("John");
        validRequest.setLastName("Doe");
        validRequest.setEmail("john.doe@example.com");
        validRequest.setPhone("+254712345678");
        validRequest.setKycStatus(KycStatus.VERIFIED);

        savedCustomer = new Customer();
        savedCustomer.setId(1L);
        savedCustomer.setFirstName("John");
        savedCustomer.setLastName("Doe");
        savedCustomer.setEmail("john.doe@example.com");
        savedCustomer.setPhone("+254712345678");
        savedCustomer.setKycStatus(KycStatus.VERIFIED);
        savedCustomer.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("createCustomer")
    class CreateCustomer {

        @Test
        @DisplayName("should create customer successfully")
        void shouldCreateCustomerSuccessfully() {
            when(customerRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
            when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

            CustomerResponseDTO response = customerService.createCustomer(validRequest);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getFirstName()).isEqualTo("John");
            assertThat(response.getLastName()).isEqualTo("Doe");
            assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
            assertThat(response.getKycStatus()).isEqualTo(KycStatus.VERIFIED);

            verify(customerRepository).existsByEmail("john.doe@example.com");
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            when(customerRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

            assertThatThrownBy(() -> customerService.createCustomer(validRequest))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Customer already exists with email");

            verify(customerRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCustomerById")
    class GetCustomerById {

        @Test
        @DisplayName("should return customer when found")
        void shouldReturnCustomerWhenFound() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(savedCustomer));

            CustomerResponseDTO response = customerService.getCustomerById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("should throw exception when customer not found")
        void shouldThrowExceptionWhenCustomerNotFound() {
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.getCustomerById(999L))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining("Customer not found with ID: 999");
        }
    }

    @Nested
    @DisplayName("updateCustomer")
    class UpdateCustomer {

        @Test
        @DisplayName("should update customer successfully")
        void shouldUpdateCustomerSuccessfully() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(savedCustomer));
            when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

            CustomerRequestDTO updateRequest = new CustomerRequestDTO();
            updateRequest.setFirstName("Jane");
            updateRequest.setLastName("Doe");
            updateRequest.setEmail("john.doe@example.com"); // same email
            updateRequest.setPhone("+254712345678");
            updateRequest.setKycStatus(KycStatus.VERIFIED);

            CustomerResponseDTO response = customerService.updateCustomer(1L, updateRequest);

            assertThat(response).isNotNull();
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("should throw exception when changing to existing email")
        void shouldThrowExceptionWhenChangingToExistingEmail() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(savedCustomer));
            when(customerRepository.existsByEmail("taken@example.com")).thenReturn(true);

            CustomerRequestDTO updateRequest = new CustomerRequestDTO();
            updateRequest.setFirstName("John");
            updateRequest.setLastName("Doe");
            updateRequest.setEmail("taken@example.com"); // different email that's taken
            updateRequest.setPhone("+254712345678");
            updateRequest.setKycStatus(KycStatus.VERIFIED);

            assertThatThrownBy(() -> customerService.updateCustomer(1L, updateRequest))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Email already in use");

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when customer not found")
        void shouldThrowExceptionWhenCustomerNotFound() {
            when(customerRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.updateCustomer(999L, validRequest))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }
}
