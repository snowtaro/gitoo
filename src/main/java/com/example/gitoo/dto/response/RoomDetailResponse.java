package com.example.gitoo.dto.response;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDetailResponse {
    private String id;
    private String title;
    private int max;
    private int now;
    private boolean locked;
    private List<Member> members;

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Member {
        private Long userId;
        private String username;
        private String role;   // HOST/MEMBER
        private boolean ready;
        private String schoolName; // 지금은 null 가능
    }
}
