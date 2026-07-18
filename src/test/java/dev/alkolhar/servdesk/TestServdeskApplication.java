package dev.alkolhar.servdesk;

import org.springframework.boot.SpringApplication;

public class TestServdeskApplication {

	static void main(String[] args) {
		SpringApplication.from(ServdeskApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
