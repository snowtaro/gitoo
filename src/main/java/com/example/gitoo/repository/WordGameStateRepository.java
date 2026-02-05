package com.example.gitoo.repository;

import com.example.gitoo.model.WordGameState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordGameStateRepository extends JpaRepository<WordGameState, String> {
}