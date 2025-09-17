package io.github.fbibonne.springaddons.boot.propertieslogger;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PropertiesLoggerTest {

    @ParameterizedTest
    @CsvSource({
            "'io.github.fbibonne', 'fbibonne', true",
            "'org.test-dash', 't-d', true",
            "'io.github.fbibonne', 'fBibonne', true",
            "'io.github.fbibonne', 'hub.FB', true",
            "'org.test-dash', 't-D', true",
            "'io.github.FBibonne', 'fbibonne', true",
            "'io.github.FBibonne', 'FBIBONNE', true",
            "'io.gitHUB.fbibonne', 'hub.fb', true",
            "'org.test-dash', 't-D', true",
            "'org.tesT-Dash', 't-d', true",
            "'org.test-dash', 't_d', false",
            "'io.gitHUB-fbibonne', 'hub.fb', false",
            "'io.github.fbibonne', 'io.fb', false",
            "'stringFirst', 'string', true",
            "'stringFirst', 'StrinG', true",
            "'stringLast', 'Lasts', false",
            "'stringFirst', 'astring', false",
            "'null.is.not.contained', , false",
            "'empty.is.contained', '', true",
            "'self.is.contained', 'self.is.contained', true",
            "'self.is.contained', 'SelF.iS.conTained', true",
            "'longer.is.not.contained','longer.is.not.contained+', false"
    })
    void isValueContainedIgnoringCaseInTest(String container, String value, boolean expected) {
        PropertiesLogger propertiesLogger = new PropertiesLogger(null, null, null);
        assertThat(propertiesLogger.isValueContainedIgnoringCaseIn(container).test(value)).isEqualTo(expected);
    }
}