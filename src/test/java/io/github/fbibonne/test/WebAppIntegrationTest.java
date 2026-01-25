package io.github.fbibonne.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = WebAppIntegrationTest.class, properties = {
        "properties.logger.sources-ignored = systemEnvironment",
        "properties.logger.prefix-for-properties = info, logging, spring, server, management, properties, springdoc, io, from",
        "logging.level.io.github.fbibonne.springaddons.boot=trace"
}, args = "--spring.config.additional-location=classpath:additional-file.properties,file:src/test/resources/otherProps/application.properties")
@Configuration
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WebAppIntegrationTest {

    static {
        System.setProperty("from.system.properties", "true");
    }

    @Test
    @DisplayName("Properties should be correctly displayed and sorted when starting in Web context")
    void contextLoads(CapturedOutput output) {
        // ### FROM "spring.application.pid" from property source "applicationInfo" ###
        assertThat(output.toString()).contains(("""
                        main] i.g.f.s.b.p.PropertiesLogger             : %n\
                        ================================================================================
                                                Values of properties from sources :
                        - Config resource 'class path resource [additional-file.properties]' via location 'classpath:additional-file.properties'%n\
                        - Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'%n\
                        - Config resource 'file [src/test/resources/otherProps/application.properties]' via location 'file:src/test/resources/otherProps/application.properties'%n\
                        - Inlined Test Properties%n\
                        - applicationInfo%n\
                        - commandLineArgs%n\
                        - systemProperties%n\
                                                             ====%n\
                        from.system.properties = true ### FROM "from.system.properties" from property source "systemProperties" ###
                        io.github.fbibonne.secret = ****** ### FROM class path resource [application.properties] - 2:29 ###
                        io.github.fbibonne.shared = additionalPropsInClasspath ### FROM URL [file:src/test/resources/otherProps/application.properties] - 1:29 ###
                        io.github.fbibonne.specific.additional-file = additional-file.properties ### FROM class path resource [additional-file.properties] - 2:47 ###
                        io.github.fbibonne.specific.additionalPropsInClasspath = additionalPropsInClasspath
                        io.github.fbibonne.specific.applicationproperties = application.properties ### FROM class path resource [application.properties] - 4:53 ###
                        io.github.fbibonne.test = ok ### FROM class path resource [application.properties] - 1:27 ###
                        logging.level.io.github.fbibonne.springaddons.boot = trace ### FROM "logging.level.io.github.fbibonne.springaddons.boot" from property source "Inlined Test Properties" ###
                        properties.logger.prefix-for-properties = info, logging, spring, server, management, properties, springdoc, io, from ### FROM "properties.logger.prefix-for-properties" from property source "Inlined Test Properties" ###
                        properties.logger.sources-ignored = systemEnvironment ### FROM "properties.logger.sources-ignored" from property source "Inlined Test Properties" ###
                        spring.application.pid =\s""").formatted())
                .contains(("""
                        spring.config.additional-location = classpath:additional-file.properties,file:src/test/resources/otherProps/application.properties ### FROM "spring.config.additional-location" from property source "commandLineArgs" ###
                        spring.jmx.enabled = false ### FROM "spring.jmx.enabled" from property source "Inlined Test Properties" ###
                        ================================================================================%n""").formatted()
                );
    }

}
