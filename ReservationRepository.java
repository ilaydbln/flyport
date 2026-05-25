package com.flightrez.repository;

import com.flightrez.entity.Reservation;
import com.flightrez.service.DataStore;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public class ReservationRepository {
    private final DataStore dataStore;

    public ReservationRepository(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public Collection<Reservation> findAll() {
        return dataStore.reservations();
    }

    public Optional<Reservation> findById(long id) {
        return dataStore.findReservation(id);
    }

    public Reservation save(Reservation reservation) {
        return dataStore.saveReservation(reservation);
    }

    public long nextId() {
        return dataStore.nextReservationId();
    }
}
