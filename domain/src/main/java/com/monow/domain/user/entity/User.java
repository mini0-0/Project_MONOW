package com.monow.domain.user.entity;

import jakarta.persistence.*;
import com.monow.global.common.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name= "email",unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "real_name", nullable = false, length = 50)
    private String name;

    @Column(name = "nickname",unique = true, nullable = false, length = 255)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole userRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus userStatus;


    public static User createUser(String email, String password, String name, String nickname) {
        User user = new User();
        user.email = email;
        user.password = password;
        user.name = name;
        user.nickname = nickname;
        user.userRole = UserRole.USER;
        user.userStatus = UserStatus.ACTIVE;

        return user;
    }
}
