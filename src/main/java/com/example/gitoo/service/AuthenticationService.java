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
        if (userRepository.existsByUsername(registerRequest.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByNickname(registerRequest.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // ✅ 필수값 검증 (여기서 null이면 프론트 payload/DTO 매핑 문제)
        String schoolKey = registerRequest.getSchoolKey();
        String schoolName = registerRequest.getSchoolName();

        if (schoolKey == null || schoolKey.isBlank()) {
            throw new IllegalArgumentException("schoolKey는 필수입니다. (학교 찾기에서 선택 필요)");
        }
        if (schoolName == null || schoolName.isBlank()) {
            throw new IllegalArgumentException("schoolName은 필수입니다. (학교 찾기에서 선택 필요)");
        }

        // ✅ schoolKey 기준으로 조회/생성
        School school = schoolRepository.findBySchoolKey(schoolKey)
                .orElseGet(() -> {
                    School newSchool = new School();
                    newSchool.setSchoolKey(schoolKey);
                    newSchool.setSchoolName(schoolName);
                    return schoolRepository.save(newSchool);
                });

        User user = new User(
                registerRequest.getEmail(), // username field gets email
                registerRequest.getUsername(), // nickname field gets username (nickname)
                passwordEncoder.encode(registerRequest.getPassword()),
                Role.USER,
                school);

        user.setEnabled(true);
        return userRepository.save(user);
    }

    public User authenticate(LoginRequest loginRequest) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()));
        return (User) authentication.getPrincipal(); // Principal is User entity
    }
}
