package io.github.fbibonne.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;


@ExtendWith(OutputCaptureExtension.class)
class ExternalPropertiesFilesIntegrationTest {

    @Test
    void startingSpringBootApplicationWithExternalOptionalPropertiesFilesAndUnexistingPlaceholder_shouldNotFail(CapturedOutput capturedOutput) throws IOException {
        final var contextRef = new Object(){
            ApplicationContext context;
        };
        final Path nonExcludedPath = Path.of((new ClassPathResource("/nonExcluded")).getURI());
        final Path ignoredPath = Path.of((new ClassPathResource("/ignored")).getURI());
        assertThat(System.getProperty("unexisting")).isNull();
        assertThat(System.getenv("unexisting")).isNull();
        System.setProperty("existing", nonExcludedPath.toString());
        assertThatCode(() -> contextRef.context=SpringApplication.run(VerySimpleSpringBootApplication.class, "--spring.config.location="+
                "optional:file:/${unexisting}/application.properties,"+
                "optional:file:/unexisting/path/to/property/file/application.properties,"+
                "optional:classpath:${unexisting:/otherProps}/application.properties,"+
                "optional:file:${existing}/application.properties," +
                "optional:file:${existing:"+ ignoredPath +"}/application.properties",
                "--properties.logger.prefix-for-properties=info,logging,spring,server,management,properties,springdoc,io")).doesNotThrowAnyException();
        Environment environment = contextRef.context.getEnvironment();
        assertThatCode(()->environment.getProperty("spring.config.location")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Could not resolve placeholder 'unexisting'");
        assertThat(capturedOutput.toString()).contains("spring.config.location = optional:file:/${unexisting}/application.properties");
        //otherProps/application.properties
        /*
         * io.github.fbibonne.shared = additionalPropsInClasspath
         * io.github.fbibonne.specific.additionalPropsInClasspath = additionalPropsInClasspath
         */
        assertThat(environment.getProperty("io.github.fbibonne.specific.additionalPropsInClasspath")).hasToString("additionalPropsInClasspath");
        assertThat(capturedOutput.toString()).contains("io.github.fbibonne.specific.additionalPropsInClasspath = additionalPropsInClasspath");
        // /${existing}/application.properties => nonExcluded/application.properties
        /*
         * io.github.fbibonne.shared = nonExcludedFile
         * io.github.fbibonne.specific.nonExcludedFile = nonExcludedFile
         * io.github.fbibonne.sharedWithExternal = nonExcludedFile
         */
        assertThat(environment.getProperty("io.github.fbibonne.specific.nonExcludedFile")).hasToString("nonExcludedFile");
        assertThat(capturedOutput.toString()).contains("io.github.fbibonne.specific.nonExcludedFile = nonExcludedFile");
        // /${existing:"+ ignoredPath +"}/application.properties => not /ignored/application.properties
        assertThat(environment.getProperty("io.github.fbibonne.specific.ignoredFile")).isNull();
        assertThat(capturedOutput.toString()).doesNotContain("io.github.fbibonne.specific.ignoredFile");
    }

    @Configuration
    static class VerySimpleSpringBootApplication {}

}
