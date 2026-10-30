package backend.controller;

import backend.dto.AuthDtos.AuthResponse;
import backend.dto.AuthDtos.ForgotPasswordRequest;
import backend.dto.AuthDtos.LoginRequest;
import backend.dto.AuthDtos.MessageResponse;
import backend.dto.AuthDtos.RegisterRequest;
import backend.dto.AuthDtos.ResetPasswordRequest;
import backend.dto.AuthDtos.UserDto;
import backend.security.CurrentUser;
import backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return auth.forgotPassword(req.email());
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return auth.resetPassword(req.token(), req.password());
    }

    @GetMapping("/me")
    public UserDto me() {
        return auth.me(CurrentUser.require().getUserId());
    }

    @GetMapping("/verify-email")
    public MessageResponse verify(@RequestParam("token") String token) {
        return auth.verifyEmail(token);
    }

    @PostMapping("/resend-verification")
    public MessageResponse resend() {
        return auth.resendVerification(CurrentUser.require().getUserId());
    }
}
