package fr.insee.test;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ExcludedPropertySourcesTest.class, properties = {
        "fr.insee.shared = inlineTestProperties",
        "fr.insee.specific.inlineTestProperties = inlineTestProperties"},
        args = { "--fr.insee.shared=args", "--fr.insee.specific.args=args","--spring.config.additional-location=classpath:/otherProps/,classpath:/nonExcluded/"}
)
@Configuration
@ExtendWith(OutputCaptureExtension.class)
class ExcludedPropertySourcesTest {

    static Path externalPropertiesPath;

    @BeforeAll
    static void createTempPropertyFileInFileSystem() throws IOException {
        externalPropertiesPath = Files.createTempFile("inFileSystem", ".properties");
        String content = """
                fr.insee.shared = inFileSystem
                fr.insee.specific.inFileSystem = inFileSystem
                fr.insee.sharedWithExternal = inFileSystem
                properties.logger.sources-ignored = systemProperties, systemEnvironment,[application.properties],[otherProps/application.properties],commandLineArgs,Inlined\\ Test\\ Properties,["""
                + externalPropertiesPath.toAbsolutePath()+"]";
        Files.writeString(externalPropertiesPath, content);

        System.setProperty("spring.config.import", "file:" + externalPropertiesPath.toAbsolutePath());
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("fr.insee.shared", ()->"dynamicPropertySource");
        registry.add("fr.insee.specific.dynamicPropertySource",()->"dynamicPropertySource");
    }


    @Test
    void contextLoad_shouldNotPrintExcludedPropertySources(@Autowired Environment environment, CapturedOutput output) {

        //GIVEN context and prop properties.logger.sources-ignored= systemProperties, systemEnvironment,[application.properties],otherProps/application.properties,commandLineArgs,Inlined\ Test\ Properties
        //not excluded : nonExcluded.properties,
        //WHEN context loads
        //THEN
        String logOutput = output.toString();

        assertThat(environment.getProperty("properties.logger.sources-ignored")).hasToString("systemProperties, systemEnvironment,[application.properties],[otherProps/application.properties],commandLineArgs,Inlined Test Properties,["+ externalPropertiesPath.toAbsolutePath()+"]");
        assertThat(environment.getProperty("fr.insee.shared")).hasToString("dynamicPropertySource");
        assertThat(environment.getProperty("fr.insee.specific.nonExcludedFile")).hasToString("nonExcludedFile");
        assertThat(logOutput).contains("fr.insee.shared = inlineTestProperties")
                .doesNotContain("fr.insee.specific.dynamicPropertySource")
                .contains("fr.insee.specific.nonExcludedFile = nonExcludedFile")
                .doesNotContain("fr.insee.sharedWithExternal = nonExcludedFile")
                .contains("fr.insee.sharedWithExternal = inFileSystem")
                .doesNotContain("fr.insee.specific.inFileSystem = inFileSystem")
                .doesNotContain("fr.insee.specific.applicationproperties")
                .doesNotContain("fr.insee.specific.additionalPropsInClasspath")
                .doesNotContain("fr.insee.specific.args")
                .doesNotContain("fr.insee.specific.inFileSystem")
                .doesNotContain("fr.insee.specific.inlineTestProperties")
                .doesNotContain("class path resource [application.properties]")
                .doesNotContain("class path resource [otherProps/application.properties]")
                .doesNotContain("- commandLineArgs")
                .doesNotContain("Config resource 'file ["+externalPropertiesPath+"]'")
                .doesNotContain("- Inlined Test Properties")
                .doesNotContain("- systemProperties")
                .doesNotContain("- systemEnvironment")
                .doesNotContain("class path resource [application.properties]")
        ;
        assertThat(environment.getProperty("fr.insee.specific.inFileSystem")).hasToString("inFileSystem");
        assertThat(environment.getProperty("fr.insee.specific.applicationproperties")).hasToString("application.properties");
        assertThat(environment.getProperty("fr.insee.specific.additionalPropsInClasspath")).hasToString("additionalPropsInClasspath");
        assertThat(environment.getProperty("fr.insee.specific.args")).hasToString("args");

    }

    @AfterAll
    static void deleteTempPropertyFileInFileSystem() throws IOException {
        Files.delete(externalPropertiesPath);
        System.clearProperty("spring.config.import");
    }

}
