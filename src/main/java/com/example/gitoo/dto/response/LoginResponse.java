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

    public LoginResponse(String jwtToken, long expirationTime) {
        this.token = jwtToken;
        this.expiration = expirationTime;
    }
}
