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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link KeywordService}의 단위 테스트입니다.
 * 키워드 등록, 조회, 삭제, 정규화 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class KeywordServiceTest {

    @InjectMocks
    private KeywordService keywordService;

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private UserKeywordRepository userKeywordRepository;

    @Test
    @DisplayName("createKeyword_성공")
    void createKeyword_success() {
        // given
        Long userId = 1L;
        KeywordCreateRequest request = new KeywordCreateRequest("소금빵");
        KeywordEntity keywordEntity = createKeywordEntity(10L, "소금빵");

        when(keywordRepository.findByNormalisedText("소금빵")).thenReturn(Optional.empty());
        when(keywordRepository.save(any(KeywordEntity.class))).thenReturn(keywordEntity);
        when(userKeywordRepository.existsByUserIdAndKeywordId(userId, 10L)).thenReturn(false);
        when(userKeywordRepository.countByUserIdWithLock(userId)).thenReturn(0L);

        // when
        KeywordCreateResponse response = keywordService.createKeyword(userId, request);

        // then
        assertThat(response.success()).isTrue();
        verify(keywordRepository).save(any(KeywordEntity.class));
        verify(userKeywordRepository).save(any(UserKeywordEntity.class));
    }

    @Test
    @DisplayName("createKeyword_길이초과_에러")
    void createKeyword_lengthExceeded_error() {
        // given
        Long userId = 1L;
        KeywordCreateRequest request = new KeywordCreateRequest("이것은열한글자키워드임");

        // when & then
        assertThatThrownBy(() -> keywordService.createKeyword(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.KEYWORD_LENGTH_LIMIT);
    }

    @Test
    @DisplayName("createKeyword_중복등록_에러")
    void createKeyword_alreadyExists_error() {
        // given
        Long userId = 1L;
        KeywordCreateRequest request = new KeywordCreateRequest("소금빵");
        KeywordEntity keywordEntity = createKeywordEntity(10L, "소금빵");

        when(keywordRepository.findByNormalisedText("소금빵")).thenReturn(Optional.of(keywordEntity));
        when(userKeywordRepository.existsByUserIdAndKeywordId(userId, 10L)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> keywordService.createKeyword(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.KEYWORD_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("createKeyword_5개초과_에러")
    void createKeyword_limitExceeded_error() {
        // given
        Long userId = 1L;
        KeywordCreateRequest request = new KeywordCreateRequest("소금빵");
        KeywordEntity keywordEntity = createKeywordEntity(10L, "소금빵");

        when(keywordRepository.findByNormalisedText("소금빵")).thenReturn(Optional.of(keywordEntity));
        when(userKeywordRepository.existsByUserIdAndKeywordId(userId, 10L)).thenReturn(false);
        when(userKeywordRepository.countByUserIdWithLock(userId)).thenReturn(5L);

        // when & then
        assertThatThrownBy(() -> keywordService.createKeyword(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.KEYWORD_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("getMyKeywords_빈목록")
    void getMyKeywords_empty() {
        // given
        Long userId = 1L;
        when(userKeywordRepository.findByUserId(userId)).thenReturn(List.of());

        // when
        List<KeywordResponse> result = keywordService.getMyKeywords(userId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("deleteKeyword_성공")
    void deleteKeyword_success() {
        // given
        Long userId = 1L;
        Long userKeywordId = 100L;
        UserKeywordEntity entity = createUserKeywordEntity(userKeywordId, userId, 10L, "소금빵");

        when(userKeywordRepository.findById(userKeywordId)).thenReturn(Optional.of(entity));

        // when
        KeywordDeleteResponse response = keywordService.deleteKeyword(userId, userKeywordId);

        // then
        assertThat(response.success()).isTrue();
        verify(userKeywordRepository).delete(entity);
    }

    @Test
    @DisplayName("deleteKeyword_존재하지않는키워드_에러")
    void deleteKeyword_notFound_error() {
        // given
        Long userId = 1L;
        Long userKeywordId = 999L;

        when(userKeywordRepository.findById(userKeywordId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> keywordService.deleteKeyword(userId, userKeywordId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.KEYWORD_NOT_FOUND);
    }

    @Test
    @DisplayName("deleteKeyword_권한없음_에러")
    void deleteKeyword_forbidden_error() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long userKeywordId = 100L;
        UserKeywordEntity entity = createUserKeywordEntity(userKeywordId, otherUserId, 10L, "소금빵");

        when(userKeywordRepository.findById(userKeywordId)).thenReturn(Optional.of(entity));

        // when & then
        assertThatThrownBy(() -> keywordService.deleteKeyword(userId, userKeywordId))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.KEYWORD_FORBIDDEN);
    }

    @Test
    @DisplayName("normalise_영어소문자변환")
    void normalise_lowercaseConversion() {
        // given — normalise는 private이므로 createKeyword를 통해 간접 테스트
        Long userId = 1L;
        KeywordCreateRequest request = new KeywordCreateRequest("Hello");
        KeywordEntity keywordEntity = createKeywordEntity(10L, "hello");

        // normalise("Hello") → "hello" 이므로 findByNormalisedText("hello")가 호출되어야 함
        when(keywordRepository.findByNormalisedText("hello")).thenReturn(Optional.of(keywordEntity));
        when(userKeywordRepository.existsByUserIdAndKeywordId(userId, 10L)).thenReturn(false);
        when(userKeywordRepository.countByUserIdWithLock(userId)).thenReturn(0L);

        // when
        keywordService.createKeyword(userId, request);

        // then — "hello"로 정규화되어 조회되었는지 검증
        verify(keywordRepository).findByNormalisedText(eq("hello"));
        verify(keywordRepository, never()).findByNormalisedText(eq("Hello"));
    }

    // ===== 헬퍼 메서드 =====

    private KeywordEntity createKeywordEntity(Long id, String normalisedText) {
        KeywordEntity entity = KeywordEntity.builder().normalisedText(normalisedText).build();
        setId(entity, id);
        return entity;
    }

    private UserKeywordEntity createUserKeywordEntity(Long id, Long userId, Long keywordId, String displayText) {
        UserKeywordEntity entity = UserKeywordEntity.builder()
                .userId(userId).keywordId(keywordId).displayText(displayText).build();
        setId(entity, id);
        return entity;
    }

    private void setId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
