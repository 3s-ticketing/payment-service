package org.ticketing.payment.infrastructure.toss;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.ticketing.payment.domain.exception.TossPaymentCancelException;
import org.ticketing.payment.domain.exception.TossPaymentConfirmException;
import org.ticketing.payment.infrastructure.toss.dto.TossCancelRequest;
import org.ticketing.payment.infrastructure.toss.dto.TossCancelResponse;
import org.ticketing.payment.infrastructure.toss.dto.TossConfirmRequest;
import org.ticketing.payment.infrastructure.toss.dto.TossConfirmResponse;
import org.ticketing.payment.infrastructure.toss.dto.TossPaymentStatusResponse;

@Component
@RequiredArgsConstructor
public class TossPaymentClient {

    private static final String TOSS_BASE_URL = "https://api.tosspayments.com";

    private final TossPaymentProperties properties;

    public TossConfirmResponse confirm(String paymentKey, String orderId, Long amount) {
        String encoded = Base64.getEncoder()
                .encodeToString((properties.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8));

        return RestClient.builder()
                .baseUrl(TOSS_BASE_URL)
                .build()
                .post()
                .uri("/v1/payments/confirm")
                .header("Authorization", "Basic " + encoded)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TossConfirmRequest(paymentKey, orderId, amount))
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        (request, response) -> {
                            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            throw new TossPaymentConfirmException(response.getStatusCode().value(), body);
                        }
                )
                .body(TossConfirmResponse.class);
    }

    public TossCancelResponse cancel(String paymentKey, String cancelReason) {
        String encoded = Base64.getEncoder()
                .encodeToString((properties.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8));

        return RestClient.builder()
                .baseUrl(TOSS_BASE_URL)
                .build()
                .post()
                .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                .header("Authorization", "Basic " + encoded)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TossCancelRequest(cancelReason))
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        (request, response) -> {
                            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            throw new TossPaymentCancelException(response.getStatusCode().value(), body);
                        }
                )
                .body(TossCancelResponse.class);
    }

    // TOSS에서의 orderId = Payment service의 paymentId
    public TossPaymentStatusResponse getByOrderId(String orderId) {
        String encoded = Base64.getEncoder()
                .encodeToString((properties.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8));

        return RestClient.builder()
                .baseUrl(TOSS_BASE_URL)
                .build()
                .get()
                .uri("/v1/payments/orders/{orderId}", orderId)
                .header("Authorization", "Basic " + encoded)
                .retrieve()
                .body(TossPaymentStatusResponse.class);
    }

    public TossPaymentStatusResponse getByPaymentKey(String paymentKey) {
        String encoded = Base64.getEncoder()
                .encodeToString((properties.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8));

        return RestClient.builder()
                .baseUrl(TOSS_BASE_URL)
                .build()
                .get()
                .uri("/v1/payments/{paymentKey}", paymentKey)
                .header("Authorization", "Basic " + encoded)
                .retrieve()
                .body(TossPaymentStatusResponse.class);
    }
}