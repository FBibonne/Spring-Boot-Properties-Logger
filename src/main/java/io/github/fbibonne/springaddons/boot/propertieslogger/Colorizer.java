package io.github.fbibonne.springaddons.boot.propertieslogger;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiStyle;

/**
 * Applies ANSI color sequences to strings for console display, based on the type of data to colorize.
 * When coloration is disabled (property {@code properties.logger.coloration.disabled=true}), all methods
 * return the input string unchanged.
 * Uses Spring Boot's {@link AnsiColor} and {@link AnsiStyle} constants for ANSI code values.
 */
class Colorizer {

    private static final String ANSI_OPEN = "\u001B[";
    private static final String ANSI_CLOSE = "m";
    private static final String ANSI_RESET = ANSI_OPEN + AnsiStyle.NORMAL + ANSI_CLOSE;

    private final boolean colorationDisabled;

    Colorizer(boolean colorationDisabled) {
        this.colorationDisabled = colorationDisabled;
    }

    String colorizePropertyName(String propertyName) {
        if (colorationDisabled) return propertyName;
        return styled(propertyName, AnsiStyle.BOLD, AnsiColor.CYAN);
    }

    String colorizeValue(@Nullable String value) {
        String text = String.valueOf(value);
        if (colorationDisabled) return text;
        return styled(text, AnsiStyle.UNDERLINE, AnsiColor.YELLOW);
    }

    String colorizeOrigin(String origin) {
        if (colorationDisabled) return origin;
        return styled(origin, AnsiStyle.BOLD, AnsiStyle.ITALIC, AnsiColor.MAGENTA);
    }

    String colorizeHeader(String header) {
        if (colorationDisabled) return header;
        return styled(header, AnsiStyle.BOLD, AnsiColor.GREEN);
    }

    private static String styled(String text, AnsiElement... styles) {
        StringBuilder sb = new StringBuilder(ANSI_OPEN);
        for (int i = 0; i < styles.length; i++) {
            if (i > 0) sb.append(";");
            sb.append(styles[i]);
        }
        return sb.append(ANSI_CLOSE).append(text).append(ANSI_RESET).toString();
    }
}