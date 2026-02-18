package com.fintech.account.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AccountNumberGenerator {

    private static final SecureRandom random = new SecureRandom();
    private static final String PREFIX = "ACC";

    public static String generate() {
        // Generate account number: ACC + timestamp + random digits
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomNum = 1000 + random.nextInt(9000); // 4-digit random number
        return PREFIX + timestamp.substring(2) + randomNum; // Using substring to shorten
    }
}