package io.github.fbibonne.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "properties.logger.coloration.disabled=true",
        "properties.logger.sources-ignored = systemEnvironment",
        "properties.logger.prefix-for-properties = properties",
})
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ColorationDisabledIntegrationTest {

    @Test
    @DisplayName("When properties.logger.coloration.disabled=true, properties should be displayed without ANSI color sequences")
    void propertiesShouldBeDisplayedWithoutAnsiSequencesWhenColorationDisabled(CapturedOutput output) {
        assertThat(output.toString())
                .contains("properties.logger.coloration.disabled = true")
                .doesNotContain("\u001B[1;36m")   // no cyan bold (property names)
                .doesNotContain("\u001B[4;33m")   // no brown underline (values)
                .doesNotContain("\u001B[1;3;35m") // no purple italic (origins)
                .doesNotContain("\u001B[1;32m");  // no green bold (header)
    }

    @SpringBootApplication
    static class ConfigurationForTest {}
}