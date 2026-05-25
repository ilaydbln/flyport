package com.flightrez.entity;

import com.flightrez.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class Reservation {
    private final long id;
    private final long flightId;
    private final long seatId;
    private final long userId;
    private final String passengerName;
    private final String passengerEmail;
    private final String phone;
    private final String cardLast4;
    private final String pnrCode;
    private final BigDecimal paidAmount;
    private final String paymentReference;
    private final Instant createdAt;
    private ReservationStatus status;
    private Instant cancelledAt;
    private BigDecimal refundAmount;

    public Reservation(long id, long flightId, long seatId, long userId, String passengerName,
                       String passengerEmail, String phone, String cardLast4, BigDecimal paidAmount, String paymentReference) {
        this.id = id;
        this.flightId = flightId;
        this.seatId = seatId;
        this.userId = userId;
        this.passengerName = passengerName;
        this.passengerEmail = passengerEmail;
        this.phone = phone;
        this.cardLast4 = cardLast4;
        this.pnrCode = "FR-" + id;
        this.paidAmount = paidAmount;
        this.paymentReference = paymentReference;
        this.createdAt = Instant.now();
        this.status = ReservationStatus.ACTIVE;
        this.refundAmount = BigDecimal.ZERO;
    }

    public long getId() {
        return id;
    }

    public long getFlightId() {
        return flightId;
    }

    public long getSeatId() {
        return seatId;
    }

    public long getUserId() {
        return userId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public String getPassengerEmail() {
        return passengerEmail;
    }

    public String getPhone() {
        return phone;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public String getPnrCode() {
        return pnrCode;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }
}
