package com.opstrack;

import org.springframework.boot.SpringApplication;

public class TestOpsTrackApplication {

	public static void main(String[] args) {
		SpringApplication.from(OpsTrackApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
