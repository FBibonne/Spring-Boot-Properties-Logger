package spring.concerns;

import fr.insee.boot.PropertiesLogger;
import fr.insee.test.Slf4jStub;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AdditionalLocationWithDynamicPropertySourcesTest.class ,properties = {
        "spring.config.import=",
        "properties.logger.sources-ignored="
}
)
@Configuration
/*
 * this test checks if `spring.config.additional-location`  is taken into account with @DynamicPropertySource
 *
 * As @DynamicPropertySource are processed after Spring Boot has prepared environment, `spring.config.additional-location`
 * are "ignored".
 *
 * This test checks if it is still the case for the current version.
 */
class AdditionalLocationWithDynamicPropertySourcesTest {

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.config.additional-location", () -> "classpath:/spring/concerns/");
    }

    @Test
    void propertyFromAddtionalLocationShouldBeLoaded(@Autowired Environment environment) {
        assertThat(environment.getProperty("spring.config.additional-location")).hasToString("classpath:/spring/concerns/");
        assertThat(environment.getProperty("property.in.addtional.file")).isNull();
    }

    @AfterAll
    static void clearLogStub(){
        ((Slf4jStub) LoggerFactory.getLogger(PropertiesLogger.class)).getStringBuilder().setLength(0);
    }

}
