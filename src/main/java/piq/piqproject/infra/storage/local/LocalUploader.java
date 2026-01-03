package piq.piqproject.infra.storage.local;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.common.file.FileUploader;
import java.io.File;

@Component
@Slf4j
@Primary
// @Profile("local") // local 프로필일 때만 활성화
public class LocalUploader implements FileUploader {

    @Value("${app.upload.dir}")
    private String uploadDir; // 실제 저장될 로컬 경로

    @Value("${app.upload.url-prefix}")
    private String uploadUrlPrefix; // 브라우저 접근용 URL 접두사

    /**
     * 파일을 로컬 디스크에 저장하고, 해당 파일의 절대경로를 반환한다.
     * 
     * @param file         업로드할 파일
     * @param relativePath 파일을 저장할 상대경로 (예: /images/2025/09/17/~~~~.jpg)
     * @return 브라우저에서 접근가능한 url (예: /uploads/images/2025/)
     */
    @Override
    public String upload(MultipartFile file, String relativePath) {
        try {
            File targetFile = new File(uploadDir + relativePath);
            File parentDir = targetFile.getParentFile(); // 부모 디렉토리의 절대경로를 반환함
            if (!parentDir.exists()) {
                parentDir.mkdirs(); // 부모 디렉토리가 없으면 생성
            }
            file.transferTo(targetFile);
            return uploadUrlPrefix + relativePath;
        } catch (Exception e) {
            log.error("로컬 파일 업로드 실패", e.getMessage());
            throw new InternalServerException(ErrorCode.FILE_UPLOAD_ERROR, "로컬 파일 업로드 실패");
        }
    }

    /**
     * [서버 내부 파일 전용] File 객체를 로컬 디스크에 저장한다.
     */
    @Override
    public String upload(File file, String relativePath) {
        try {
            File targetFile = new File(uploadDir + relativePath);
            File parentDir = targetFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 기존 파일 복사
            java.nio.file.Files.copy(file.toPath(), targetFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            return uploadUrlPrefix + relativePath;
        } catch (Exception e) {
            log.error("로컬 파일 업로드 실패 (File)", e);
            throw new InternalServerException(ErrorCode.FILE_UPLOAD_ERROR, "로컬 파일 업로드 실패");
        }
    }

    /*
     * 파일을 로컬 디스크에서 삭제한다.
     * 
     * @param fileUrl 브라우저에서 접근가능한 url (예: /uploads/images/2025/09/17/~~~~.jpg)
     * 
     * @return void
     */
    @Override
    public void delete(String fileUrl) {
        try {
            // fileUrl의 접두사를 제거해야 실제 파일 경로(relative Path)와 매칭됨
            String relativePath = fileUrl.replaceFirst(uploadUrlPrefix, "");
            File file = new File(uploadDir + relativePath);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            log.error("로컬 파일 삭제 실패: {}", fileUrl);
        }
    }
}