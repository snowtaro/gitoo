package com.example.gitoo.user.service;

import com.example.gitoo.user.model.User;
import com.example.gitoo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = findUser(username);
        validatePassword(currentPassword, newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deleteAccount(String username, String password) {
        User user = findUser(username);
        validatePassword(user.getPassword(), password);
        userRepository.delete(user);
    }

    public void addPoints(String nickname, long points) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        user.setScore(user.getScore() + points);
    }

    public User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private void validatePassword(String password, String original) {
        if (!passwordEncoder.matches(original, password)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
    }
}
