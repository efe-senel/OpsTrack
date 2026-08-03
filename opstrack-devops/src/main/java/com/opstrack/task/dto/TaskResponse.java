package com.opstrack.task.dto;

import com.opstrack.task.entity.Task;
import com.opstrack.task.entity.TaskStatus;

import java.time.OffsetDateTime;

public record TaskResponse(
		Long id,
		String title,
		String description,
		TaskStatus status,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
	public static TaskResponse from(Task task) {
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
