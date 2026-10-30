package backend.dto;

import backend.model.Category;
import backend.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Name is required.")
            @Size(min = 2, max = 60, message = "Name must be 2-60 characters.")
            String name,

            @NotBlank(message = "Username is required.")
            @Size(min = 3, max = 24, message = "Username must be 3-24 characters.")
            @Pattern(regexp = "^[A-Za-z0-9_.]*$", message = "Username can use letters, numbers, dot and underscore only.")
            String username,

            @NotBlank(message = "Email is required.")
            @Email(message = "Enter a valid email address.")
            @Size(max = 120, message = "Email is too long.")
            String email,

            @NotBlank(message = "Password is required.")
            @Size(min = 8, max = 72, message = "Password must be 8-72 characters.")
            String password,

            @NotNull(message = "Choose how you will use FerryWala.")
            Role role,

            @Size(max = 80, message = "Shop name must be under 80 characters.")
            String shopName,

            Category category
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "Enter your email or username.") String identifier,
            @NotBlank(message = "Enter your password.") String password
    ) {
    }

    public record ForgotPasswordRequest(
            @NotBlank(message = "Enter your email address.")
            @Email(message = "Enter a valid email address.")
            @Size(max = 120, message = "Email is too long.")
            String email
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "This reset link is invalid or has expired.")
            @Size(max = 128, message = "This reset link is invalid or has expired.")
            String token,

            @NotBlank(message = "Password is required.")
            @Size(min = 8, max = 72, message = "Password must be 8-72 characters.")
            String password
    ) {
    }

    public record UserDto(
            Long id,
            String name,
            String username,
            String email,
            Role role,
            boolean emailVerified,
            Long vendorId
    ) {
    }

    public record AuthResponse(String token, UserDto user) {
    }

    public record MessageResponse(String message) {
    }
}
