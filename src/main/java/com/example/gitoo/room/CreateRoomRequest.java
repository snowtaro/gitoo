package com.example.gitoo.room;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateRoomRequest(
        @NotBlank String title,
        @Min(2) @Max(8) int maxPlayers,
        boolean usePassword,
        String password
) {}
