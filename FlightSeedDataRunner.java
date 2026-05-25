package com.flightrez.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class FlightSeedDataRunner implements CommandLineRunner {
    private final DataStore dataStore;

    public FlightSeedDataRunner(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void run(String... args) {
        dataStore.seedFlights();
    }
}
