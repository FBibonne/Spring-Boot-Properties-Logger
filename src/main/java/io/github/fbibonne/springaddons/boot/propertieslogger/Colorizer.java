package io.github.fbibonne.springaddons.boot.propertieslogger;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiStyle;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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

    private final BiFunction<@Nullable String, AnsiElement[], String> styleApplierIfEnabled;

    Colorizer(boolean colorationDisabled) {
        this.styleApplierIfEnabled = colorationDisabled
                ? Colorizer::noColoration
                : Colorizer::applyStyles;
    }

    String colorizePropertyNameIfEnabled(String propertyName) {
        return applyStylesIfEnabled(propertyName, AnsiStyle.BOLD, AnsiColor.CYAN);
    }

    String colorizeValueIfEnabled(@Nullable String value) {
        return applyStylesIfEnabled(value, AnsiStyle.UNDERLINE, AnsiColor.YELLOW);
    }

    String colorizeOriginIfEnabled(String origin) {
        return applyStylesIfEnabled(origin, AnsiStyle.BOLD, AnsiStyle.ITALIC, AnsiColor.MAGENTA);
    }

    String colorizeHeaderIfEnabled(String header) {
        return applyStylesIfEnabled(header, AnsiStyle.BOLD, AnsiColor.GREEN);
    }

    private String applyStylesIfEnabled(@Nullable String propertyName, AnsiElement... ansiElements) {
        return styleApplierIfEnabled.apply(propertyName, ansiElements);
    }

    private static String applyStyles(@Nullable String text, AnsiElement... styles) {
        return ANSI_OPEN
                + Arrays.stream(styles).map(AnsiElement::toString).collect(Collectors.joining(";"))
                + ANSI_CLOSE
                + text
                + ANSI_RESET;
    }

    private static String noColoration(@Nullable String text, AnsiElement[] ignored) {
        return String.valueOf(text);
    }
}