package com.todaybread.server.domain.store.service;

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
import java.nio.file.StandardCopyOption;

/**
 * FileStorageService의 로컬 파일 시스템 구현체.
 * EC2 인스턴스 내부 디스크에 파일을 저장한다.
 */
@Service
@Primary
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path uploadDir;

    public LocalFileStorageService(@Value("${app.file.upload-dir}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, Long storeId, int displayOrder) {
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String storedFilename = "store_" + storeId + "_" + displayOrder + "." + extension;

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

    @Override
    public void delete(String storedFilename) {
        try {
            Path filePath = uploadDir.resolve(storedFilename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", storedFilename, e);
        }
    }

    @Override
    public String getFileUrl(String storedFilename) {
        return "/images/" + storedFilename;
    }
}
