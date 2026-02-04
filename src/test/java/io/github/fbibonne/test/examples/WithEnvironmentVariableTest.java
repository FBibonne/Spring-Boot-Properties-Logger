package io.github.fbibonne.test.examples;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;

import static io.github.fbibonne.springaddons.boot.propertieslogger.PropertiesLogger.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WithEnvironmentVariableTest.class)
@ExtendWith(OutputCaptureExtension.class)
@Configuration
class WithEnvironmentVariableTest {

    @Test
    void shouldPickKeyFromApplicationPropertiesAndValueFromEnvironmentVariable(CapturedOutput output) {
        String logOutput = output.toString();

        assertThat(logOutput).contains("spring.datasource.username"+ANSI_NORMAL_SEQUENCE+" = "+ANSI_BROWN_UNDERLINE_SEQUENCE+"user_prod"+ANSI_NORMAL_SEQUENCE+" ### "+AINSI_PURPLE_ITALIC_SEQUENCE+"FROM System Environment Property \"SPRING_DATASOURCE_USERNAME\""+ANSI_NORMAL_SEQUENCE+" ###");
    }

}
