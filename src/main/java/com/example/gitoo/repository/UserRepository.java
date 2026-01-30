package com.example.gitoo.repository;

import com.example.gitoo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username); // For Email (ID)

    boolean existsByUsername(String username); // For Email (ID)

    boolean existsByNickname(String nickname); // For Nickname

}
