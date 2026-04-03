package io.github.fbibonne.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;

import static io.github.fbibonne.springaddons.boot.propertieslogger.ConstantsForTests.MASK;
import static io.github.fbibonne.springaddons.boot.propertieslogger.PropertiesLogger.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ExcludedPropertySourcesTest.class, properties = {
        "com.mycompany.serviceContactPassword = verysecretpassword",
        "com.mycompany.service-contact-token = ",
        "properties.logger.prefix-for-properties = com",
        "properties.logger.with-hidden-values = passWord"
})
@Configuration
@ExtendWith(OutputCaptureExtension.class)
class MaskedValuesTest {

    @Test
    void checkMaskValuesIgnoreCase(CapturedOutput output) {
        assertThat(output.toString()).doesNotContain("verysecretpassword")
                .contains(ANSI_CYAN_BOLD_SEQUENCE+"com.mycompany.serviceContactPassword"+ANSI_NORMAL_SEQUENCE+" = "+ANSI_BROWN_UNDERLINE_SEQUENCE+MASK);
    }

    @Test
    void emptySecretShouldNotBeMasked(CapturedOutput output) {
        assertThat(output.toString())
                .contains("""
                %3$scom.mycompany.service-contact-token%2$s = %4$s%2$s ### %5$sFROM "com.mycompany.service-contact-token" from property source "Inlined Test Properties"%2$s ###
                """.formatted(null, ANSI_NORMAL_SEQUENCE, ANSI_CYAN_BOLD_SEQUENCE, ANSI_BROWN_UNDERLINE_SEQUENCE, AINSI_PURPLE_ITALIC_SEQUENCE));
    }

}
