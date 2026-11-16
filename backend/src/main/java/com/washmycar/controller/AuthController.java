package com.washmycar.controller;

import com.washmycar.dto.Dtos.*;
import com.washmycar.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/client/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse registerClient(@Valid @RequestBody ClientRegisterRequest req) {
        return auth.registerClient(req);
    }

    @GetMapping("/verify-email")
    public MessageResponse verifyEmail(@RequestParam String token) {
        return auth.verifyEmail(token);
    }

    @PostMapping("/station/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse registerStation(@Valid @RequestBody StationRegisterRequest req) {
        return auth.registerStation(req);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }
}
