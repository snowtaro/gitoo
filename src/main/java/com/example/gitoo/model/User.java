package com.example.gitoo.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username; // Now used for Email
    @Column(unique = true, nullable = false)
    private String nickname; // Was email, now nickname
    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    // DB 스키마 불일치 해결: DB에는 points 컬럼이 있고, Entity는 score를 사용 중일 경우
    // 에러 로그: Field 'points' doesn't have a default value
    @Column(name = "points", nullable = false)
    private Long score = 0L;

    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public User(String username, String nickname, String password, Role role, School school) {
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.role = role;
        this.school = school;
    }

    public User() { // JPA는 Entity를 리플랙션으로 생성하므로 default constructor가 반드시 필요함
    }

    // getPassword, getUsername
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

}
