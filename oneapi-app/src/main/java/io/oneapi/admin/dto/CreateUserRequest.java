package io.oneapi.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Login is required")
    private String login;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    private String firstName;
    private String lastName;

    @NotBlank(message = "Password is required")
    private String password;

    private Boolean activated = true;
    private String langKey = "en";
}
