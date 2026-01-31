package com.example.gitoo.controller;

import com.example.gitoo.dto.request.LoginRequest;
import com.example.gitoo.dto.request.RegisterRequest;
import com.example.gitoo.dto.response.LoginResponse;
import com.example.gitoo.dto.response.RegisterResponse;
import com.example.gitoo.model.User;
import com.example.gitoo.security.JwtTokenProvider;
import com.example.gitoo.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationService authenticationService;

    @PostMapping("/signup")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest registerUserDto) {
        User registeredUser = authenticationService.signup(registerUserDto);
        return ResponseEntity.ok(new RegisterResponse(registeredUser));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginRequest loginUserDto) {
        User authenticatedUser = authenticationService.authenticate(loginUserDto);
        String jwtToken = jwtTokenProvider.generateTokenWithNickname(
                authenticatedUser,
                authenticatedUser.getNickname()
        );
        LoginResponse loginResponse = new LoginResponse(jwtToken, jwtTokenProvider.getExpirationTime(),
                authenticatedUser.getRole().name());
        return ResponseEntity.ok(loginResponse);
    }

}
