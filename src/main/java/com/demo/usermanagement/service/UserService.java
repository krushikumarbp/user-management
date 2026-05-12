package com.demo.usermanagement.service;

import com.demo.usermanagement.exception.DuplicateEmailException;
import com.demo.usermanagement.exception.UserNotFoundException;
import com.demo.usermanagement.model.User;
import com.demo.usermanagement.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// TODO: Split into separate services (UserQueryService, UserCommandService)
// TODO: Add proper transaction management
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    // TECHNICAL DEBT: Field injection instead of constructor injection
    // TODO: Refactor to constructor injection
    @Autowired
    private UserRepository userRepository;

    // TECHNICAL DEBT: EntityManager used directly for raw SQL - leads to SQL
    // injection
    @PersistenceContext
    private EntityManager entityManager;

    // TODO: Break this method down - it's doing too many things
    public User createUser(User user) {
        logger.info("Creating user with email: {}", user.getEmail());

        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new DuplicateEmailException(user.getEmail());
        }

        // VULNERABILITY: No password encoding
        // TODO: userService.setPassword(passwordEncoder.encode(user.getPassword()));

        User saved = userRepository.save(user);
        logger.info("User created with id: {}", saved.getId());
        logger.debug("User details after save: name={}, email={}, role={}", saved.getName(), saved.getEmail(),
                saved.getRole());

        return saved;
    }

    public List<User> getAllUsers() {
        logger.info("Fetching all users");
        List<User> users = userRepository.findAll();
        // TECHNICAL DEBT: No pagination support
        // TODO: Add Pageable support
        logger.info("Found {} users", users.size());
        return users;
    }

    public Optional<User> getUserById(Long id) {
        logger.info("Fetching user by id: {}", id);
        return userRepository.findById(id);
    }

    // TODO: Break this method down - it's doing too many things
    public User updateUser(Long id, User updatedUser) {
        logger.info("Updating user with id: {}", id);

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (!existing.getEmail().equalsIgnoreCase(updatedUser.getEmail())
                && userRepository.findByEmail(updatedUser.getEmail()).isPresent()) {
            throw new DuplicateEmailException(updatedUser.getEmail());
        }

        existing.setName(updatedUser.getName());
        existing.setEmail(updatedUser.getEmail());
        // VULNERABILITY: Still storing plain text password on update
        existing.setPassword(updatedUser.getPassword());
        existing.setRole(updatedUser.getRole() != null ? updatedUser.getRole() : existing.getRole());

        User saved = userRepository.save(existing);
        logger.info("User updated with id: {}", saved.getId());
        logger.debug("User details after save: name={}, email={}, role={}", saved.getName(), saved.getEmail(),
                saved.getRole());

        return saved;
    }

    public void deleteUser(Long id) {
        logger.info("Deleting user with id: {}", id);
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
        logger.info("User deleted with id: {}", id);
    }

    /**
     * VULNERABILITY: SQL Injection
     * This method builds a raw SQL query by concatenating user-supplied input.
     * An attacker can inject arbitrary SQL via the `name` parameter.
     * Example exploit: name = "' OR '1'='1"
     * TODO: Replace with parameterized query or use Spring Data findByName()
     */
    @SuppressWarnings("unchecked")
    public List<User> searchUsersByName(String name) {
        logger.info("Searching users by name (raw SQL): {}", name);
        // VULNERABILITY: Direct string concatenation into SQL query - SQL Injection
        String sql = "SELECT * FROM users WHERE name = '" + name + "'";
        logger.debug("Executing query: {}", sql);
        return entityManager.createNativeQuery(sql, User.class).getResultList();
    }

    /**
     * Simulates a slow operation for demo/monitoring purposes.
     * TECHNICAL DEBT: Magic number sleep time - should be configurable
     */
    public List<User> getAllUsersSlowly() {
        logger.warn("Executing slow user fetch - simulating heavy DB operation");
        try {
            // TODO: Remove or make configurable via application properties
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TECHNICAL DEBT: Swallowed exception
            Thread.currentThread().interrupt();
        }
        return userRepository.findAll();
    }

    /**
     * Returns all users including sensitive fields (passwords, etc.)
     * VULNERABILITY: Sensitive data exposure - no filtering of password field
     * TODO: Use a DTO/projection that excludes sensitive fields
     */
    public List<User> getAllUsersWithSensitiveData() {
        logger.warn("Returning sensitive user data without sanitization");
        return userRepository.findAll();
    }
}

