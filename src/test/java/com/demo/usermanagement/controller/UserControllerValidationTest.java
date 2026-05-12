package com.demo.usermanagement.controller;

import com.demo.usermanagement.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UserControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // -------------------------------------------------------------------------
    // Input Validation Tests
    // -------------------------------------------------------------------------

    @Test
    void createUser_withBlankName_returns400WithDetails() throws Exception {
        User user = new User(null, "", "valid@example.com", "secret123", "USER");
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details", hasItem(containsString("name"))));
    }

    @Test
    void createUser_withInvalidEmail_returns400WithDetails() throws Exception {
        User user = new User(null, "Alice", "not-an-email", "secret123", "USER");
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details", hasItem(containsString("email"))));
    }

    @Test
    void createUser_withShortPassword_returns400WithDetails() throws Exception {
        User user = new User(null, "Alice", "alice@example.com", "abc", "USER");
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details", hasItem(containsString("password"))));
    }

    @Test
    void createUser_withValidData_returns201() throws Exception {
        User user = new User(null, "Bob", "newuser@example.com", "password1", "USER");
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("newuser@example.com"));
    }

    @Test
    void createUser_withoutRole_defaultsToUser() throws Exception {
        User user = new User(null, "Charlie", "charlie2@example.com", "password1", null);
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(user)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    // -------------------------------------------------------------------------
    // Global Exception Handler Tests
    // -------------------------------------------------------------------------

    @Test
    void getUserById_withNonExistentId_returns404WithErrorBody() throws Exception {
        mockMvc.perform(get("/users/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("99999")));
    }

    @Test
    void updateUser_withNonExistentId_returns404WithErrorBody() throws Exception {
        User user = new User(null, "Alice", "alice@example.com", "secret123", "USER");
        mockMvc.perform(put("/users/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteUser_withNonExistentId_returns404WithErrorBody() throws Exception {
        mockMvc.perform(delete("/users/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createUser_withDuplicateEmail_returns409() throws Exception {
        // Use an email already seeded by data.sql (alice@example.com)
        User user = new User(null, "Another Alice", "alice@example.com", "secret123", "USER");
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(user)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void createUser_withMalformedJson_returns400() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not valid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getUserById_withStringId_returns400() throws Exception {
        mockMvc.perform(get("/users/notAnId"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
