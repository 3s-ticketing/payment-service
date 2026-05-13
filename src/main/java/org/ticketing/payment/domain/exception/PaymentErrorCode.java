package org.ticketing.payment.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode {
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Payment not found"),
    DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "이미 진행 중인 결제가 존재합니다"),
    INVALID_RESERVATION_STATE(HttpStatus.CONFLICT, "결제 불가한 예약 상태"),
    USER_MISMATCH(HttpStatus.BAD_REQUEST, "사용자 ID 불일치"),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "상태 변경이 불가합니다"),
    ALREADY_TERMINATED(HttpStatus.CONFLICT, "이미 종료된 결제입니다"),
    AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액 불일치"),
    TOSS_CONFIRM_FAILED(HttpStatus.BAD_GATEWAY, "Toss 결제 승인 실패"),
    TOSS_CANCEL_FAILED(HttpStatus.BAD_GATEWAY, "Toss 결제 취소 실패");

    private final HttpStatus status;
    private final String message;
}
