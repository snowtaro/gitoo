package com.example.gitoo.room;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
@Entity
@Table(
        name = "room_members",
        uniqueConstraints = @UniqueConstraint(name = "uq_room_member", columnNames = {"room_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="room_id", nullable = false, length = 36)
    private String roomId;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 50)
    private String nickname;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RoomMemberRole role;

    @Column(nullable = false)
    private boolean ready;

    @Column(name="joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @PrePersist
    void prePersist() {
        if (joinedAt == null) joinedAt = Instant.now();
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }


    public void makeHost() { this.role = RoomMemberRole.HOST; }
    public void makeMember() { this.role = RoomMemberRole.MEMBER; }
}

