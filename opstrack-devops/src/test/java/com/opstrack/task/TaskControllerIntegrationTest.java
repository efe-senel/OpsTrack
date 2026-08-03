package com.opstrack.task;

import com.opstrack.TestcontainersConfiguration;
import com.opstrack.auth.entity.User;
import com.opstrack.auth.repository.UserRepository;
import com.opstrack.task.dto.TaskRequest;
import com.opstrack.task.entity.TaskStatus;
import com.opstrack.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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

	private static final String FIRST_EMAIL = "first@example.com";
	private static final String SECOND_EMAIL = "second@example.com";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private User firstUser;
	private User secondUser;

	@BeforeEach
	void setUpUsers() {
		taskRepository.deleteAll();
		userRepository.deleteAll();
		firstUser = userRepository.saveAndFlush(
				new User("First User", FIRST_EMAIL, passwordEncoder.encode("password123"))
		);
		secondUser = userRepository.saveAndFlush(
				new User("Second User", SECOND_EMAIL, passwordEncoder.encode("password123"))
		);
	}

	@Test
	void supportsTaskCrudLifecycleForOwner() throws Exception {
		Long id = createTaskAs(FIRST_EMAIL, "Create pipeline");

		assertThat(taskRepository.findById(id).orElseThrow().getOwner().getId())
				.isEqualTo(firstUser.getId());

		mockMvc.perform(get("/api/v1/tasks")
						.with(user(FIRST_EMAIL).roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].title").value("Create pipeline"))
				.andExpect(jsonPath("$[0].owner").doesNotExist());

		mockMvc.perform(put("/api/v1/tasks/{id}", id)
						.with(user(FIRST_EMAIL).roles("USER"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new TaskRequest(
										"Create pipeline",
										"Add Jenkinsfile later",
										TaskStatus.IN_PROGRESS
								)
						)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.description").value("Add Jenkinsfile later"));

		mockMvc.perform(delete("/api/v1/tasks/{id}", id)
						.with(user(FIRST_EMAIL).roles("USER"))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/tasks/{id}", id)
						.with(user(FIRST_EMAIL).roles("USER")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Task not found with id: " + id));
	}

	@Test
	void isolatesTasksByOwnerAndHidesUnownedLegacyTasks() throws Exception {
		Long firstTaskId = createTaskAs(FIRST_EMAIL, "First user's task");
		createTaskAs(SECOND_EMAIL, "Second user's task");

		jdbcTemplate.update("""
				INSERT INTO tasks (title, description, status, created_at, updated_at, owner_id)
				VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL)
				""", "Legacy task", "No owner", TaskStatus.OPEN.name());

		mockMvc.perform(get("/api/v1/tasks")
						.with(user(FIRST_EMAIL).roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].title").value("First user's task"));

		mockMvc.perform(get("/api/v1/tasks/{id}", firstTaskId)
						.with(user(SECOND_EMAIL).roles("USER")))
				.andExpect(status().isNotFound());

		mockMvc.perform(put("/api/v1/tasks/{id}", firstTaskId)
						.with(user(SECOND_EMAIL).roles("USER"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new TaskRequest("Hijacked", "Should fail", TaskStatus.DONE)
						)))
				.andExpect(status().isNotFound());

		mockMvc.perform(delete("/api/v1/tasks/{id}", firstTaskId)
						.with(user(SECOND_EMAIL).roles("USER"))
						.with(csrf()))
				.andExpect(status().isNotFound());

		assertThat(taskRepository.findById(firstTaskId)).isPresent();
		assertThat(taskRepository.findById(firstTaskId).orElseThrow().getTitle())
				.isEqualTo("First user's task");
		assertThat(secondUser.getId()).isNotEqualTo(firstUser.getId());
	}

	@Test
	void rejectsInvalidCreateRequest() throws Exception {
		mockMvc.perform(post("/api/v1/tasks")
						.with(user(FIRST_EMAIL).roles("USER"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new TaskRequest("", "Missing title", TaskStatus.OPEN)
						)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.validationErrors.title").value("Title is required"));
	}

	private Long createTaskAs(String email, String title) throws Exception {
		String createdTask = mockMvc.perform(post("/api/v1/tasks")
						.with(user(email).roles("USER"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new TaskRequest(title, "Task description", TaskStatus.OPEN)
						)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", notNullValue()))
				.andReturn()
				.getResponse()
				.getContentAsString();

		return objectMapper.readTree(createdTask).get("id").asLong();
	}
}
