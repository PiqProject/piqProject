package piq.piqproject.common.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploader {
    /**
     * 파일을 업로드하고 접근 가능한 URL을 반환합니다.
     *
     * @param file     업로드할 파일
     * @param fullPath 파일이 저장될 전체 경로 (예: images/2025/09/17/uuid.jpg)
     * @return 외부에서 접근 가능한 최종 URL
     */
    String upload(MultipartFile file, String fullPath);

    /**
     * 스토리지에서 파일을 삭제합니다.
     * 
     * @param fileUrl DB에 저장된 파일의 전체 URL 또는 경로
     */
    void delete(String fileUrl);
}