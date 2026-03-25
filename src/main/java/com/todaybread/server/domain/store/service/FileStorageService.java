package com.todaybread.server.domain.store.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장소 추상화 인터페이스.
 * 로컬 파일 시스템, S3 등 구현체를 교체할 수 있다.
 */
public interface FileStorageService {

    /**
     * 파일을 저장하고 저장된 파일명을 반환한다.
     * 저장 파일명은 store_{storeId}_{displayOrder}.{확장자} 형식이다.
     * @param file 업로드된 파일
     * @param storeId 가게 ID
     * @param displayOrder 표시 순서
     * @return 저장 파일명 (예: store_5_1.jpg)
     */
    String store(MultipartFile file, Long storeId, int displayOrder);

    /**
     * 저장된 파일을 삭제한다.
     * @param storedFilename 저장 파일명
     */
    void delete(String storedFilename);

    /**
     * 저장된 파일의 접근 URL을 생성한다.
     * @param storedFilename 저장 파일명
     * @return 파일 접근 URL
     */
    String getFileUrl(String storedFilename);
}
