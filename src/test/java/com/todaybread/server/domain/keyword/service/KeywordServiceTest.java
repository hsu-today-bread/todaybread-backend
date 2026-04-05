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
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KeywordServiceTest {

    @Mock
    private KeywordRepository keywordRepository;

    @Mock
    private UserKeywordRepository userKeywordRepository;

    @InjectMocks
    private KeywordService keywordService;

    @Test
    void createKeyword_savesNewNormalisedKeyword() {
        KeywordEntity keyword = TestFixtures.keyword(10L, "sourdough");
        given(keywordRepository.findByNormalisedText("sourdough")).willReturn(Optional.empty());
        given(keywordRepository.save(any(KeywordEntity.class))).willReturn(keyword);
        given(userKeywordRepository.existsByUserIdAndKeywordId(1L, 10L)).willReturn(false);
        given(userKeywordRepository.countByUserIdWithLock(1L)).willReturn(0L);

        KeywordCreateResponse response = keywordService.createKeyword(1L, new KeywordCreateRequest("  SourDough  "));

        assertThat(response.success()).isTrue();
        verify(userKeywordRepository).save(any(UserKeywordEntity.class));
    }

    @Test
    void createKeyword_rejectsDuplicateUserKeyword() {
        KeywordEntity keyword = TestFixtures.keyword(10L, "bagel");
        given(keywordRepository.findByNormalisedText("bagel")).willReturn(Optional.of(keyword));
        given(userKeywordRepository.existsByUserIdAndKeywordId(1L, 10L)).willReturn(true);

        assertThatThrownBy(() -> keywordService.createKeyword(1L, new KeywordCreateRequest("bagel")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.KEYWORD_ALREADY_EXISTS);
    }

    @Test
    void createKeyword_retriesLookupOnIntegrityViolation() {
        KeywordEntity keyword = TestFixtures.keyword(10L, "croissant");
        given(keywordRepository.findByNormalisedText("croissant"))
                .willReturn(Optional.empty(), Optional.of(keyword));
        given(keywordRepository.save(any(KeywordEntity.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));
        given(userKeywordRepository.existsByUserIdAndKeywordId(1L, 10L)).willReturn(false);
        given(userKeywordRepository.countByUserIdWithLock(1L)).willReturn(0L);

        KeywordCreateResponse response = keywordService.createKeyword(1L, new KeywordCreateRequest("Croissant"));

        assertThat(response.success()).isTrue();
    }

    @Test
    void getMyKeywords_returnsDisplayTexts() {
        given(userKeywordRepository.findByUserId(1L)).willReturn(List.of(
                TestFixtures.userKeyword(1L, 1L, 10L, "치아바타"),
                TestFixtures.userKeyword(2L, 1L, 11L, "bagel")
        ));

        List<KeywordResponse> responses = keywordService.getMyKeywords(1L);

        assertThat(responses).extracting(KeywordResponse::displayText)
                .containsExactly("치아바타", "bagel");
    }

    @Test
    void deleteKeyword_rejectsNonOwner() {
        UserKeywordEntity userKeyword = TestFixtures.userKeyword(1L, 99L, 10L, "bagel");
        given(userKeywordRepository.findById(1L)).willReturn(Optional.of(userKeyword));

        assertThatThrownBy(() -> keywordService.deleteKeyword(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.KEYWORD_FORBIDDEN);
    }

    @Test
    void deleteKeyword_deletesOwnedKeyword() {
        UserKeywordEntity userKeyword = TestFixtures.userKeyword(1L, 1L, 10L, "bagel");
        given(userKeywordRepository.findById(1L)).willReturn(Optional.of(userKeyword));

        KeywordDeleteResponse response = keywordService.deleteKeyword(1L, 1L);

        assertThat(response.success()).isTrue();
        verify(userKeywordRepository).delete(userKeyword);
    }
}
