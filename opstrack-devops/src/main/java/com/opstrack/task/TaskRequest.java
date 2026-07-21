package com.opstrack.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskRequest(
		@NotBlank(message = "Title is required")
		@Size(max = 120, message = "Title must be at most 120 characters")
		String title,

		@Size(max = 1000, message = "Description must be at most 1000 characters")
		String description,

		TaskStatus status
) {
}
