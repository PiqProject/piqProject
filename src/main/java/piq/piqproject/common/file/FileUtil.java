package piq.piqproject.common.file;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileUtil {

    /**
     * 날짜 기반의 디렉토리 경로를 생성합니다. (예: images/2025/09/17)
     */
    public String createDirectoryPath() {
        LocalDate now = LocalDate.now();
        return "images/" + now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    }

    /**
     * UUID를 이용하여 고유한 파일 이름을 생성하고, 원본 확장자를 유지합니다.
     */
    public String createUniqueFileName(String originalFilename) {
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * 전달된 파일이 이미지 파일인지 검증합니다.
     * 
     * @param file 검증할 MultipartFile
     * @throws IllegalArgumentException 이미지 파일이 아닐 경우
     */
    public boolean isImageFile(MultipartFile file) {
        // file이 null이거나 비어있는 경우 false 반환
        if (file == null || file.isEmpty())
            return false;

        // File의 Content-Type이 "image/"로 시작하는지 확인
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image");
    }

}
