package io.github.fbibonne.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static io.github.fbibonne.springaddons.boot.propertieslogger.PropertiesLogger.*;
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
        assertThat(output.toString()).contains(("""
                        main] i.g.f.s.b.p.PropertiesLogger             : %n\
                        ================================================================================
                                                %1$sValues of properties from sources :%2$s
                        - Config resource 'class path resource [additional-file.properties]' via location 'classpath:additional-file.properties'%n\
                        - Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'%n\
                        - Config resource 'file [src/test/resources/otherProps/application.properties]' via location 'file:src/test/resources/otherProps/application.properties'%n\
                        - Inlined Test Properties%n\
                        - applicationInfo%n\
                        - commandLineArgs%n\
                        - systemProperties%n\
                                                             ====%n\
                        %3$sfrom.system.properties%2$s = %4$strue%2$s ### %5$sFROM "from.system.properties" from property source "systemProperties"%2$s ###
                        %3$sio.github.fbibonne.secret%2$s = %4$s******%2$s ### %5$sFROM class path resource [application.properties] - 2:29%2$s ###
                        %3$sio.github.fbibonne.shared%2$s = %4$sadditionalPropsInClasspath%2$s ### %5$sFROM URL [file:src/test/resources/otherProps/application.properties] - 1:29%2$s ###
                        %3$sio.github.fbibonne.specific.additional-file%2$s = %4$sadditional-file.properties%2$s ### %5$sFROM class path resource [additional-file.properties] - 2:47%2$s ###
                        %3$sio.github.fbibonne.specific.additionalPropsInClasspath%2$s = %4$sadditionalPropsInClasspath%2$s ### %5$sWARNING ! Configuration property name 'io.github.fbibonne.specific.additionalPropsInClasspath' is not valid : see org.springframework.boot.context.properties.source.ConfigurationPropertyName%2$s ###
                        %3$sio.github.fbibonne.specific.applicationproperties%2$s = %4$sapplication.properties%2$s ### %5$sFROM class path resource [application.properties] - 4:53%2$s ###
                        %3$sio.github.fbibonne.test%2$s = %4$sok%2$s ### %5$sFROM class path resource [application.properties] - 1:27%2$s ###
                        %3$slogging.level.io.github.fbibonne.springaddons.boot%2$s = %4$strace%2$s ### %5$sFROM "logging.level.io.github.fbibonne.springaddons.boot" from property source "Inlined Test Properties"%2$s ###
                        %3$sproperties.logger.prefix-for-properties%2$s = %4$sinfo, logging, spring, server, management, properties, springdoc, io, from%2$s ### %5$sFROM "properties.logger.prefix-for-properties" from property source "Inlined Test Properties"%2$s ###
                        %3$sproperties.logger.sources-ignored%2$s = %4$ssystemEnvironment%2$s ### %5$sFROM "properties.logger.sources-ignored" from property source "Inlined Test Properties"%2$s ###
                        %3$sspring.application.pid%2$s =\s""").formatted(AINSI_GREEN_BOLD_SEQUENCE, ANSI_NORMAL_SEQUENCE, ANSI_CYAN_BOLD_SEQUENCE, ANSI_BROWN_UNDERLINE_SEQUENCE, AINSI_PURPLE_ITALIC_SEQUENCE))
                .contains(("""
                        %3$sspring.config.additional-location%2$s = %4$sclasspath:additional-file.properties,file:src/test/resources/otherProps/application.properties%2$s ### %5$sFROM "spring.config.additional-location" from property source "commandLineArgs"%2$s ###
                        %3$sspring.datasource.username%2$s = %4$suser_prod%2$s ### %5$sFROM System Environment Property "SPRING_DATASOURCE_USERNAME"%2$s ###
                        %3$sspring.jmx.enabled%2$s = %4$sfalse%2$s ### %5$sFROM "spring.jmx.enabled" from property source "Inlined Test Properties"%2$s ###
                        ================================================================================%n""").formatted(AINSI_GREEN_BOLD_SEQUENCE, ANSI_NORMAL_SEQUENCE, ANSI_CYAN_BOLD_SEQUENCE, ANSI_BROWN_UNDERLINE_SEQUENCE, AINSI_PURPLE_ITALIC_SEQUENCE)
                );
    }

}
