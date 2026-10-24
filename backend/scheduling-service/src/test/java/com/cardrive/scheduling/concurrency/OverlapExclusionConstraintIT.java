package com.cardrive.scheduling.concurrency;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves - against a real PostgreSQL instance, not H2 - that the partial
 * EXCLUDE USING gist constraint from V2 is the thing that actually stops two
 * concurrent, overlapping test-drive bookings for the same car, even when the
 * classic "check then insert" race is deliberately triggered by two threads
 * hitting the database at (as close as possible to) the same instant.
 *
 * This bypasses the Spring context entirely and talks straight JDBC against
 * a Testcontainers Postgres, applying the real Flyway migrations first, so it
 * is exercising exactly the schema that ships to production.
 */
@Testcontainers
@EnabledIf("dockerIsAvailable")
class OverlapExclusionConstraintIT {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("scheduling_db")
                    .withUsername("test")
                    .withPassword("test");

    @BeforeAll
    static void migrate() {
        POSTGRES.start();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @AfterAll
    static void tearDown() {
        POSTGRES.stop();
    }

    @Test
    void concurrentOverlappingInsertsForSameCar_onlyOneSucceeds() throws Exception {
        long carId = 100L;
        seedCarAndSeller(carId);

        int attempts = 10;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        java.util.List<Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            futures.add(pool.submit(() -> {
                try (Connection conn = openConnection()) {
                    startGate.await();
                    try (Statement st = conn.createStatement()) {
                        // All threads try to book the exact same overlapping window.
                        st.executeUpdate("""
                            INSERT INTO test_drive (car_id, seller_username, buyer_username, start_time, end_time, status)
                            VALUES (%d, 'seller.jane', 'buyer.joe', '2026-08-25 14:30:00', '2026-08-25 15:15:00', 'PENDING')
                            """.formatted(carId));
                        succeeded.incrementAndGet();
                    }
                } catch (SQLException e) {
                    // 23P01 = exclusion_violation
                    assertEquals("23P01", e.getSQLState(), "Unexpected SQL error: " + e.getMessage());
                    rejected.incrementAndGet();
                } catch (InterruptedException ignored) {
                }
            }));
        }

        startGate.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, succeeded.get(), "Exactly one overlapping booking should have been accepted");
        assertEquals(attempts - 1, rejected.get(), "All other overlapping attempts must be rejected by the database");

        // Sanity: a non-overlapping booking for the same car on the same day must still succeed.
        try (Connection conn = openConnection(); Statement st = conn.createStatement()) {
            int updated = st.executeUpdate("""
                INSERT INTO test_drive (car_id, seller_username, buyer_username, start_time, end_time, status)
                VALUES (%d, 'seller.jane', 'buyer.joe', '2026-08-25 16:00:00', '2026-08-25 16:45:00', 'PENDING')
                """.formatted(carId));
            assertTrue(updated == 1);
        }
    }

    private static void seedCarAndSeller(long carId) throws SQLException {
        try (Connection conn = openConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT INTO car_schedule_config (car_id, seller_username, duration_minutes) VALUES (%d, 'seller.jane', 45)"
                    .formatted(carId));
        }
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    static boolean dockerIsAvailable() {
        try {
            return org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }
}
