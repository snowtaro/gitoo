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

@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    public User signup(RegisterRequest registerRequest) {
        User user = new User(registerRequest.getUsername(), registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPassword()));
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
