package org.project;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.global.config.vectorstore.TestVectorStoreConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


@ActiveProfiles("test")
@DisplayName("테스트 환경 Health Check")
@SpringBootTest
@Import(TestVectorStoreConfig.class)
class MainApplicationTest {

    @Test
    @DisplayName("테스트 환경이 정상적으로 작동하는지 확인합니다")
    void contextLoads() {
    }

}
