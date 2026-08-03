package com.opstrack.task.controller;

import com.opstrack.task.dto.TaskRequest;
import com.opstrack.task.dto.TaskResponse;
import com.opstrack.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@GetMapping
	public List<TaskResponse> findAll(Authentication authentication) {
		return taskService.findAll(authentication);
	}

	@GetMapping("/{id}")
	public TaskResponse findById(@PathVariable Long id, Authentication authentication) {
		return taskService.findById(id, authentication);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TaskResponse create(@Valid @RequestBody TaskRequest request, Authentication authentication) {
		return taskService.create(request, authentication);
	}

	@PutMapping("/{id}")
	public TaskResponse update(
			@PathVariable Long id,
			@Valid @RequestBody TaskRequest request,
			Authentication authentication
	) {
		return taskService.update(id, request, authentication);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id, Authentication authentication) {
		taskService.delete(id, authentication);
	}
}
