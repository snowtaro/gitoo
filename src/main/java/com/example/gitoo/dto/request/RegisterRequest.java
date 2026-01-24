package com.example.gitoo.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String email;
    private String password;
    private String username;
    private String schoolName;
    private String schoolKey;
}
