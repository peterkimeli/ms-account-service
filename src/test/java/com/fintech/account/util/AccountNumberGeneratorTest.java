package com.fintech.account.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountNumberGeneratorTest {

    @Test
    @DisplayName("should generate account number starting with ACC")
    void shouldStartWithPrefix() {
        String accountNumber = AccountNumberGenerator.generate();
        assertThat(accountNumber).startsWith("ACC");
    }

    @Test
    @DisplayName("should generate account number with expected length")
    void shouldHaveExpectedLength() {
        String accountNumber = AccountNumberGenerator.generate();
        // ACC (3) + 12-char trimmed timestamp + 4-digit random = 19
        assertThat(accountNumber).hasSize(19);
    }

    @RepeatedTest(10)
    @DisplayName("should generate unique account numbers")
    void shouldGenerateUniqueNumbers() {
        String first = AccountNumberGenerator.generate();
        String second = AccountNumberGenerator.generate();
        // While not guaranteed, consecutive generations should differ
        // (randomness + timestamp resolution makes collision extremely unlikely)
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("should contain only alphanumeric characters")
    void shouldContainOnlyAlphanumericCharacters() {
        String accountNumber = AccountNumberGenerator.generate();
        assertThat(accountNumber).matches("[A-Z0-9]+");
    }
}
