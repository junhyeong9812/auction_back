package com.auction.back.global.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Slf4j
public class FileStorageUtils {

    /**
     * basePath: 예) "src/main/resources/images/auction"
     * filename: 예) "Auction_12.jpg"
     * file: 업로드된 멀티파트 파일
     */
    public static void storeFile(String basePath, String filename, MultipartFile file) throws IOException {
        File dir = new File(basePath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs(); // 디렉터리 없으면 생성
            if (!created) {
                log.warn("디렉토리 생성 실패 또는 이미 존재: {}", basePath);
            }
        }

        File dest = new File(dir, filename);
        file.transferTo(dest);
        log.info("파일 저장: {}", dest.getAbsolutePath());
    }

    /**
     * basePath + filename 에 해당하는 파일을 삭제
     */
    public static boolean deleteFile(String basePath, String filename) {
        File file = new File(basePath, filename);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                log.info("파일 삭제 성공: {}", file.getAbsolutePath());
                return true;
            } else {
                log.warn("파일 삭제 실패: {}", file.getAbsolutePath());
            }
        } else {
            log.warn("파일이 존재하지 않습니다: {}", file.getAbsolutePath());
        }
        return false;
    }
}
