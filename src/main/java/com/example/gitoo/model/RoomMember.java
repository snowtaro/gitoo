package com.example.gitoo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "room_members",
        uniqueConstraints = @UniqueConstraint(name="uq_room_member", columnNames = {"room_id","user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RoomMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="room_id", nullable=false, length=36)
    private String roomId;

    @Column(name="user_id", nullable=false)
    private Long userId;

    @Column(nullable=false, length=50)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=10)
    private Role role;

    @Column(nullable=false)
    private boolean ready;

    @Column(nullable=false, updatable=false)
    private Instant joinedAt;

    @PrePersist
    void prePersist(){
        if(joinedAt == null) joinedAt = Instant.now();
    }

    public enum Role { HOST, MEMBER }
}
