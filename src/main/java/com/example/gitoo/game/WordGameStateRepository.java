package com.example.gitoo.game;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WordGameStateRepository extends JpaRepository<WordGameState, String> {
}