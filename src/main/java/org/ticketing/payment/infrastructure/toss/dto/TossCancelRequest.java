package org.ticketing.payment.infrastructure.toss.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TossCancelRequest {

    private String cancelReason;
}