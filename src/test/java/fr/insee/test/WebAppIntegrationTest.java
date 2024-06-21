package fr.insee.test;

import fr.insee.boot.PropertiesLogger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WebAppIntegrationTest.class, properties = {
        "properties.logger.sources-ignored = systemEnvironment",
        "properties.logger.prefix-for-properties = debug, info, logging, spring, server, management, properties, springdoc, fr"
})
@Configuration
class WebAppIntegrationTest {

    @Test
    void contextLoads() {
        assertThat(((Slf4jStub)LoggerFactory.getLogger(PropertiesLogger.class)).getStringBuilder().toString()).hasToString(
                """
                       [INFO]\s
                       ================================================================================
                                               Values of properties from sources :
                       - Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'
                       - Inlined Test Properties
                       - systemProperties
                                                            ====
                       spring.jmx.enabled = false
                       properties.logger.sources-ignored = systemEnvironment
                       properties.logger.prefix-for-properties = debug, info, logging, spring, server, management, properties, springdoc, fr
                       fr.insee.test = ok
                       fr.insee.secret = ******
                       ================================================================================
                       """);
    }

}
