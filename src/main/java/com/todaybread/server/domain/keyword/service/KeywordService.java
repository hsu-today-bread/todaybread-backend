package com.todaybread.server.domain.keyword.service;

import com.todaybread.server.domain.keyword.dto.KeywordResponse;
import com.todaybread.server.domain.keyword.entity.KeywordEntity;
import com.todaybread.server.domain.keyword.entity.UserKeywordEntity;
import com.todaybread.server.domain.keyword.repository.KeywordRepository;
import com.todaybread.server.domain.keyword.repository.UserKeywordRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KeywordService {

    private final KeywordRepository keywordRepository;
    private final UserKeywordRepository userKeywordRepository;

    /**
     * 키워드를 추가합니다.
     * @param userId 유저 ID
     * @param keyword 추가할 키워드 텍스트
     * @return 생성/조회된 키워드 ID
     */
    @Transactional
    public Long addKeyword(Long userId, String keyword) {
        String normalisedText = keyword.trim().replaceAll("\\s+", "");

        KeywordEntity keywordEntity;
        Optional<KeywordEntity> existingKeyword = keywordRepository.findByNormalisedText(normalisedText);
        if (existingKeyword.isPresent()) {
            keywordEntity = existingKeyword.get();
        } else {
            keywordEntity = keywordRepository.save(KeywordEntity.builder()
                    .normalisedText(normalisedText)
                    .build());
        }

        // 유저의 키워드 목록을 가져와서 중복 확인
        List<UserKeywordEntity> userKeywords = userKeywordRepository.findByUserId(userId);
        boolean alreadyExists = userKeywords.stream()
                .anyMatch(uk -> uk.getKeywordId().equals(keywordEntity.getId()));

        if (alreadyExists) {
            throw new CustomException(ErrorCode.KEYWORD_ALREADY_EXISTS);
        }

        UserKeywordEntity userKeywordEntity = UserKeywordEntity.builder()
                .userId(userId)
                .keywordId(keywordEntity.getId())
                .displayText(keyword)
                .build();

        userKeywordRepository.save(userKeywordEntity);

        return keywordEntity.getId();
    }

    /**
     * 키워드를 삭제합니다.
     * @param userId 유저 ID
     * @param keywordId 키워드 ID
     */
    @Transactional
    public void deleteKeyword(Long userId, Long keywordId) {
        List<UserKeywordEntity> userKeywords = userKeywordRepository.findByUserId(userId);
        
        UserKeywordEntity targetEntity = userKeywords.stream()
                .filter(uk -> uk.getKeywordId().equals(keywordId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.USER_KEYWORD_NOT_FOUND));

        userKeywordRepository.delete(targetEntity);
    }

    /**
     * 특정 유저의 등록된 키워드 목록을 조회합니다.
     * @param userId 유저 ID
     * @return 등록된 유저 키워드 DTO 목록
     */
    @Transactional(readOnly = true)
    public List<KeywordResponse> getUserKeywords(Long userId) {
        return userKeywordRepository.findByUserId(userId).stream()
                .map(KeywordResponse::from)
                .toList();
    }

    /**
     * 특정 키워드를 구독 중인 유저 연결 목록을 조회합니다.
     * @param keywordId 키워드 ID
     * @return 유저-키워드 연결 엔티티 목록
     */
    @Transactional(readOnly = true)
    public List<UserKeywordEntity> getSubscribers(Long keywordId) {
        return userKeywordRepository.findByKeywordId(keywordId);
    }

    /**
     * 특정 유저가 등록한 키워드가 존재하는지 확인합니다.
     * @param userId 유저 ID
     * @return 존재 여부
     */
    @Transactional(readOnly = true)
    public boolean hasKeywords(Long userId) {
        return userKeywordRepository.existsByUserId(userId);
    }
}
