package com.opstrack.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "OpsTrack API",
				version = "v1",
				description = "Task tracking REST API"
		)
)
public class OpenApiConfiguration {
}
