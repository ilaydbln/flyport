package com.flightrez.controller;

import com.flightrez.dto.CreateReservationRequest;
import com.flightrez.dto.LockSeatRequest;
import com.flightrez.dto.LoginRequest;
import com.flightrez.dto.RegisterRequest;
import com.flightrez.dto.SeatLockResponse;
import com.flightrez.entity.Flight;
import com.flightrez.entity.Reservation;
import com.flightrez.entity.UserAccount;
import com.flightrez.service.DataStore;
import com.flightrez.service.ReservationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class FlightController {
    private final ReservationService reservationService;
    private final DataStore dataStore;

    public FlightController(ReservationService reservationService, DataStore dataStore) {
        this.reservationService = reservationService;
        this.dataStore = dataStore;
    }

    @GetMapping("/users")
    public Object users() {
        return dataStore.users();
    }

    @PostMapping("/auth/register")
    public UserAccount register(@RequestBody RegisterRequest request) {
        if (request.password() == null || !request.password().equals(request.passwordConfirm())) {
            throw new IllegalArgumentException("Şifreler eşleşmiyor.");
        }
        return dataStore.registerUser(request.fullName(), request.email(), request.password());
    }

    @PostMapping("/auth/login")
    public UserAccount login(@RequestBody LoginRequest request) {
        return dataStore.loginUser(request.email(), request.password());
    }

    @GetMapping("/flights")
    public List<Flight> flights(@RequestParam(required = false) String from,
                                @RequestParam(required = false) String to,
                                @RequestParam(required = false) String fromAirportCode,
                                @RequestParam(required = false) String toAirportCode,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate flightDate) {
        return reservationService.searchFlights(from, to, fromAirportCode, toAirportCode,
                flightDate != null ? flightDate : date);
    }

    @GetMapping("/flights/{flightId}")
    public Flight flight(@PathVariable long flightId) {
        return reservationService.getFlight(flightId);
    }

    @PostMapping("/seats/{seatId}/lock")
    public SeatLockResponse lockSeat(@PathVariable long seatId, @RequestBody LockSeatRequest request) {
        return reservationService.lockSeat(seatId, request.userId());
    }

    @PostMapping("/reservations")
    public Reservation reserve(@RequestBody CreateReservationRequest request) {
        return reservationService.createReservation(request);
    }

    @GetMapping("/reservations")
    public List<Reservation> reservations(@RequestParam long userId) {
        return reservationService.reservationsForUser(userId);
    }

    @GetMapping("/admin/reservations")
    public List<Reservation> adminReservations(@RequestParam long userId) {
        return reservationService.allReservations(userId);
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    public Reservation cancel(@PathVariable long reservationId) {
        return reservationService.cancelReservation(reservationId);
    }

    @PostMapping("/reservations/{reservationId}/refund")
    public Reservation refund(@PathVariable long reservationId) {
        return reservationService.refundReservation(reservationId);
    }
}
