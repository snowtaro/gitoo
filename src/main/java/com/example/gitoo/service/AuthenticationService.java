package com.example.gitoo.service;

import com.example.gitoo.dto.request.LoginRequest;
import com.example.gitoo.dto.request.RegisterRequest;
import com.example.gitoo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.example.gitoo.model.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gitoo.model.Role;

import com.example.gitoo.model.School;
import com.example.gitoo.repository.SchoolRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository; // Add dependency
    private final AuthenticationManager authenticationManager;

    public User signup(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        School school = schoolRepository.findBySchoolName(registerRequest.getSchoolName())
                .orElseGet(() -> {
                    School newSchool = new School();
                    newSchool.setSchoolName(registerRequest.getSchoolName());
                    return schoolRepository.save(newSchool);
                });

        User user = new User(registerRequest.getUsername(), registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPassword()), Role.USER, school);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User authenticate(LoginRequest loginRequest) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()));
        return (User) authentication.getPrincipal();
    }
}
