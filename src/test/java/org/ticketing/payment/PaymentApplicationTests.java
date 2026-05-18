package org.ticketing.payment;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentApplicationTests {

	@Test
	@Disabled("CI/CD workflow 도입 단계에서 전체 context loading 테스트는 임시 비활성화")
	void contextLoads() {
	}

}