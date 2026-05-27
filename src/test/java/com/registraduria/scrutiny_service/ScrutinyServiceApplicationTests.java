package com.registraduria.scrutiny_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.flyway.enabled=false",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"server.port=0"
})
class ScrutinyServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
