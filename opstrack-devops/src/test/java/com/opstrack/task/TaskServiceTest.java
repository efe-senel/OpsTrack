package com.opstrack.task;

import com.opstrack.auth.entity.User;
import com.opstrack.auth.service.CurrentUserService;
import com.opstrack.task.dto.TaskRequest;
import com.opstrack.task.dto.TaskResponse;
import com.opstrack.task.entity.Task;
import com.opstrack.task.entity.TaskStatus;
import com.opstrack.task.exception.TaskNotFoundException;
import com.opstrack.task.repository.TaskRepository;
import com.opstrack.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskServiceTest {

	private final TaskRepository taskRepository = mock(TaskRepository.class);
	private final CurrentUserService currentUserService = mock(CurrentUserService.class);
	private final Authentication authentication = mock(Authentication.class);
	private final User owner = mock(User.class);
	private final TaskService taskService = new TaskService(taskRepository, currentUserService);

	@Test
	void createDefaultsStatusToOpenAndAssignsCurrentUser() {
		Task savedTask = new Task("Write tests", "Cover service logic", null, owner);
		when(currentUserService.require(authentication)).thenReturn(owner);
		when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

		TaskResponse response = taskService.create(
				new TaskRequest("Write tests", "Cover service logic", null),
				authentication
		);

		assertThat(response.status()).isEqualTo(TaskStatus.OPEN);
		verify(taskRepository).save(argThat(task -> task.getOwner() == owner));
	}

	@Test
	void findByIdThrowsWhenOwnedTaskDoesNotExist() {
		when(owner.getId()).thenReturn(7L);
		when(currentUserService.require(authentication)).thenReturn(owner);
		when(taskRepository.findByIdAndOwnerId(99L, 7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> taskService.findById(99L, authentication))
				.isInstanceOf(TaskNotFoundException.class)
				.hasMessage("Task not found with id: 99");
	}
}
