package io.oneapi.admin.service;

import io.oneapi.admin.dto.CreateUserRequest;
import io.oneapi.admin.dto.UpdateUserRequest;
import io.oneapi.admin.dto.UserDTO;
import io.oneapi.admin.security.domain.User;
import io.oneapi.admin.security.repository.UserRepository;
import io.oneapi.admin.security.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        // Check if user already exists
        if (userRepository.findOneByLogin(request.getLogin()).isPresent()) {
            throw new IllegalArgumentException("User already exists: " + request.getLogin());
        }
        if (request.getEmail() != null && userRepository.findOneByEmailIgnoreCase(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use: " + request.getEmail());
        }

        User user = new User();
        user.setLogin(request.getLogin());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setActivated(request.getActivated() != null ? request.getActivated() : true);

        user = userRepository.save(user);
        log.info("Created user: {}", user.getLogin());

        return toDTO(user);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        return toDTO(user);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserByLogin(String login) {
        User user = userRepository.findOneByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + login));
        return toDTO(user);
    }

    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());

        if (request.getActivated() != null) {
            user.setActivated(request.getActivated());
        }

        user = userRepository.save(user);
        log.info("Updated user: {}", user.getLogin());

        return toDTO(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        userRepository.deleteById(id);
        log.info("Deleted user: {}", id);
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setLogin(user.getLogin());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setActivated(user.getActivated());
        dto.setCreatedDate(user.getCreatedDate());
        dto.setLastModifiedDate(user.getLastModifiedDate());
        return dto;
    }
}
