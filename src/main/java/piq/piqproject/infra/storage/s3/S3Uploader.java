package piq.piqproject.infra.storage.s3;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.common.file.FileUploader;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class S3Uploader implements FileUploader {

    private final S3Client s3Client;

    @Value("${cloud.s3.bucket}")
    private String bucket;

    /**
     * MultipartFile을 S3에 업로드
     * 
     * @param file MultipartFile
     * @param key  S3 키(파일이름(경로))
     * @return 업로드된 S3 키
     */
    @Override
    public String upload(MultipartFile file, String key) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // 업로드된 S3 키를 반환
            return key;
        } catch (IOException e) {
            log.error("S3 파일 업로드 실패 (MultipartFile): {}", key, e);
            throw new InternalServerException(ErrorCode.FILE_UPLOAD_ERROR, "S3 파일 업로드 실패");
        }
    }

    /**
     * [서버 내부 파일 전용] File 객체를 S3에 업로드
     * 
     * @param file File 객체
     * @param key  S3 키(파일이름(경로))
     * @return 업로드된 S3 키
     */
    @Override
    public String upload(File file, String key) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
            return key;
        } catch (Exception e) {
            log.error("S3 파일 업로드 실패 (File): {}", key, e);
            throw new InternalServerException(ErrorCode.FILE_UPLOAD_ERROR, "S3 파일 업로드 실패");
        }
    }

    /**
     * S3에서 파일 삭제
     * 
     * @param key S3 키(파일이름(경로))
     */
    @Override
    public void delete(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", key, e);
        }
    }
}