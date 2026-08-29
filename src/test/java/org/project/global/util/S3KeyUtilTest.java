package org.project.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.global.exception.domainException.MemoException;
import org.project.global.exception.errorcode.MemoErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("S3KeyUtil 테스트")
class S3KeyUtilTest {

    private final S3KeyUtil s3KeyUtil = new S3KeyUtil();

    @Nested
    @DisplayName("확장자 추출")
    class ExtractExtension {

        @Test
        @DisplayName("s3Key 끝의 확장자를 소문자로 반환한다")
        void extractExtension_returnsLowerCase() {
            assertThat(s3KeyUtil.extractExtension("memo-image/1/uuid.png")).isEqualTo("png");
            assertThat(s3KeyUtil.extractExtension("memo-image/1/uuid.PNG")).isEqualTo("png");
            assertThat(s3KeyUtil.extractExtension("memo-file/12/uuid.pdf")).isEqualTo("pdf");
        }

        @Test
        @DisplayName("파일명에 점이 여러 개면 마지막 것을 확장자로 본다")
        void extractExtension_multipleDots() {
            assertThat(s3KeyUtil.extractExtension("memo-file/1/my.report.v2.pdf")).isEqualTo("pdf");
        }

        @Test
        @DisplayName("확장자가 없으면 INVALID_S3_KEY_FORMAT 예외가 발생한다")
        void extractExtension_noExtension_throws() {
            assertThatThrownBy(() -> s3KeyUtil.extractExtension("memo-image/1/uuid"))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.INVALID_S3_KEY_FORMAT);
        }

        @Test
        @DisplayName("점으로 끝나면 INVALID_S3_KEY_FORMAT 예외가 발생한다")
        void extractExtension_trailingDot_throws() {
            assertThatThrownBy(() -> s3KeyUtil.extractExtension("memo-image/1/uuid."))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.INVALID_S3_KEY_FORMAT);
        }

        @Test
        @DisplayName("경로에만 점이 있고 파일명에 없으면 INVALID_S3_KEY_FORMAT 예외가 발생한다")
        void extractExtension_dotOnlyInPath_throws() {
            assertThatThrownBy(() -> s3KeyUtil.extractExtension("memo.image/1/uuid"))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.INVALID_S3_KEY_FORMAT);
        }

        @Test
        @DisplayName("null이거나 빈 값이면 INVALID_S3_KEY_FORMAT 예외가 발생한다")
        void extractExtension_blank_throws() {
            assertThatThrownBy(() -> s3KeyUtil.extractExtension(null))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.INVALID_S3_KEY_FORMAT);
            assertThatThrownBy(() -> s3KeyUtil.extractExtension("  "))
                    .isInstanceOf(MemoException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MemoErrorCode.INVALID_S3_KEY_FORMAT);
        }
    }
}
