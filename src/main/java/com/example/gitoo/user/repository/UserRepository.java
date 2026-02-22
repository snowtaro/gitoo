package com.example.gitoo.user.repository;

import com.example.gitoo.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username); // For Email (ID)

    Optional<User> findByNickname(String nickname); // For Nickname

    boolean existsByUsername(String username); // For Email (ID)

    boolean existsByNickname(String nickname); // For Nickname
}
