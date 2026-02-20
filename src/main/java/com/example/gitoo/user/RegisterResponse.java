package com.example.gitoo.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterResponse {
    private String email;
    private String username;
    private Role role;
    private String schoolName;

    public RegisterResponse(User user) {
        this.email = user.getUsername(); // username filed is email
        this.username = user.getNickname(); // nickname field is nickname
        this.role = user.getRole();
        this.schoolName = user.getSchool().getSchoolName();
    }
}
