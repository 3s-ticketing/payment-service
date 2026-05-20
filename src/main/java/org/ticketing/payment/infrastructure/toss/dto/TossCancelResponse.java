package org.ticketing.payment.infrastructure.toss.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TossCancelResponse {

    private String paymentKey;
    private String orderId;
    private String status;
    private Long totalAmount;
}
