package toubson.backendspringboot;

import org.springframework.boot.SpringApplication;

public class TestBackendSpringbootApplication {

	public static void main(String[] args) {
		SpringApplication.from(BackendSpringbootApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
