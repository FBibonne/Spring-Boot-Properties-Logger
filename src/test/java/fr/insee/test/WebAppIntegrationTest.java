package fr.insee.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WebAppIntegrationTest.class, properties = {
        "properties.logger.sources-ignored = systemEnvironment",
        "properties.logger.prefix-for-properties = info, logging, spring, server, management, properties, springdoc, fr",
})
@Configuration
@ExtendWith(OutputCaptureExtension.class)
class WebAppIntegrationTest {

    @Test
    void contextLoads(CapturedOutput output) {
        assertThat(output.toString()).contains(("""
                main] fr.insee.boot.PropertiesLogger           : %n\
                ================================================================================
                                        Values of properties from sources :
                - Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'%n\
                - applicationInfo%n\
                - Inlined Test Properties%n\
                - systemProperties%n\
                                                     ====%n\
                spring.jmx.enabled = false%n\
                properties.logger.sources-ignored = systemEnvironment%n\
                properties.logger.prefix-for-properties = info, logging, spring, server, management, properties, springdoc, fr%n\
                spring.application.pid =\s""").formatted())
                .contains(("""
                fr.insee.test = ok%n\
                fr.insee.secret = ******%n\
                fr.insee.shared = application.properties%n\
                fr.insee.specific.applicationproperties = application.properties%n\
                ================================================================================%n""").formatted()
               );
    }

}
