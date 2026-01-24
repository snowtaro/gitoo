package com.example.gitoo.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class LoginResponse {
    private String token;
    private Long expiration;
    private String role;

    public LoginResponse(String jwtToken, long expirationTime, String role) {
        this.token = jwtToken;
        this.expiration = expirationTime;
        this.role = role;
    }
}
