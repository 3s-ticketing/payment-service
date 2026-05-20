package org.ticketing.payment.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.ticketing.payment.infrastructure.toss.TossPaymentProperties;

@Configuration
public class TossRestClientConfig {

    @Bean
    public RestClient tossRestClient(TossPaymentProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));

        String encoded = Base64.getEncoder()
                .encodeToString((props.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8));

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
                .build();
    }
}