package com.flightrez.service;

import com.flightrez.dto.CreateReservationRequest;
import com.flightrez.dto.SeatLockResponse;
import com.flightrez.entity.Flight;
import com.flightrez.entity.Reservation;
import com.flightrez.enums.ReservationStatus;
import com.flightrez.entity.Seat;
import com.flightrez.enums.SeatStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ReservationService {
    private static final Duration LOCK_DURATION = Duration.ofMinutes(8);

    private final Object bookingLock = new Object();
    private final DataStore dataStore;
    private final PaymentService paymentService;

    public ReservationService(DataStore dataStore, PaymentService paymentService) {
        this.dataStore = dataStore;
        this.paymentService = paymentService;
    }

    public List<Flight> searchFlights(String from, String to, String fromAirportCode, String toAirportCode, LocalDate date) {
        releaseExpiredLocks();
        return dataStore.flights().stream()
                .filter(flight -> matchesAny(from, flight.getFromCity(), flight.getDepartureAirportCode(), flight.getDepartureAirportName()))
                .filter(flight -> matchesAny(to, flight.getToCity(), flight.getArrivalAirportCode(), flight.getArrivalAirportName()))
                .filter(flight -> matchesCode(flight.getDepartureAirportCode(), fromAirportCode))
                .filter(flight -> matchesCode(flight.getArrivalAirportCode(), toAirportCode))
                .filter(flight -> date == null || flight.getFlightDate().equals(date))
                .sorted(Comparator.comparing(Flight::getDepartureTime))
                .toList();
    }

    public Flight getFlight(long flightId) {
        releaseExpiredLocks();
        return dataStore.findFlight(flightId).orElseThrow(() -> new IllegalArgumentException("Ucus bulunamadi."));
    }

    public SeatLockResponse lockSeat(long seatId, long userId) {
        synchronized (bookingLock) {
            releaseExpiredLocks();
            dataStore.findUser(userId).orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi."));
            Seat seat = findSeat(seatId);
            if (seat.getStatus() == SeatStatus.RESERVED) {
                throw new IllegalStateException("Bu koltuk daha once rezerve edilmis.");
            }
            if (seat.getStatus() == SeatStatus.LOCKED && !Long.valueOf(userId).equals(seat.getLockedByUserId())) {
                throw new IllegalStateException("Bu koltuk gecici olarak baska kullanici tarafindan tutuluyor.");
            }
            Instant lockedUntil = Instant.now().plus(LOCK_DURATION);
            seat.setStatus(SeatStatus.LOCKED);
            seat.setLockedByUserId(userId);
            seat.setLockedUntil(lockedUntil);
            return new SeatLockResponse(seatId, seat.getStatus().name(), lockedUntil);
        }
    }

    public Reservation createReservation(CreateReservationRequest request) {
        synchronized (bookingLock) {
            releaseExpiredLocks();
            dataStore.findUser(request.userId()).orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi."));
            validatePassengerAndPayment(request);
            Flight flight = getFlight(request.flightId());
            Seat seat = findSeat(request.seatId());
            if (seat.getFlightId() != flight.getId()) {
                throw new IllegalArgumentException("Secilen koltuk bu ucusa ait degil.");
            }
            if (seat.getStatus() == SeatStatus.RESERVED) {
                throw new IllegalStateException("Bu koltuk icin aktif rezervasyon var.");
            }
            if (seat.getStatus() == SeatStatus.LOCKED && !Long.valueOf(request.userId()).equals(seat.getLockedByUserId())) {
                throw new IllegalStateException("Koltuk kilidi baska kullaniciya ait.");
            }
            String paymentReference = paymentService.charge(request.cardNumber(), seat.getPrice());
            Reservation reservation = new Reservation(
                    dataStore.nextReservationId(),
                    flight.getId(),
                    seat.getId(),
                    request.userId(),
                    request.passengerName(),
                    request.passengerEmail(),
                    request.phone(),
                    cardLast4(request.cardNumber()),
                    seat.getPrice(),
                    paymentReference
            );
            seat.setStatus(SeatStatus.RESERVED);
            seat.setLockedByUserId(null);
            seat.setLockedUntil(null);
            return dataStore.saveReservation(reservation);
        }
    }

    private void validatePassengerAndPayment(CreateReservationRequest request) {
        if (isBlank(request.passengerName()) || isBlank(request.passengerEmail()) || isBlank(request.phone())) {
            throw new IllegalArgumentException("Yolcu adı, e-posta ve telefon zorunludur.");
        }
        if (isBlank(request.cardNumber()) || isBlank(request.cardExpiry()) || isBlank(request.cardCvv())) {
            throw new IllegalArgumentException("Kart numarası, son kullanma tarihi ve CVV zorunludur.");
        }
        String cvv = request.cardCvv().trim();
        if (!cvv.matches("\\d{3,4}")) {
            throw new IllegalArgumentException("CVV 3 veya 4 haneli olmalıdır.");
        }
    }

    private String cardLast4(String cardNumber) {
        String normalized = cardNumber == null ? "" : cardNumber.replaceAll("\\D", "");
        return normalized.length() <= 4 ? normalized : normalized.substring(normalized.length() - 4);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public Reservation cancelReservation(long reservationId) {
        synchronized (bookingLock) {
            Reservation reservation = dataStore.findReservation(reservationId)
                    .orElseThrow(() -> new IllegalArgumentException("Rezervasyon bulunamadi."));
            if (reservation.getStatus() != ReservationStatus.ACTIVE) {
                throw new IllegalStateException("Bu rezervasyon zaten iptal/iade surecinde.");
            }
            Flight flight = getFlight(reservation.getFlightId());
            Seat seat = findSeat(reservation.getSeatId());
            int refundRate = refundRate(flight.getDepartureTime());
            BigDecimal refundAmount = paymentService.refund(reservation.getPaidAmount(), refundRate);
            reservation.setStatus(refundRate == 100 ? ReservationStatus.REFUNDED : ReservationStatus.CANCELLED);
            reservation.setCancelledAt(Instant.now());
            reservation.setRefundAmount(refundAmount);
            seat.setStatus(SeatStatus.AVAILABLE);
            return reservation;
        }
    }

    public Reservation refundReservation(long reservationId) {
        synchronized (bookingLock) {
            Reservation reservation = dataStore.findReservation(reservationId)
                    .orElseThrow(() -> new IllegalArgumentException("Rezervasyon bulunamadi."));
            if (reservation.getStatus() == ReservationStatus.REFUNDED) {
                return reservation;
            }
            if (reservation.getStatus() != ReservationStatus.ACTIVE) {
                throw new IllegalStateException("Bu rezervasyon aktif degil.");
            }
            Seat seat = findSeat(reservation.getSeatId());
            BigDecimal refundAmount = paymentService.refund(reservation.getPaidAmount(), 100);
            reservation.setStatus(ReservationStatus.REFUNDED);
            reservation.setCancelledAt(Instant.now());
            reservation.setRefundAmount(refundAmount);
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setLockedByUserId(null);
            seat.setLockedUntil(null);
            return reservation;
        }
    }

    public List<Reservation> reservationsForUser(long userId) {
        return dataStore.reservations().stream()
                .filter(reservation -> reservation.getUserId() == userId)
                .sorted(Comparator.comparing(Reservation::getCreatedAt).reversed())
                .toList();
    }

    public List<Reservation> allReservations(long userId) {
        var user = dataStore.findUser(userId).orElseThrow(() -> new IllegalArgumentException("Kullanici bulunamadi."));
        if (!"ADMIN".equals(user.getRole().name())) {
            throw new IllegalStateException("Bu islem admin rolu gerektirir.");
        }
        return dataStore.reservations().stream()
                .sorted(Comparator.comparing(Reservation::getCreatedAt).reversed())
                .toList();
    }

    private boolean matches(String value, String query) {
        return query == null || query.isBlank() || value.toLowerCase().contains(query.toLowerCase());
    }

    private boolean matchesAny(String query, String... values) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalizedQuery = normalize(query);
        for (String value : values) {
            if (normalize(value).contains(normalizedQuery)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCode(String value, String query) {
        return query == null || query.isBlank() || value.equalsIgnoreCase(query.trim());
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.forLanguageTag("tr-TR"));
    }

    private Seat findSeat(long seatId) {
        return dataStore.flights().stream()
                .flatMap(flight -> flight.getSeats().stream())
                .filter(seat -> seat.getId() == seatId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Koltuk bulunamadi."));
    }

    private void releaseExpiredLocks() {
        Instant now = Instant.now();
        dataStore.flights().stream()
                .flatMap(flight -> flight.getSeats().stream())
                .filter(seat -> seat.getStatus() == SeatStatus.LOCKED)
                .filter(seat -> seat.getLockedUntil() != null && seat.getLockedUntil().isBefore(now))
                .forEach(seat -> {
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setLockedByUserId(null);
                    seat.setLockedUntil(null);
                });
    }

    private int refundRate(LocalDateTime departureTime) {
        long hoursUntilDeparture = Duration.between(LocalDateTime.now(), departureTime).toHours();
        if (hoursUntilDeparture >= 72) {
            return 100;
        }
        if (hoursUntilDeparture >= 24) {
            return 90;
        }
        return 50;
    }
}
