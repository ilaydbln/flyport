package com.flightrez.service;

import com.flightrez.entity.Flight;
import com.flightrez.entity.Reservation;
import com.flightrez.enums.Role;
import com.flightrez.entity.Seat;
import com.flightrez.entity.UserAccount;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DataStore {
    private static final LocalDate SEED_START = LocalDate.of(2026, 5, 22);
    private static final LocalDate SEED_END = LocalDate.of(2027, 1, 26);

    private final Map<Long, Flight> flights = new ConcurrentHashMap<>();
    private final Map<Long, UserAccount> users = new ConcurrentHashMap<>();
    private final Map<Long, Reservation> reservations = new ConcurrentHashMap<>();
    private final AtomicLong reservationSequence = new AtomicLong(5000);
    private final AtomicLong flightSequence = new AtomicLong(1000);
    private final AtomicLong userSequence = new AtomicLong(2);

    public DataStore() {
        seedUsers();
    }

    public Collection<Flight> flights() {
        return flights.values();
    }

    public Collection<UserAccount> users() {
        return users.values();
    }

    public Collection<Reservation> reservations() {
        return reservations.values();
    }

    public Optional<Flight> findFlight(long id) {
        return Optional.ofNullable(flights.get(id));
    }

    public Optional<UserAccount> findUser(long id) {
        return Optional.ofNullable(users.get(id));
    }

    public Optional<UserAccount> findUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return users.values().stream()
                .filter(user -> normalizeEmail(user.getEmail()).equals(normalizedEmail))
                .findFirst();
    }

    public UserAccount registerUser(String fullName, String email, String password) {
        if (isBlank(fullName) || isBlank(email) || isBlank(password)) {
            throw new IllegalArgumentException("Ad soyad, e-posta ve şifre zorunludur.");
        }
        if (findUserByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Bu e-posta ile kayıtlı bir kullanıcı var.");
        }
        long id = userSequence.incrementAndGet();
        UserAccount user = new UserAccount(id, email.trim(), fullName.trim(), email.trim(), Role.USER, false, password);
        users.put(id, user);
        return user;
    }

    public UserAccount loginUser(String email, String password) {
        if (isBlank(email) || isBlank(password)) {
            throw new IllegalArgumentException("E-posta ve şifre zorunludur.");
        }
        UserAccount user = findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("E-posta veya şifre hatalı."));
        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("E-posta veya şifre hatalı.");
        }
        return user;
    }

    public Optional<Reservation> findReservation(long id) {
        return Optional.ofNullable(reservations.get(id));
    }

    public Reservation saveReservation(Reservation reservation) {
        reservations.put(reservation.getId(), reservation);
        return reservation;
    }

    public long nextReservationId() {
        return reservationSequence.incrementAndGet();
    }

    private void seedUsers() {
        users.put(1L, new UserAccount(1, "user", "Demo User", "user@flightrez.test", Role.USER, false, "123456"));
        users.put(2L, new UserAccount(2, "admin", "Admin", "admin@flightrez.test", Role.ADMIN, true, "123456"));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public void seedFlights() {
        if (!flights.isEmpty()) {
            return;
        }
        Random random = new Random(20260522L);
        List<RouteSeed> routes = List.of(
                new RouteSeed(airport("İzmir Adnan Menderes Havalimanı", "ADB", "İzmir"),
                        airport("Trabzon Havalimanı", "TZX", "Trabzon"),
                        List.of("SunExpress", "Pegasus"), 110, "Yurt İçi",
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                        LocalTime.of(8, 35)),
                new RouteSeed(airport("İstanbul Havalimanı", "IST", "İstanbul"),
                        airport("Antalya Havalimanı", "AYT", "Antalya"),
                        List.of("Turkish Airlines"), 80, "Yurt İçi",
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                        LocalTime.of(10, 10)),
                new RouteSeed(airport("İstanbul Havalimanı", "IST", "İstanbul"),
                        airport("Ankara Esenboğa Havalimanı", "ESB", "Ankara"),
                        List.of("Turkish Airlines"), 65, "Yurt İçi",
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
                        LocalTime.of(7, 45)),
                new RouteSeed(airport("İstanbul Sabiha Gökçen Havalimanı", "SAW", "İstanbul"),
                        airport("İzmir Adnan Menderes Havalimanı", "ADB", "İzmir"),
                        List.of("Pegasus", "AJet"), 65, "Yurt İçi",
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
                        LocalTime.of(9, 20)),
                new RouteSeed(airport("İstanbul Sabiha Gökçen Havalimanı", "SAW", "İstanbul"),
                        airport("Antalya Havalimanı", "AYT", "Antalya"),
                        List.of("Pegasus", "AJet"), 75, "Yurt İçi",
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                        LocalTime.of(12, 0)),
                new RouteSeed(airport("İzmir Adnan Menderes Havalimanı", "ADB", "İzmir"),
                        airport("Antalya Havalimanı", "AYT", "Antalya"),
                        List.of("SunExpress"), 70, "Yurt İçi",
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
                        LocalTime.of(15, 25)),
                new RouteSeed(airport("İstanbul Havalimanı", "IST", "İstanbul"),
                        airport("Milas-Bodrum Havalimanı", "BJV", "Bodrum"),
                        List.of("Turkish Airlines"), 75, "Yurt İçi",
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                        LocalTime.of(11, 15)),
                new RouteSeed(airport("İstanbul Havalimanı", "IST", "İstanbul"),
                        airport("Londra Heathrow Havalimanı", "LHR", "Londra"),
                        List.of("Turkish Airlines"), 250, "Yurt Dışı",
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
                        LocalTime.of(13, 35)),
                new RouteSeed(airport("İstanbul Sabiha Gökçen Havalimanı", "SAW", "İstanbul"),
                        airport("Berlin Brandenburg Havalimanı", "BER", "Berlin"),
                        List.of("Pegasus", "AJet"), 185, "Yurt Dışı",
                        Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                        LocalTime.of(14, 45)),
                new RouteSeed(airport("İstanbul Sabiha Gökçen Havalimanı", "SAW", "İstanbul"),
                        airport("Dubai Uluslararası Havalimanı", "DXB", "Dubai"),
                        List.of("Pegasus", "AJet"), 260, "Yurt Dışı",
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                        LocalTime.of(21, 30)),
                new RouteSeed(airport("İstanbul Havalimanı", "IST", "İstanbul"),
                        airport("Paris Charles de Gaulle Havalimanı", "CDG", "Paris"),
                        List.of("Turkish Airlines"), 220, "Yurt Dışı",
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
                        LocalTime.of(16, 40))
        );

        for (LocalDate date = SEED_START; !date.isAfter(SEED_END); date = date.plusDays(1)) {
            for (RouteSeed route : routes) {
                if (route.operatingDays().contains(date.getDayOfWeek())) {
                    int airlineIndex = Math.floorMod(date.toEpochDay() + route.departure().code().hashCode(), route.airlines().size());
                    String airline = route.airlines().get(airlineIndex);
                    addFlight(route, airline, date, random);
                }
            }
        }
    }

    private void addFlight(RouteSeed route, String airline, LocalDate date, Random random) {
        long id = flightSequence.incrementAndGet();
        int minuteJitter = random.nextInt(0, 4) * 5;
        LocalDateTime departure = LocalDateTime.of(date, route.baseDepartureTime().plusMinutes(minuteJitter));
        LocalDateTime arrival = departure.plusMinutes(route.durationMinutes());
        int availableSeats = random.nextInt(18, 91);
        BigDecimal price = BigDecimal.valueOf(randomPrice(route.flightType(), random));
        String flightCode = airlinePrefix(airline) + (100 + random.nextInt(900));

        Flight flight = new Flight(
                id,
                airline,
                flightCode,
                route.departure().name(),
                route.departure().code(),
                route.departure().city(),
                route.arrival().name(),
                route.arrival().code(),
                route.arrival().city(),
                departure,
                arrival,
                price,
                availableSeats,
                route.flightType()
        );
        addSeats(flight, availableSeats, price);
        flights.put(id, flight);
    }

    private void addSeats(Flight flight, int availableSeats, BigDecimal basePrice) {
        long seatId = flight.getId() * 1000;
        String[] letters = {"A", "B", "C", "D", "E", "F"};
        int rows = (int) Math.ceil(availableSeats / 6.0);
        int created = 0;
        for (int row = 1; row <= rows && created < availableSeats; row++) {
            for (String letter : letters) {
                if (created >= availableSeats) {
                    break;
                }
                String cabin = row <= 2 ? "Business" : "Economy";
                BigDecimal price = row <= 2 ? basePrice.multiply(new BigDecimal("1.8")) : basePrice;
                flight.addSeat(new Seat(++seatId, flight.getId(), row + letter, cabin, price));
                created++;
            }
        }
    }

    private int randomPrice(String flightType, Random random) {
        if ("Yurt Dışı".equals(flightType)) {
            return random.nextInt(4500, 18001);
        }
        return random.nextInt(1200, 3501);
    }

    private String airlinePrefix(String airline) {
        return switch (airline) {
            case "Turkish Airlines" -> "TK";
            case "Pegasus" -> "PC";
            case "AJet" -> "VF";
            case "SunExpress" -> "XQ";
            default -> "FP";
        };
    }

    private AirportSeed airport(String name, String code, String city) {
        return new AirportSeed(name, code, city);
    }

    private record AirportSeed(String name, String code, String city) {
    }

    private record RouteSeed(AirportSeed departure,
                             AirportSeed arrival,
                             List<String> airlines,
                             int durationMinutes,
                             String flightType,
                             Set<DayOfWeek> operatingDays,
                             LocalTime baseDepartureTime) {
    }
}
