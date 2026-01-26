package com.example.gitoo.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Room {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false)
    private int maxPlayers;

    @Column(nullable = false)
    private int nowPlayers;

    @Column(nullable = false)
    private boolean locked;

    // locked=true일 때만 값 존재(해시)
    @Column(length = 200)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
        if (nowPlayers < 0) nowPlayers = 0;
    }

    public void join() {
        if (nowPlayers >= maxPlayers) throw new IllegalStateException("방이 가득 찼습니다.");
        nowPlayers++;
    }
}
