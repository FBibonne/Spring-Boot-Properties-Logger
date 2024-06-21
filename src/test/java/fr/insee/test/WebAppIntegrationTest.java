package fr.insee.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import static fr.insee.boot.PropertiesLogger.MASK;
import static fr.insee.boot.PropertiesLogger.SEPARATION_LINE;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WebAppIntegrationTest.class, properties = {
        "properties.logger.sources-ignored = systemEnvironment",
        "properties.logger.prefix-for-properties = debug, info, logging, spring, server, management, properties, springdoc, fr"
})
@Configuration
class WebAppIntegrationTest {

    private final LocalLoggerStub localLogger = new LocalLoggerStub();

    @Test
    void contextLoads() {
        String logs = localLogger.logs().toString();
        assertThat(logs).contains(
                SEPARATION_LINE+
                """
                                        Values of properties from sources :
                - Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'
                - Inlined Test Properties
                - systemProperties
                                                     ====
                spring.jmx.enabled = false
                properties.logger.sources-ignored = systemEnvironment
                properties.logger.prefix-for-properties = debug, info, logging, spring, server, management, properties, springdoc, fr
                fr.insee.test = ok
                fr.insee.secret =\s
                """ +MASK);
    }

}
