package fr.insee.test;

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
        assertThatCode(() -> {
            contextRef.context=SpringApplication.run(VerySimpleSpringBootApplication.class, "--spring.config.location="+
                    "optional:file:/${unexisting}/application.properties,"+
                    "optional:file:/unexisting/path/to/property/file/application.properties,"+
                    "optional:classpath:${unexisting:/otherProps}/application.properties,"+
                    "optional:file:${existing}/application.properties," +
                    "optional:file:${existing:"+ ignoredPath +"}/application.properties",
                    "--properties.logger.disabled=true",
                    "--properties.logger.prefix-for-properties=info,logging,spring,server,management,properties,springdoc,fr");
        }).doesNotThrowAnyException();
        Environment environment = contextRef.context.getEnvironment();
        assertThatCode(()->environment.getProperty("spring.config.location")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Could not resolve placeholder 'unexisting'");
        assertThat(capturedOutput.toString()).contains("spring.config.location=optional:file:/${unexisting}/application.properties");
        //otherProps/application.properties
        /*
         * fr.insee.shared = additionalPropsInClasspath
         * fr.insee.specific.additionalPropsInClasspath = additionalPropsInClasspath
         */
        assertThat(environment.getProperty("fr.insee.specific.additionalPropsInClasspath")).hasToString("additionalPropsInClasspath");
        assertThat(capturedOutput.toString()).contains("fr.insee.specific.additionalPropsInClasspath = additionalPropsInClasspath");
        // /${existing}/application.properties => nonExcluded/application.properties
        /*
         * fr.insee.shared = nonExcludedFile
         * fr.insee.specific.nonExcludedFile = nonExcludedFile
         * fr.insee.sharedWithExternal = nonExcludedFile
         */
        assertThat(environment.getProperty("fr.insee.specific.nonExcludedFile")).hasToString("nonExcludedFile");
        assertThat(capturedOutput.toString()).contains("fr.insee.specific.nonExcludedFile = nonExcludedFile");
        // /${existing:"+ ignoredPath +"}/application.properties => not /ignored/application.properties
        assertThat(environment.getProperty("fr.insee.specific.ignoredFile")).isNull();
        assertThat(capturedOutput.toString()).doesNotContain("fr.insee.specific.ignoredFile");
    }

    @Configuration
    static class VerySimpleSpringBootApplication {}

}
