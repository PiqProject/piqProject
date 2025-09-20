package piq.piqproject.infra.s3;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import piq.piqproject.common.file.FileUploader;

//TODO: S3 SDK 의존성 추가 및 업로드 로직 구현해야함 /현재는 임시로 만든 클래스
// @Component
@Profile("prod") // prod 프로필일 때만 활성화
public class S3Uploader implements FileUploader {

    // ... 이전 답변의 S3 클라이언트 및 업로드 로직 ...

    @Override
    public String upload(MultipartFile file, String fullPath) {
        // S3에 fullPath를 key로 하여 파일을 업로드하고,
        // 최종적으로 접근 가능한 URL을 반환하는 로직 구현
        // return amazonS3Client.getUrl(bucket, fullPath).toString();
        return "S3_UPLOADED_URL/" + fullPath; // 예시 URL
    }

    @Override
    public void delete(String fileUrl) {
        try {
            // S3에 저장된 파일의 key는 전체 URL이 아니라, URL에서 도메인 부분을 제외한 경로입니다.
            String fileKey = fileUrl.substring(fileUrl.indexOf("images/"));
            // amazonS3Client.deleteObject(bucket, fileKey);
        } catch (Exception e) {
            System.err.println("S3 파일 삭제 실패: " + fileUrl);
        }
    }
}