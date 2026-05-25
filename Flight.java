package com.flightrez.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Flight {
    private final long id;
    private final String airline;
    private final String flightCode;
    private final String departureAirportName;
    private final String departureAirportCode;
    private final String arrivalAirportName;
    private final String arrivalAirportCode;
    private final String departureCity;
    private final String arrivalCity;
    private final LocalDateTime departureTime;
    private final LocalDateTime arrivalTime;
    private final LocalDate flightDate;
    private final int durationMinutes;
    private final BigDecimal price;
    private final int availableSeats;
    private final String flightType;
    private final List<Seat> seats = new ArrayList<>();

    public Flight(long id, String flightNumber, String airline, String fromCity, String toCity,
                  LocalDateTime departureTime, LocalDateTime arrivalTime, BigDecimal basePrice) {
        this(id, airline, flightNumber, fromCity, fromCity, fromCity, toCity, toCity, toCity,
                departureTime, arrivalTime, basePrice, 36, "Yurt İçi");
    }

    public Flight(long id,
                  String airline,
                  String flightCode,
                  String departureAirportName,
                  String departureAirportCode,
                  String departureCity,
                  String arrivalAirportName,
                  String arrivalAirportCode,
                  String arrivalCity,
                  LocalDateTime departureTime,
                  LocalDateTime arrivalTime,
                  BigDecimal price,
                  int availableSeats,
                  String flightType) {
        this.id = id;
        this.airline = airline;
        this.flightCode = flightCode;
        this.departureAirportName = departureAirportName;
        this.departureAirportCode = departureAirportCode;
        this.departureCity = departureCity;
        this.arrivalAirportName = arrivalAirportName;
        this.arrivalAirportCode = arrivalAirportCode;
        this.arrivalCity = arrivalCity;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.flightDate = departureTime.toLocalDate();
        this.durationMinutes = (int) java.time.Duration.between(departureTime, arrivalTime).toMinutes();
        this.price = price;
        this.availableSeats = availableSeats;
        this.flightType = flightType;
    }

    public long getId() {
        return id;
    }

    public String getFlightNumber() {
        return flightCode;
    }

    public String getAirline() {
        return airline;
    }

    public String getFromCity() {
        return departureCity;
    }

    public String getToCity() {
        return arrivalCity;
    }

    public String getFlightCode() {
        return flightCode;
    }

    public String getDepartureAirportName() {
        return departureAirportName;
    }

    public String getDepartureAirportCode() {
        return departureAirportCode;
    }

    public String getArrivalAirportName() {
        return arrivalAirportName;
    }

    public String getArrivalAirportCode() {
        return arrivalAirportCode;
    }

    public String getDepartureCity() {
        return departureCity;
    }

    public String getArrivalCity() {
        return arrivalCity;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public BigDecimal getBasePrice() {
        return price;
    }

    public LocalDate getFlightDate() {
        return flightDate;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public String getFlightType() {
        return flightType;
    }

    public List<Seat> getSeats() {
        return Collections.unmodifiableList(seats);
    }

    public void addSeat(Seat seat) {
        seats.add(seat);
    }
}
