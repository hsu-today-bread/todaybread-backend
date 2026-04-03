package com.todaybread.server.domain.user.entity;

import com.todaybread.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JPA 유저 엔티티입니다. 롬복을 사용해, 빌더 형태를 제공합니다.
 * 기본 생성자는 호출 불가능합니다. 오직 빌더 형태로 작성 가능합니다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "nickname", nullable = false, unique = true, length = 30)
    private String nickname;

    @Column(name = "phone_number", nullable = false, unique = true, length = 30)
    private String phoneNumber;

    @Column(name = "is_boss", nullable = false)
    private Boolean isBoss = false;

    /**
     * 빌더 입니다. ID를 제외하고 모든 정보를 받습니다.
     *
     * @param email 이메일
     * @param passwordHash 해쉬된 비밀번호
     * @param name 이름
     * @param nickname 닉네임
     * @param phoneNumber 핸드폰 넘버
     */
    @Builder
    private UserEntity(String email, String passwordHash, String name, String nickname, String phoneNumber){
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
    }

    /**
     * 사장님 인증 후 사장님으로 바꿉니다.
     */
    public void approveBoss() {
        this.isBoss = true;
    }

    /**
     * 비밀번호 재설정 시, 기존 엔티티의 비밀번호를 수정합니다.
     *
     * @param newPassword 새 비밀번호
     */
    public void changePassword(String newPassword) {
        this.passwordHash = newPassword;
    }

    /**
     * 유저 정보를 업데이트 합니다.
     *
     * @param name 이름
     * @param nickname 닉네임
     * @param phoneNumber 전화번호
     */
    public void updateProfile(String name, String nickname, String phoneNumber){
        this.name = name;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
    }

}
