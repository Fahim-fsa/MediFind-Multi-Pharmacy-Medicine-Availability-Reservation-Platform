package com.medifind;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * SLP: Core Platform & Shared Engine → "Set up 3-tier project skeleton"
 *
 */
@SpringBootTest
@ActiveProfiles("test")
class MedifindApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty — Spring Boot fails this test automatically
        // if the ApplicationContext cannot be created.
    }
}
