-- ============================================================================
-- V2: Database-level, race-condition-proof protection against overlapping
--     active test drives for the same car.
--
-- This is the FINAL authority for scheduling correctness. Application-level
-- "check then insert" logic (see TestDriveService) is only a fast-path/UX
-- optimization; it cannot, by itself, prevent two concurrent transactions
-- (potentially on different service instances) from both passing the check
-- and then both committing an overlapping booking. A plain UNIQUE constraint
-- cannot express "no overlapping time ranges" at all - only an exclusion
-- constraint over a range type can.
--
-- The constraint is PARTIAL: it only applies to rows whose status is PENDING
-- or CONFIRMED (the "active" scheduling states). REJECTED / CANCELLED /
-- COMPLETED rows are historical and must never block a new booking for the
-- same time slot.
-- ============================================================================

-- Required for GiST equality support on a plain scalar (car_id) combined with
-- a range-overlap operator in the same exclusion constraint.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE test_drive
    ADD CONSTRAINT excl_test_drive_no_overlap
    EXCLUDE USING gist (
        car_id WITH =,
        tsrange(start_time, end_time, '[)') WITH &&
    )
    WHERE (status IN ('PENDING', 'CONFIRMED'));
