package com.flightrez.entity;

import com.flightrez.enums.SeatStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class Seat {
    private final long id;
    private final long flightId;
    private final String seatNumber;
    private final String cabinClass;
    private final BigDecimal price;
    private SeatStatus status;
    private Long lockedByUserId;
    private Instant lockedUntil;

    public Seat(long id, long flightId, String seatNumber, String cabinClass, BigDecimal price) {
        this.id = id;
        this.flightId = flightId;
        this.seatNumber = seatNumber;
        this.cabinClass = cabinClass;
        this.price = price;
        this.status = SeatStatus.AVAILABLE;
    }

    public long getId() {
        return id;
    }

    public long getFlightId() {
        return flightId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getCabinClass() {
        return cabinClass;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public Long getLockedByUserId() {
        return lockedByUserId;
    }

    public void setLockedByUserId(Long lockedByUserId) {
        this.lockedByUserId = lockedByUserId;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
}
