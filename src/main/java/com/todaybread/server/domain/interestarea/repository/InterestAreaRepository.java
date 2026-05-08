package com.todaybread.server.domain.interestarea.repository;

import com.todaybread.server.domain.interestarea.entity.InterestAreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 관심지역을 위한 리포지터리입니다.
 * 유저별 관심지역 조회, 존재 여부 확인, 삭제 메서드를 제공합니다.
 */
public interface InterestAreaRepository extends JpaRepository<InterestAreaEntity, Long> {

    /**
     * 유저 ID로 관심지역을 조회합니다.
     *
     * @param userId 유저 ID
     * @return 관심지역 엔티티 (없으면 빈 Optional)
     */
    Optional<InterestAreaEntity> findByUserId(Long userId);

    /**
     * 여러 유저의 관심지역을 한 번에 조회합니다.
     *
     * @param userIds 유저 ID 목록
     * @return 관심지역 엔티티 목록
     */
    List<InterestAreaEntity> findByUserIdIn(Collection<Long> userIds);

    /**
     * 유저의 관심지역 존재 여부를 확인합니다.
     *
     * @param userId 유저 ID
     * @return 관심지역 존재 여부
     */
    boolean existsByUserId(Long userId);

    /**
     * 유저 ID로 관심지역을 삭제합니다.
     *
     * @param userId 유저 ID
     */
    void deleteByUserId(Long userId);
}
