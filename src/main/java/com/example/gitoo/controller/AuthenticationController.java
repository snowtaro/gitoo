package com.example.gitoo.controller;

import com.example.gitoo.dto.request.LoginRequest;
import com.example.gitoo.dto.request.RegisterRequest;
import com.example.gitoo.dto.response.LoginResponse;
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
    public ResponseEntity<User> register(@RequestBody RegisterRequest registerUserDto) {
        User registeredUser = authenticationService.signup(registerUserDto);
        return ResponseEntity.ok(registeredUser); // Http 200을 Body에 registeredUser를 넣어 전송. created로 수정하기
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginRequest loginUserDto) {
        User authenticatedUser = authenticationService.authenticate(loginUserDto);
        String jwtToken = jwtTokenProvider.generateToken(authenticatedUser);
        LoginResponse loginResponse = new LoginResponse(jwtToken, jwtTokenProvider.getExpirationTime());
        return ResponseEntity.ok(loginResponse);
    }

}
