package io.github.fbibonne.test.examples;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WithEnvironmentVariableTest.class)
@ExtendWith(OutputCaptureExtension.class)
@Configuration
class WithEnvironmentVariableTest {

    @Test
    void shouldPickKeyFromApplicationPropertiesAndValueFromEnvironmentVariable(CapturedOutput output) {
        String logOutput = output.toString();

        assertThat(logOutput).contains("spring.datasource.username = user_prod ### FROM System Environment Property \"SPRING_DATASOURCE_USERNAME\" ###");
    }

}
