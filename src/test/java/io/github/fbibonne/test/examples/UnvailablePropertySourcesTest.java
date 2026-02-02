package io.github.fbibonne.test.examples;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = UnvailablePropertySourcesTest.class)
@Configuration
@ExtendWith(OutputCaptureExtension.class)
@PropertySource("classpath:/unavailable/application.properties")
class UnvailablePropertySourcesTest {

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("io.github.fbibonne.dynamic-property-source", () -> "ok");
    }

    @Test
    void propertiesFromLateSourcesShouldNotBeDisplayedButShouldBeAvailableInContext(@Autowired Environment environment, CapturedOutput capturedOutput) {
        assertThat(capturedOutput.toString()).doesNotContain("io.github.fbibonne.dynamic-property-source", "io.github.fbibonne.property-source-annotation");
        assertThat(environment.getProperty("io.github.fbibonne.dynamic-property-source")).hasToString("ok");
        assertThat(environment.getProperty("io.github.fbibonne.property-source-annotation")).hasToString("ok");
    }

}
