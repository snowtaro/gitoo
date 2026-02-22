package com.example.gitoo.game.repository;

import com.example.gitoo.game.model.WordGameState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordGameStateRepository extends JpaRepository<WordGameState, String> {
}