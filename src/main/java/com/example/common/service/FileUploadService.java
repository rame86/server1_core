package com.example.common.service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileUploadService {

    @Value("${file.upload.dir}")
    private String fileUploadDir;

    /**
     * MultipartFile을 지정된 하위 폴더에 저장하고 접근 가능한 URL을 반환한다.
     * @param file 업로드할 파일
     * @param subDir 하위 폴더명 (예: "profile" 또는 "board" 또는 "artist")
     * @return 접근 가능한 이미지 URL 경로 (예: "/images/core/profile/UUID.png")
     */
    public String uploadImage(MultipartFile file, String subDir) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("업로드된 파일이 없습니다.");
        }

        try {
            // 원본 파일명에서 확장자 추출
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 고유한 파일명 생성 (UUID 활용)
            String savedFilename = UUID.randomUUID().toString() + extension;

            // 저장할 기본 경로 = fileUploadDir + (필요시 "/") + subDir
            String uploadPath = fileUploadDir;
            if (!uploadPath.endsWith("/") && !uploadPath.endsWith("\\")) {
                uploadPath += "/";
            }
            uploadPath += subDir;

            // 디렉토리가 없다면 생성
            File uploadDirectory = new File(uploadPath);
            if (!uploadDirectory.exists()) {
                boolean created = uploadDirectory.mkdirs();
                if (created) {
                    log.info("디렉토리 생성 완료: {}", uploadPath);
                } else {
                    log.error("디렉토리 생성 실패: {}", uploadPath);
                    throw new RuntimeException("파일 저장 디렉토리를 생성할 수 없습니다.");
                }
            }

            // 파일 저장
            File dest = new File(uploadDirectory, savedFilename);
            file.transferTo(dest);
            log.info("파일 저장 성공: {}, 경로: {}", savedFilename, dest.getAbsolutePath());

            // Nginx 또는 WebConfig 리소스 매핑 규칙에 따라 URL 생성
            // "/images/core/" 하위로 매핑됨
            return "/images/core/" + subDir + "/" + savedFilename;

        } catch (IOException e) {
            log.error("파일 저장 과정에서 에러 발생", e);
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.");
        }
    }
}
