package com.agrimarket.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"MONGODB_URI=mongodb://localhost:27017/testdb",
		"MONGODB_DATABASE=testdb"
})
class Ase251S4T11BeApplicationTests {

	@Test
	void contextLoads() {
	}

}
