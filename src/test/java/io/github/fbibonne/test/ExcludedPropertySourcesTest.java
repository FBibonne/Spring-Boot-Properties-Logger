package io.github.fbibonne.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ExcludedPropertySourcesTest.class, properties = {
        "io.github.fbibonne.shared = inlineTestProperties",
        "io.github.fbibonne.specific.inlineTestProperties = inlineTestProperties"},
        args = { "--io.github.fbibonne.shared=args", "--io.github.fbibonne.specific.args=args","--spring.config.additional-location=classpath:/otherProps/,classpath:/nonExcluded/"}
)
@Configuration
@ExtendWith(OutputCaptureExtension.class)
class ExcludedPropertySourcesTest {

    static Path externalPropertiesPath;

    @BeforeAll
    static void createTempPropertyFileInFileSystem() throws IOException {
        externalPropertiesPath = Files.createTempFile("inFileSystem", ".properties");
        String content = """
                io.github.fbibonne.shared = inFileSystem
                io.github.fbibonne.specific.inFileSystem = inFileSystem
                io.github.fbibonne.sharedWithExternal = inFileSystem
                properties.logger.sources-ignored = systemProperties, systemEnvironment,[application.properties],[otherProps/application.properties],commandLineArgs,Inlined\\ Test\\ Properties,["""
                + externalPropertiesPath.toAbsolutePath()+"]";
        Files.writeString(externalPropertiesPath, content);

        System.setProperty("spring.config.import", "file:" + externalPropertiesPath.toAbsolutePath());
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("io.github.fbibonne.shared", ()->"dynamicPropertySource");
        registry.add("io.github.fbibonne.specific.dynamicPropertySource",()->"dynamicPropertySource");
    }


    @Test
    void contextLoad_shouldNotPrintExcludedPropertySources(@Autowired Environment environment, CapturedOutput output) {

        //GIVEN context and prop properties.logger.sources-ignored= systemProperties, systemEnvironment,[application.properties],otherProps/application.properties,commandLineArgs,Inlined\ Test\ Properties
        //not excluded : nonExcluded.properties,
        //WHEN context loads
        //THEN
        String logOutput = output.toString();

        assertThat(environment.getProperty("properties.logger.sources-ignored")).hasToString("systemProperties, systemEnvironment,[application.properties],[otherProps/application.properties],commandLineArgs,Inlined Test Properties,["+ externalPropertiesPath.toAbsolutePath()+"]");
        assertThat(environment.getProperty("io.github.fbibonne.shared")).hasToString("dynamicPropertySource");
        assertThat(environment.getProperty("io.github.fbibonne.specific.nonExcludedFile")).hasToString("nonExcludedFile");
        assertThat(logOutput).contains("io.github.fbibonne.shared = inlineTestProperties")
                .doesNotContain("io.github.fbibonne.specific.dynamicPropertySource")
                .contains("io.github.fbibonne.specific.nonExcludedFile = nonExcludedFile")
                .doesNotContain("io.github.fbibonne.sharedWithExternal = nonExcludedFile")
                .contains("io.github.fbibonne.sharedWithExternal = inFileSystem")
                .doesNotContain("io.github.fbibonne.specific.inFileSystem = inFileSystem")
                .doesNotContain("io.github.fbibonne.specific.applicationproperties")
                .doesNotContain("io.github.fbibonne.specific.additionalPropsInClasspath")
                .doesNotContain("io.github.fbibonne.specific.args")
                .doesNotContain("io.github.fbibonne.specific.inFileSystem")
                .doesNotContain("io.github.fbibonne.specific.inlineTestProperties")
                .doesNotContain("class path resource [application.properties]")
                .doesNotContain("class path resource [otherProps/application.properties]")
                .doesNotContain("- commandLineArgs")
                .doesNotContain("Config resource 'file ["+externalPropertiesPath+"]'")
                .doesNotContain("- Inlined Test Properties")
                .doesNotContain("- systemProperties")
                .doesNotContain("- systemEnvironment")
                .doesNotContain("class path resource [application.properties]")
        ;
        assertThat(environment.getProperty("io.github.fbibonne.specific.inFileSystem")).hasToString("inFileSystem");
        assertThat(environment.getProperty("io.github.fbibonne.specific.applicationproperties")).hasToString("application.properties");
        assertThat(environment.getProperty("io.github.fbibonne.specific.additionalPropsInClasspath")).hasToString("additionalPropsInClasspath");
        assertThat(environment.getProperty("io.github.fbibonne.specific.args")).hasToString("args");

    }

    @AfterAll
    static void deleteTempPropertyFileInFileSystem() throws IOException {
        Files.delete(externalPropertiesPath);
        System.clearProperty("spring.config.import");
    }

}
