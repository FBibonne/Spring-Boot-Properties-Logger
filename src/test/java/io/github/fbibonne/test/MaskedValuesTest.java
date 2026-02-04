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
        "properties.logger.prefix-for-properties = com"
})
@Configuration
@ExtendWith(OutputCaptureExtension.class)
class MaskedValuesTest {

    @Test
    void checkMaskValuesIgnoreCase(CapturedOutput output) {
        assertThat(output.toString()).doesNotContain("verysecretpassword")
                .contains(ANSI_CYAN_BOLD_SEQUENCE+"com.mycompany.serviceContactPassword"+ANSI_NORMAL_SEQUENCE+" = "+ANSI_BROWN_UNDERLINE_SEQUENCE+MASK);
    }

}
