package io.github.fbibonne.springaddons.boot.propertieslogger;

import org.springframework.boot.ansi.AnsiPropertySource;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.jndi.JndiPropertySource;

import java.util.function.Consumer;
import java.util.function.Supplier;

public enum PropertySourceType {
    ANSI_PROPERTY_SOURCE,
    JDNI_PROPERTY_SOURCE_TYPE,
    ENUMERABLE_PROPERTY_SOURCE,
    OTHERS, UNKNOWN;

    private static final LocalLogger log = new LocalLogger(PropertySourceType.class);

    public static boolean isEnumerable(PropertySource<?> propertySource) {
        PropertySourceType propertySourceType = of(propertySource);
        propertySourceType.log(propertySource);
        return propertySourceType.isEnumerable();
    }

    private static PropertySourceType of(PropertySource<?> propertySource) {
        return isEnumerableInstance(propertySource) ? ENUMERABLE_PROPERTY_SOURCE : ofNonEnumerablePropertySource(propertySource);
    }

    private static PropertySourceType ofNonEnumerablePropertySource(PropertySource<?> propertySource) {
        PropertySourceType propertySourceType;
        if (isAnsiPropertySource(propertySource)){
             propertySourceType = ANSI_PROPERTY_SOURCE;
        } else if (propertySource instanceof JndiPropertySource) {
            propertySourceType = JDNI_PROPERTY_SOURCE_TYPE;
        }else {
            propertySourceType = ofOtherPropertySource(propertySource);
        }
        return propertySourceType;
    }

    private static PropertySourceType ofOtherPropertySource(PropertySource<?> propertySource) {
        PropertySourceType propertySourceType;
        if (isOfOtherInstance(propertySource)) {
            propertySourceType = OTHERS;
        } else {
            propertySourceType = UNKNOWN;
        }
        return propertySourceType;
    }

    private static boolean isOfOtherInstance(PropertySource<?> propertySource) {
        return propertySource instanceof PropertySource.StubPropertySource
                || propertySource instanceof RandomValuePropertySource
                || propertySource instanceof OriginLookup<?>;
    }

    private static boolean isAnsiPropertySource(PropertySource<?> propertySource) {
        return propertySource instanceof AnsiPropertySource;
    }

    private static boolean isEnumerableInstance(PropertySource<?> propertySource) {
        return propertySource instanceof EnumerablePropertySource<?>;
    }

    private void log(PropertySource<?> propertySource) {
        withLogLevel().accept(() -> logMessage(propertySource));
    }

    private String logMessage(PropertySource<?> propertySource) {
        return switch (this) {
            case ANSI_PROPERTY_SOURCE ->
                    "Processing of AnsiPropertySource " + propertySource + " not yet implemented : properties exclusively from this property source will be ignored";
            case OTHERS, JDNI_PROPERTY_SOURCE_TYPE, UNKNOWN -> propertySource + " is not enumerable : will be ignored";
            case ENUMERABLE_PROPERTY_SOURCE ->
                    propertySource + " is a EnumerablePropertySource : is a candidate to find keys";
        };
    }

    private Consumer<Supplier<String>> withLogLevel() {
        return switch (this) {
            case ANSI_PROPERTY_SOURCE, JDNI_PROPERTY_SOURCE_TYPE, UNKNOWN -> log::warn;
            case OTHERS -> log::debug;
            case ENUMERABLE_PROPERTY_SOURCE -> log::trace;
        };
    }

    private boolean isEnumerable() {
        return this == ENUMERABLE_PROPERTY_SOURCE;
    }
}
