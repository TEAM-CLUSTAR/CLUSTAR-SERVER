package org.project.global.config.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key:}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    // 파일 업로드 및 삭제를 위한 설정
    @Bean
    public S3Client s3Client() {
        // dev 환경 (credentials이 없으므로 EC2의 IAM 역할로)
        if (accessKey == null || accessKey.isEmpty()) {
            return S3Client.builder()
                    .region(Region.of(region))
                    .build();
        }

        // local 환경 (credentials이 없으므로 EC2의 IAM 역할로)
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }

    // presigned URL 생성용
    @Bean
    public S3Presigner s3Presigner() {
        // dev 환경
        if (accessKey == null || accessKey.isEmpty()) {
            return S3Presigner.builder()
                    .region(Region.of(region))
                    .build();
        }

        // local 환경
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }
}