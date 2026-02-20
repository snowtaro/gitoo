package com.example.gitoo.user;

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
