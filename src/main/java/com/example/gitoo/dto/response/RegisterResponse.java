package com.example.gitoo.dto.response;

import com.example.gitoo.model.Role;
import com.example.gitoo.model.User;
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
        this.email = user.getEmail();
        this.username = user.getUsername();
        this.role = user.getRole();
        this.schoolName = user.getSchool().getSchoolName();
    }
}
