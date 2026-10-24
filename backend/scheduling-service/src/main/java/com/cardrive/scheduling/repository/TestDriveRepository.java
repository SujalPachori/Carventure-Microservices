package com.cardrive.scheduling.repository;

import com.cardrive.scheduling.domain.entity.TestDrive;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TestDriveRepository extends JpaRepository<TestDrive, Long> {

    /**
     * Row-level pessimistic lock used for all status-transition operations
     * (confirm / reject / cancel / reschedule) so that two concurrent requests
     * against the SAME test-drive id serialize instead of racing on a stale
     * in-memory status read. This is on top of - not instead of - the DB
     * exclusion constraint, which protects across DIFFERENT test-drive rows.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TestDrive t where t.id = :id")
    Optional<TestDrive> findByIdForUpdate(@Param("id") Long id);

    /**
     * Fast-path application-level overlap pre-check (UX-friendly error before we
     * even attempt the insert). NOT the source of truth for correctness - see the
     * V2 Flyway migration's EXCLUDE constraint for that.
     */
    @Query("""
            select t from TestDrive t
            where t.carId = :carId
              and t.status in ('PENDING', 'CONFIRMED')
              and t.startTime < :endTime
              and t.endTime > :startTime
            """)
    List<TestDrive> findOverlapping(@Param("carId") Long carId,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * Same overlap query, excluding the booking being rescheduled itself.
     */
    @Query("""
            select t from TestDrive t
            where t.carId = :carId
              and t.id <> :excludeId
              and t.status in ('PENDING', 'CONFIRMED')
              and t.startTime < :endTime
              and t.endTime > :startTime
            """)
    List<TestDrive> findOverlappingExcludingSelf(@Param("carId") Long carId,
                                                  @Param("excludeId") Long excludeId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    List<TestDrive> findByBuyerUsernameOrderByStartTimeDesc(String buyerUsername);

    List<TestDrive> findBySellerUsernameOrderByStartTimeDesc(String sellerUsername);
}
