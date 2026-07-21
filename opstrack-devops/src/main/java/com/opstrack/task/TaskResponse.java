package com.opstrack.task;

import java.time.OffsetDateTime;

public record TaskResponse(
		Long id,
		String title,
		String description,
		TaskStatus status,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
	static TaskResponse from(Task task) {
		return new TaskResponse(
				task.getId(),
				task.getTitle(),
				task.getDescription(),
				task.getStatus(),
				task.getCreatedAt(),
				task.getUpdatedAt()
		);
	}
}
