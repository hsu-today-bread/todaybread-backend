package com.todaybread.server.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저 정보를 담고 있는 엔티티 클래스입니다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(length = 30, nullable = false)
    private String name;

    @Column(length = 30, nullable = false, unique = true)
    private String nickname;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "is_boss", nullable = false)
    private Boolean isBoss = false;

    /**
     * 내용: UserEntity를 생성하는 빌더 메서드입니다.
     *
     * @param email 유저 이메일
     * @param passwordHash 암호화된 비밀번호
     * @param name 실명
     * @param nickname 서비스 활동 닉네임
     * @param phoneNumber 전화번호
     * @param isBoss 사장님 여부
     */
    @Builder
    public UserEntity(String email, String passwordHash, String name, String nickname, String phoneNumber, Boolean isBoss) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        if (isBoss != null) {
            this.isBoss = isBoss;
        }
    }
}