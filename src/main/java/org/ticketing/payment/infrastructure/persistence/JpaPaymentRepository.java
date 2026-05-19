package org.ticketing.payment.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.ticketing.payment.application.dto.PaymentContext;
import org.ticketing.payment.domain.model.Payment;
import org.ticketing.payment.domain.model.PaymentStatus;

public interface JpaPaymentRepository extends JpaRepository<Payment, UUID> {

    // ── entity queries (read / write paths) ──────────────────────────────────

    Optional<Payment> findByIdAndDeletedAtIsNull(UUID id);
    Optional<Payment> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
    Page<Payment> findAllByDeletedAtIsNull(Pageable pageable);
    boolean existsByReservationIdAndStatusIn(UUID reservationId, List<PaymentStatus> statuses);
    Page<Payment> findByReservationIdAndDeletedAtIsNull(UUID reservationId, Pageable pageable);
    Page<Payment> findByReservationIdAndUserIdAndDeletedAtIsNull(UUID reservationId, UUID userId, Pageable pageable);
    Optional<Payment> findFirstByReservationIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(UUID reservationId, PaymentStatus status);
    Optional<Payment> findFirstByReservationIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID reservationId);
    Optional<Payment> findFirstByReservationIdAndUserIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(UUID reservationId, UUID userId, PaymentStatus status);
    Page<Payment> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    // ── CAS UPDATE: INIT → PAYING (status + totalPrice 동시 업데이트) ─────────

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Payment p SET p.status = :toStatus, p.totalPrice = :expectedAmount " +
           "WHERE p.id = :id AND p.status = :fromStatus")
    int casUpdateStatusWithAmount(@Param("id") UUID id,
                                  @Param("fromStatus") PaymentStatus fromStatus,
                                  @Param("toStatus") PaymentStatus toStatus,
                                  @Param("expectedAmount") Long expectedAmount);

    // ── CAS UPDATE: status + paymentKey (PAYING → SUCCESS) ───────────────────

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Payment p SET p.status = :toStatus, p.paymentKey = :paymentKey " +
           "WHERE p.id = :id AND p.status = :fromStatus")
    int casUpdateStatusWithKey(@Param("id") UUID id,
                               @Param("fromStatus") PaymentStatus fromStatus,
                               @Param("toStatus") PaymentStatus toStatus,
                               @Param("paymentKey") String paymentKey);

    // ── CAS UPDATE: status only (PAYING→FAIL, SUCCESS→REFUNDING, etc.) ───────

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Payment p SET p.status = :toStatus WHERE p.id = :id AND p.status = :fromStatus")
    int casUpdateStatus(@Param("id") UUID id,
                        @Param("fromStatus") PaymentStatus fromStatus,
                        @Param("toStatus") PaymentStatus toStatus);

    // ── projection queries (no entity loading) ────────────────────────────────

    @Query("SELECT new org.ticketing.payment.application.dto.PaymentContext(" +
           "p.id, p.userId, p.reservationId, p.totalPrice, p.paymentKey, p.status, p.modifiedAt) " +
           "FROM Payment p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<PaymentContext> findContextById(@Param("id") UUID id);

    @Query("SELECT new org.ticketing.payment.application.dto.PaymentContext(" +
           "p.id, p.userId, p.reservationId, p.totalPrice, p.paymentKey, p.status, p.modifiedAt) " +
           "FROM Payment p WHERE p.reservationId = :reservationId AND p.status = :status " +
           "AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<PaymentContext> findContextsByReservationIdAndStatus(@Param("reservationId") UUID reservationId,
                                                              @Param("status") PaymentStatus status);

    @Query("SELECT new org.ticketing.payment.application.dto.PaymentContext(" +
           "p.id, p.userId, p.reservationId, p.totalPrice, p.paymentKey, p.status, p.modifiedAt) " +
           "FROM Payment p WHERE p.status = :status AND p.modifiedAt < :before AND p.deletedAt IS NULL")
    List<PaymentContext> findStuckContexts(@Param("status") PaymentStatus status,
                                           @Param("before") LocalDateTime before);
}
