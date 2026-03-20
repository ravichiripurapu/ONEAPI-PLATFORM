package io.oneapi.admin.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Email(message = "Invalid email format")
    private String email;

    private String firstName;
    private String lastName;
    private Boolean activated;
    private String langKey;
}
