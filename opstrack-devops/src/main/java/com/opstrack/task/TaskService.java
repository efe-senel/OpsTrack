package com.opstrack.task;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

	private final TaskRepository taskRepository;

	public TaskService(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	@Transactional(readOnly = true)
	public List<TaskResponse> findAll() {
		return taskRepository.findAll()
				.stream()
				.map(TaskResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public TaskResponse findById(Long id) {
		return TaskResponse.from(getTask(id));
	}

	@Transactional
	public TaskResponse create(TaskRequest request) {
		Task task = new Task(request.title(), request.description(), request.status());
		return TaskResponse.from(taskRepository.save(task));
	}

	@Transactional
	public TaskResponse update(Long id, TaskRequest request) {
		Task task = getTask(id);
		task.setTitle(request.title());
		task.setDescription(request.description());
		task.setStatus(request.status());
		return TaskResponse.from(task);
	}

	@Transactional
	public void delete(Long id) {
		Task task = getTask(id);
		taskRepository.delete(task);
	}

	private Task getTask(Long id) {
		return taskRepository.findById(id)
				.orElseThrow(() -> new TaskNotFoundException(id));
	}
}
