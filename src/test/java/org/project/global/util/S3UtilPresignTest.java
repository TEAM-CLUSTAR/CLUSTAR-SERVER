package org.project.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.domain.memo.dto.response.MemoPresignedUrlResponse;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * presigned PUT URL은 Content-Type을 서명에 포함하므로, 클라이언트가 그 값을 그대로
 * 보내지 않으면 S3가 403(SignatureDoesNotMatch)을 준다. 따라서 응답의 contentType이
 * 실제로 서명된 값과 일치해야 한다. presign은 네트워크 없이 로컬 계산이라 테스트 가능하다.
 */
@DisplayName("Presigned PUT URL 서명 테스트")
class S3UtilPresignTest {

    private final S3Util s3Util = newS3Util();

    private S3Util newS3Util() {
        S3Presigner presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("AKIAEXAMPLEEXAMPLE", "secretsecretsecret")))
                .build();

        S3Util util = new S3Util(null, presigner);
        ReflectionTestUtils.setField(util, "bucket", "clustar-bucket-01");
        return util;
    }

    private String signedHeadersOf(String url) {
        String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
        int start = decoded.indexOf("X-Amz-SignedHeaders=") + "X-Amz-SignedHeaders=".length();
        int end = decoded.indexOf('&', start);
        return end == -1 ? decoded.substring(start) : decoded.substring(start, end);
    }

    @Test
    @DisplayName("응답의 contentType은 URL에 실제로 서명된 값과 일치한다")
    void createPresignedPutUrl_contentTypeMatchesSignature() {
        // given: 서버 매핑에 없는 확장자 — 이때가 클라이언트 판단값과 어긋나기 쉽다
        MemoPresignedUrlResponse.PresignedUrlResponse response =
                s3Util.createPresignedPutUrl(1L, "memo-file", "hwp", 102_400L, 0);

        // then: content-type이 서명 대상에 포함되어 있고(= 클라가 반드시 맞춰 보내야 하고)
        assertThat(signedHeadersOf(response.presignedUrl())).contains("content-type");

        // 응답이 그 값을 그대로 알려준다
        assertThat(response.contentType()).isEqualTo("application/octet-stream");
        assertThat(response.s3Key()).startsWith("memo-file/1/").endsWith(".hwp");
    }

    @Test
    @DisplayName("서버 매핑에 있는 확장자는 해당 MIME 타입을 내려준다")
    void createPresignedPutUrl_knownExtension() {
        assertThat(s3Util.createPresignedPutUrl(1L, "memo-image", "png", 1L, 0).contentType())
                .isEqualTo("image/png");
        assertThat(s3Util.createPresignedPutUrl(1L, "memo-file", "pdf", 1L, 0).contentType())
                .isEqualTo("application/pdf");
    }
}
