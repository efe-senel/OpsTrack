package com.opstrack.task;

import com.opstrack.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TaskRepository taskRepository;

	@Test
	void supportsTaskCrudLifecycle() throws Exception {
		taskRepository.deleteAll();

		String createdTask = mockMvc.perform(post("/api/v1/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new TaskRequest("Create pipeline", "Add CI steps", TaskStatus.OPEN)
						)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.title").value("Create pipeline"))
				.andExpect(jsonPath("$.status").value("OPEN"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long id = objectMapper.readTree(createdTask).get("id").asLong();

		mockMvc.perform(get("/api/v1/tasks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));

		mockMvc.perform(put("/api/v1/tasks/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new TaskRequest("Create pipeline", "Add Jenkinsfile later", TaskStatus.IN_PROGRESS)
						)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.description").value("Add Jenkinsfile later"));

		mockMvc.perform(delete("/api/v1/tasks/{id}", id))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/tasks/{id}", id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Task not found with id: " + id));
	}

	@Test
	void rejectsInvalidCreateRequest() throws Exception {
		mockMvc.perform(post("/api/v1/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new TaskRequest("", "Missing title", TaskStatus.OPEN))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationErrors.title").value("Title is required"));
	}
}
