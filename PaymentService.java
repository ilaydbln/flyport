package com.flightrez.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    public String charge(String cardNumber, BigDecimal amount) {
        String normalized = cardNumber == null ? "" : cardNumber.replaceAll("\\s", "");
        if (normalized.length() < 12 || normalized.endsWith("0000")) {
            throw new IllegalArgumentException("Odeme reddedildi. Test icin 0000 ile bitmeyen bir kart numarasi girin.");
        }
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public BigDecimal refund(BigDecimal paidAmount, int refundRate) {
        return paidAmount.multiply(BigDecimal.valueOf(refundRate)).divide(BigDecimal.valueOf(100));
    }
}
