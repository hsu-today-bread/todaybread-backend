package com.todaybread.server.global.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장소 추상화 인터페이스입니다.
 * 가게 이미지 저장/삭제/조회 URL 생성 책임을 캡슐화합니다.
 * 현재는 로컬 디스크 구현체를 사용하지만, 이후 S3 같은 외부 스토리지로
 * 전환하더라도 상위 서비스는 이 인터페이스만 의존하도록 하기 위한 목적입니다.
 */
public interface FileStorage {

    /**
     * 업로드된 파일을 실제 저장소에 저장합니다.
     * 구현체는 저장 위치에 맞는 방식으로 파일을 저장한 뒤, 내부 식별자로 사용할
     * 저장 파일명을 반환해야 합니다. 현재 규칙은
     * {@code store_{storeId}_{displayOrder}.{확장자}} 형식입니다.
     *
     * @param file 업로드된 원본 파일
     * @param storeId 가게 ID
     * @param displayOrder 이미지 표시 순서 (0 = 대표 이미지)
     * @return 저장소 내부에서 관리할 저장 파일명
     */
    String store(MultipartFile file, Long storeId, int displayOrder);

    /**
     * 저장소에 있는 파일을 삭제합니다.
     * DB 레코드 삭제와는 별개로 실제 파일 리소스를 정리하는 용도입니다.
     *
     * @param storedFilename 저장소 내부 저장 파일명
     */
    void delete(String storedFilename);

    /**
     * 저장된 파일의 접근 URL을 생성합니다.
     * 로컬 저장소는 서버의 정적 리소스 경로를, S3 구현체는 버킷 또는 CDN URL을
     * 반환하는 식으로 구현체마다 달라질 수 있습니다.
     *
     * @param storedFilename 저장소 내부 저장 파일명
     * @return 클라이언트가 사용할 파일 접근 URL
     */
    String getFileUrl(String storedFilename);
}
