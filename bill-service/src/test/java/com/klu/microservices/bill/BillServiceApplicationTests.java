package com.klu.microservices.bill;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
    "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:postgresql://localhost:5432/precisionauto_billing"
})
class BillServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
