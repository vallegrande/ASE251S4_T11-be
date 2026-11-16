package pe.edu.vallegrande.vgmsagrimarket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"MONGODB_URI=mongodb://localhost:27017/testdb",
		"MONGODB_DATABASE=testdb",
		"SQLSERVER_HOST=localhost",
		"SQLSERVER_PORT=1433",
		"SQLSERVER_USER=sa",
		"SQLSERVER_PASSWORD=test-password"
})
class Ase251S4T11BeApplicationTests {

	@Test
	void contextLoads() {
	}

}
