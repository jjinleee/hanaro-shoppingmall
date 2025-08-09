package com.ijin.hanaro.user;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length = 50) private String username;
    @Column(nullable=false) private String password;
    @Column(nullable=false) private String nickname;
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, columnDefinition = "ENUM('ROLE_ADMIN','ROLE_USER')")
    private Role role = Role.ROLE_USER;

    @Column(nullable=false) private boolean enabled = true;
    private java.time.LocalDateTime deletedAt;
    public enum Role { ROLE_ADMIN, ROLE_USER }
}