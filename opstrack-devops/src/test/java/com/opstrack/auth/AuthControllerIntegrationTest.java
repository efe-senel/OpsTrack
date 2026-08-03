package com.opstrack.auth;

import com.opstrack.TestcontainersConfiguration;
import com.opstrack.auth.dto.LoginRequest;
import com.opstrack.auth.dto.RegisterRequest;
import com.opstrack.auth.entity.User;
import com.opstrack.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void registersUserAndHashesPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Efe",
                "efe@example.com",
                "strongPassword123"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Efe"))
                .andExpect(jsonPath("$.email").value("efe@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        User savedUser = userRepository
                .findByEmailIgnoreCase("efe@example.com")
                .orElseThrow();

        assertThat(savedUser.getPasswordHash())
                .isNotEqualTo("strongPassword123");

        assertThat(passwordEncoder.matches(
                "strongPassword123",
                savedUser.getPasswordHash()
        )).isTrue();
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() throws Exception {
        RegisterRequest firstRequest = new RegisterRequest(
                "Efe",
                "efe@example.com",
                "strongPassword123"
        );

        RegisterRequest duplicateRequest = new RegisterRequest(
                "Other Efe",
                "EFE@EXAMPLE.COM",
                "anotherPassword123"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/auth/register"));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidRegistrationRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "",
                "invalid-email",
                "123"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void logsInAndPersistsAuthenticationInSession() throws Exception {
        registerUser();

        LoginRequest loginRequest = new LoginRequest(
                "EFE@EXAMPLE.COM",
                "strongPassword123"
        );

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("efe@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult
                .getRequest()
                .getSession(false);

        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/v1/tasks")
                        .session(session))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidLoginCredentials() throws Exception {
        registerUser();

        LoginRequest loginRequest = new LoginRequest(
                "efe@example.com",
                "wrongPassword"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message")
                        .value("Invalid email or password"));
    }

    private void registerUser() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Efe",
                "efe@example.com",
                "strongPassword123"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}