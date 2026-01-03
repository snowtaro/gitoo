package com.example.gitoo.service;

import com.example.gitoo.dto.request.LoginDto;
import com.example.gitoo.dto.request.RegisterDto;
import com.example.gitoo.repository.UserRepository;
import com.example.gitoo.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.gitoo.model.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    public User signup(RegisterDto registerDto){
        User user = new User(registerDto.getUsername(), registerDto.getEmail(), passwordEncoder.encode(registerDto.getPassword()));
        user.setEnabled(true);
        return userRepository.save(user);
    }
    public User authenticate(LoginDto loginDto){
        User user =userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if(!user.isEnabled()){
            throw new RuntimeException("Please verify your email or password");
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getEmail(),
                        loginDto.getPassword()
                )
        );
        return user;
    }
}
