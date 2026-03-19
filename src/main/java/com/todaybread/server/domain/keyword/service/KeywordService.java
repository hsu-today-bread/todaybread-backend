package com.todaybread.server.domain.keyword.service;

import com.todaybread.server.domain.keyword.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 키워드 도메인 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class KeywordService {

    private final KeywordRepository keywordRepository;
}
