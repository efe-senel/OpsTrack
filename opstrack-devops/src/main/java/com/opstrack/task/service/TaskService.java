package com.opstrack.task.service;

import com.opstrack.auth.entity.User;
import com.opstrack.auth.service.CurrentUserService;
import com.opstrack.task.dto.TaskRequest;
import com.opstrack.task.dto.TaskResponse;
import com.opstrack.task.entity.Task;
import com.opstrack.task.exception.TaskNotFoundException;
import com.opstrack.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

	private final TaskRepository taskRepository;
	private final CurrentUserService currentUserService;

	@Transactional(readOnly = true)
	public List<TaskResponse> findAll(Authentication authentication) {
		User owner = currentUserService.require(authentication);

		return taskRepository.findAllByOwnerId(owner.getId())
				.stream()
				.map(TaskResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public TaskResponse findById(Long id, Authentication authentication) {
		User owner = currentUserService.require(authentication);
		return TaskResponse.from(getOwnedTask(id, owner.getId()));
	}

	@Transactional
	public TaskResponse create(TaskRequest request, Authentication authentication) {
		User owner = currentUserService.require(authentication);
		Task task = new Task(request.title(), request.description(), request.status(), owner);
		return TaskResponse.from(taskRepository.save(task));
	}

	@Transactional
	public TaskResponse update(Long id, TaskRequest request, Authentication authentication) {
		User owner = currentUserService.require(authentication);
		Task task = getOwnedTask(id, owner.getId());
		task.setTitle(request.title());
		task.setDescription(request.description());
		task.setStatus(request.status());
		return TaskResponse.from(task);
	}

	@Transactional
	public void delete(Long id, Authentication authentication) {
		User owner = currentUserService.require(authentication);
		Task task = getOwnedTask(id, owner.getId());
		taskRepository.delete(task);
	}

	private Task getOwnedTask(Long id, Long ownerId) {
		return taskRepository.findByIdAndOwnerId(id, ownerId)
				.orElseThrow(() -> new TaskNotFoundException(id));
	}
}
