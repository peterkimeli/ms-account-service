package com.fintech.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponseDTO {

    private String accountNumber;
    private BigDecimal balance;
    private String currency;
}