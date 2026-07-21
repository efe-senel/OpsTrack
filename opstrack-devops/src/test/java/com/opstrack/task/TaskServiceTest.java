package com.opstrack.task;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceTest {

	private final TaskRepository taskRepository = mock(TaskRepository.class);
	private final TaskService taskService = new TaskService(taskRepository);

	@Test
	void createDefaultsStatusToOpenWhenMissing() {
		Task savedTask = new Task("Write tests", "Cover service logic", null);
		when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

		TaskResponse response = taskService.create(new TaskRequest("Write tests", "Cover service logic", null));

		assertThat(response.status()).isEqualTo(TaskStatus.OPEN);
		verify(taskRepository).save(any(Task.class));
	}

	@Test
	void findByIdThrowsWhenTaskDoesNotExist() {
		when(taskRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> taskService.findById(99L))
				.isInstanceOf(TaskNotFoundException.class)
				.hasMessage("Task not found with id: 99");
	}
}
