package com.todaybread.server.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA 유저 엔티티입니다. 롬복을 사용해, 빌더 형태를 제공합니다.
 * 기본 생성자는 호출 불가능합니다. 오직 빌더 형태로 작성 가능합니다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Setter
    @Column(nullable = false, length = 30)
    private String name;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Setter
    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    @Setter
    @Column(name = "phone_number", unique = true, length = 30)
    private String phone;

    @Column(name = "is_boss", nullable = false)
    private boolean boss = false;

    /**
     * 빌더 입니다. ID를 제외하고 모든 정보를 받습니다.
     * @param email 이메일
     * @param passwordHash 해쉬된 비밀번호
     * @param name 이름
     * @param nickname 닉네임
     * @param phone 핸드폰 넘버
     */
    @Builder
    private UserEntity(String email, String passwordHash, String name, String nickname, String phone){
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.phone = phone;
    }

    /**
     * 사장님 인증 후 사장님으로 바꿉니다.
     */
    public void approveBoss() {
        this.boss = true;
    }
}
