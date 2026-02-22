package com.example.gitoo.user.controller;

import com.example.gitoo.system.config.JwtTokenProvider;
import com.example.gitoo.user.dto.LoginRequest;
import com.example.gitoo.user.dto.LoginResponse;
import com.example.gitoo.user.dto.RegisterRequest;
import com.example.gitoo.user.dto.RegisterResponse;
import com.example.gitoo.user.model.User;
import com.example.gitoo.user.service.AuthenticationService;
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

    @PostMapping("/signup") // ResponseEntity:서버에서 클라이언트로 보내는 클래스(상태코드+헤더+바디)
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest registerUserDto) { // JSON형태로 매개변수를 받을것이므로 @RequestBody를 작성해줘야함
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
