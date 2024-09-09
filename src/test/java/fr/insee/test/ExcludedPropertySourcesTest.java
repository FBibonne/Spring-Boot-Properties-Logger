package fr.insee.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest(classes = ExcludedPropertySourcesTest.class,properties = {
        "properties.logger.sources-ignored= systemProperties, systemEnvironment,[application.properties],/secrets/secret.properties,commandLineArgs,Inlined\\ Test\\ Properties",
    "spring.config.additional-location="}
)
@Configuration
class ExcludedPropertySourcesTest {

    static Path externalPropertiesPath;

    @BeforeAll
    static void createTempPropertyFileInFileSystem() throws IOException {
        externalPropertiesPath=Files.createTempFile("inFileSystem", "properties");
        fr.insee.shared = inFileSystem
        fr.insee.specific.inFileSystem = inFileSystem
        fr.insee.sharedWithExternal = inFileSystem
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.config.additional-location", () -> "file:"+externalPropertiesPath.toAbsolutePath());
    }


    void contextLoad_shouldNotPrintExcludedPropertySources() {

        non exclu:
        props internes

        exclus:
        application.properties
       classpath:otherProps/additionalPropsInClasspath.properties
        commandLineArgs
        Inlined\ Test\ Properties
                externalPropertiesPath
        @DynamicPropertySource


    }

    @AfterAll
    static void deleteTempPropertyFileInFileSystem() throws IOException {
        Files.delete(externalPropertiesPath);
    }

}
