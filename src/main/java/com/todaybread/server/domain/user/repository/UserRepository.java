package com.todaybread.server.domain.user.repository;

import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.stereotype.Repository;

/**
 * 유저 도메인 용 리포지터리입니다.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity> {
}
