package com.example.gitoo.user.dto;

import com.example.gitoo.user.model.User;

public record UserInfoResponse(
        String username,
        String nickname,
        long score,
        String role
) {
    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getUsername(), user.getNickname(), user.getScore(), user.getRole().name()
        );
    }
}
