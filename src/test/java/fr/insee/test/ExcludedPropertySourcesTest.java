package fr.insee.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(classes = ExcludedPropertySourcesTest.class,properties = {
        "properties.logger.sources-ignored= systemProperties, systemEnvironment,[application.properties],/secrets/secret.properties,commandLineArgs,Inlined\\ Test\\ Properties",
    "spring.config.additional-location="}
)
@Configuration
class ExcludedPropertySourcesTest {

    void contextLoad_shouldNotPrintExcludedPropertySources() {

    }

}
