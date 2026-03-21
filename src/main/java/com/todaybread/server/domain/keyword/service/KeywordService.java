package com.todaybread.server.domain.keyword.service;

import com.todaybread.server.domain.keyword.dto.KeywordCreateRequest;
import com.todaybread.server.domain.keyword.dto.KeywordCreateResponse;
import com.todaybread.server.domain.keyword.dto.KeywordDeleteResponse;
import com.todaybread.server.domain.keyword.dto.KeywordResponse;
import com.todaybread.server.domain.keyword.entity.KeywordEntity;
import com.todaybread.server.domain.keyword.entity.UserKeywordEntity;
import com.todaybread.server.domain.keyword.repository.KeywordRepository;
import com.todaybread.server.domain.keyword.repository.UserKeywordRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 키워드 도메인 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class KeywordService {

    private static final int MAX_KEYWORDS_PER_USER = 5;
    private static final int MAX_KEYWORD_LENGTH = 10;

    private final KeywordRepository keywordRepository;
    private final UserKeywordRepository userKeywordRepository;

    /**
     * 키워드 텍스트를 정규화합니다.
     * 앞뒤 공백을 제거하고, 영어 문자가 포함된 경우 소문자로 변환합니다.
     * @param text 키워드
     * @return 정규화된 키워드
     */
    String normalise(String text) {
        String stripped = text.strip();
        boolean hasEnglish = false;
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (Character.isLetter(c) && c < 128) {
                hasEnglish = true;
                break;
            }
        }
        if (hasEnglish) {
            return stripped.toLowerCase();
        }
        return stripped;
    }

    /**
     * 키워드를 등록합니다.
     * 정규화 → KeywordEntity 조회/생성 → 중복 검증 → 개수 검증 → UserKeywordEntity 생성
     * @param userId 유저 아이디
     * @param request 요청 DTO
     */
    @Transactional
    public KeywordCreateResponse createKeyword(Long userId, KeywordCreateRequest request) {
        String normalisedText = normalise(request.keyword());

        if (normalisedText.length() > MAX_KEYWORD_LENGTH) {
            throw new CustomException(ErrorCode.KEYWORD_LENGTH_LIMIT);
        }

        Optional<KeywordEntity> keywordEntityOptional = keywordRepository.findByNormalisedText(normalisedText);

        KeywordEntity keywordEntity;
        if (keywordEntityOptional.isEmpty()) {
            try {
                keywordEntity = keywordRepository.save(
                        KeywordEntity.builder().normalisedText(normalisedText).build()
                );
            } catch (DataIntegrityViolationException e) {
                Optional<KeywordEntity> retryOptional = keywordRepository.findByNormalisedText(normalisedText);
                if (retryOptional.isEmpty()) {
                    throw new CustomException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
                }
                keywordEntity = retryOptional.get();
            }
        } else {
            keywordEntity = keywordEntityOptional.get();
        }

        if (userKeywordRepository.existsByUserIdAndKeywordId(userId, keywordEntity.getId())) {
            throw new CustomException(ErrorCode.KEYWORD_ALREADY_EXISTS);
        }

        if (userKeywordRepository.findByUserId(userId).size() >= MAX_KEYWORDS_PER_USER) {
            throw new CustomException(ErrorCode.KEYWORD_LIMIT_EXCEEDED);
        }

        UserKeywordEntity savedEntity;
        try {
            savedEntity = userKeywordRepository.save(
                    UserKeywordEntity.builder()
                            .userId(userId)
                            .keywordId(keywordEntity.getId())
                            .displayText(request.keyword())
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.KEYWORD_ALREADY_EXISTS);
        }

        // 동시성 체크: 저장 후 개수 확인하여 5개 초과 시 롤백
        if (userKeywordRepository.findByUserId(userId).size() > MAX_KEYWORDS_PER_USER) {
            userKeywordRepository.delete(savedEntity);
            throw new CustomException(ErrorCode.KEYWORD_LIMIT_EXCEEDED);
        }

        return KeywordCreateResponse.ok();
    }

    /**
     * 사용자의 키워드 목록을 조회합니다.
     * @param userId 사용자 ID
     * @return 키워드 응답 DTO 목록 (등록된 키워드가 없으면 빈 목록)
     */
    @Transactional(readOnly = true)
    public List<KeywordResponse> getMyKeywords(Long userId) {
        List<UserKeywordEntity> userKeywords = userKeywordRepository.findByUserId(userId);
        List<KeywordResponse> responses = new ArrayList<>();
        for (UserKeywordEntity keyword : userKeywords) {
            responses.add(new KeywordResponse(keyword.getId(), keyword.getDisplayText()));
        }
        return responses;
    }

    /**
     * 키워드를 삭제합니다.
     * @param userId 사용자 ID
     * @param userKeywordId 삭제할 UserKeyword ID
     * @return 삭제 결과 응답
     */
    @Transactional
    public KeywordDeleteResponse deleteKeyword(Long userId, Long userKeywordId) {
        Optional<UserKeywordEntity> entityOptional = userKeywordRepository.findById(userKeywordId);

        if (entityOptional.isEmpty()) {
            throw new CustomException(ErrorCode.KEYWORD_NOT_FOUND);
        }

        UserKeywordEntity entity = entityOptional.get();

        if (!entity.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.KEYWORD_FORBIDDEN);
        }

        userKeywordRepository.delete(entity);
        return KeywordDeleteResponse.ok();
    }
}
