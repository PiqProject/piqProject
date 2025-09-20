package piq.piqproject.infra.local;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.common.file.FileUploader;
import java.io.File;

@Component
@Slf4j
// @Profile("local") // local 프로필일 때만 활성화
public class LocalUploader implements FileUploader {

    private final String uploadDir = "C:/uploads/"; // 실제 저장될 로컬 경로

    /**
     * 파일을 로컬 디스크에 저장하고, 해당 파일의 절대경로를 반환한다.
     * 
     * @param file         업로드할 파일
     * @param relativePath 파일을 저장할 상대경로 (예: /images/2025/09/17/~~~~.jpg)
     * @return 파일의 절대 URL (예: /uploads/images/2025/
     */
    @Override
    public String upload(MultipartFile file, String relativePath) {
        try {
            File targetFile = new File(uploadDir + relativePath);
            File parentDir = targetFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs(); // 부모 디렉토리가 없으면 생성
            }
            file.transferTo(targetFile);
            return "/uploads/" + relativePath;
        } catch (Exception e) {
            log.error("로컬 파일 업로드 실패", e.getMessage());
            throw new InternalServerException(ErrorCode.FILE_UPLOAD_ERROR, "로컬 파일 업로드 실패");
        }
    }

    /*
     * 파일을 로컬 디스크에서 삭제한다.
     * 
     * @param fileUrl 삭제할 파일의 절대 URL (예: /uploads/images/2025/09/17/~~~~.jpg)
     * 
     * @return void
     */
    @Override
    public void delete(String fileUrl) {
        try {
            // fileUrl은 "/uploads/images/2025/..." 형태이므로, 앞의 "/uploads"를 제거해야 실제 경로와 매칭됨
            String filePath = fileUrl.replaceFirst("/uploads", "");
            File file = new File(uploadDir + filePath);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            log.error("로컬 파일 삭제 실패: " + fileUrl, e.getMessage());
            throw new InternalServerException(ErrorCode.FILE_DELETE_ERROR, "로컬 파일 삭제 실패");
        }
    }
}