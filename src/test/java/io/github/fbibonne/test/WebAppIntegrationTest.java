package io.github.fbibonne.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WebAppIntegrationTest.class, properties = {
        "properties.logger.sources-ignored = systemEnvironment",
        "properties.logger.prefix-for-properties = info, logging, spring, server, management, properties, springdoc, io, from",
        "logging.level.io.github.fbibonne.boot=trace"
}, args = "--spring.config.additional-location=classpath:additional-file.properties,file:src/test/resources/otherProps/application.properties")
@Configuration
@ExtendWith(OutputCaptureExtension.class)
class WebAppIntegrationTest {

    static{
        System.setProperty("from.system.properties", "true");
    }

    @Test
    void contextLoads(CapturedOutput output) {
        assertThat(output.toString()).contains(("""
                        main] io.github.fbibonne.springaddons.boot.propertieslogger.PropertiesLogger           : %n\
                        ================================================================================
                                                Values of properties from sources :
                        - Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'%n\
                        - applicationInfo%n\
                        - Inlined Test Properties%n\
                        - systemProperties%n\
                                                             ====%n\
                        spring.jmx.enabled = false%n\
                        properties.logger.sources-ignored = systemEnvironment%n\
                        properties.logger.prefix-for-properties = info, logging, spring, server, management, properties, springdoc, io%n\
                        logging.level.io.github.fbibonne.boot = trace
                        spring.application.pid =\s""").formatted())
                .contains(("""
                        io.github.fbibonne.test = ok%n\
                        io.github.fbibonne.secret = ******%n\
                        io.github.fbibonne.shared = application.properties%n\
                        io.github.fbibonne.specific.applicationproperties = application.properties%n\
                        ================================================================================%n""").formatted()
                );
    }

}
