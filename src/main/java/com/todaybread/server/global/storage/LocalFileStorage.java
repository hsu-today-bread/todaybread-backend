package com.todaybread.server.global.storage;

import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@link FileStorage}의 로컬 파일 시스템 구현체입니다.
 * 업로드된 이미지를 서버 로컬 디스크에 저장하고, 정적 리소스 경로인
 * {@code /images/**} 형태의 URL을 만들어 반환합니다.
 * 운영 환경에서 S3로 전환할 경우, 이 구현체를 대체하는 새 구현체를 추가하고
 * 상위 서비스는 그대로 {@link FileStorage}만 주입받도록 유지하면 됩니다.
 */
@Service
@Primary
public class LocalFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    private final Path uploadDir;

    /**
     * 업로드 루트 디렉터리를 초기화합니다.
     *
     * @param uploadDirPath application 설정에서 주입되는 업로드 경로
     */
    public LocalFileStorage(@Value("${app.file.upload-dir}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
    }

    /**
     * 파일을 로컬 디스크에 저장합니다.
     *
     * 저장 전에 업로드 디렉터리가 없으면 생성하고, 저장 파일명은
     * {@code {domain}_{entityId}_{index}.{확장자}} 규칙으로 만듭니다.
     *
     * @param file 업로드된 원본 파일
     * @param domain 도메인 식별자 (예: "store", "bread")
     * @param entityId 엔티티 ID
     * @param index 이미지 순서 (0부터 시작)
     * @return 저장된 파일명
     * @throws CustomException 파일 저장 중 IO 오류가 발생한 경우
     */
    @Override
    public String store(MultipartFile file, String domain, Long entityId, int index) {
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String storedFilename = domain + "_" + entityId + "_" + index + "." + extension;

        try {
            Files.createDirectories(uploadDir);
            Path targetPath = uploadDir.resolve(storedFilename).normalize();
            file.transferTo(targetPath.toFile());
            return storedFilename;
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", storedFilename, e);
            throw new CustomException(ErrorCode.STORE_IMAGE_STORAGE_FAILED);
        }
    }

    /**
     * 로컬 디스크에서 파일을 삭제합니다.
     *
     * 파일이 이미 없으면 조용히 넘어가고, 삭제 중 IO 오류가 나면 로그만 남깁니다.
     *
     * @param storedFilename 삭제할 저장 파일명
     */
    @Override
    public void delete(String storedFilename) {
        try {
            Path filePath = uploadDir.resolve(storedFilename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", storedFilename, e);
        }
    }

    /**
     * 로컬 정적 리소스 접근 URL을 생성합니다.
     *
     * @param storedFilename 저장된 파일명
     * @return {@code /images/{storedFilename}} 형태의 접근 URL
     */
    @Override
    public String getFileUrl(String storedFilename) {
        return "/images/" + storedFilename;
    }
}
