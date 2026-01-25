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
    ANSI_PROPERTY_SOURCE("AnsiPropertySource"),
    STUB_PROPERTY_SOURCE("StubPropertySource"), RANDOM_VALUE_PROPRERTY_SOURCE("RandomValuePropertySource"),
    JDNI_PROPERTY_SOURCE_TYPE("JndiPropertySource"),
    ENUMERABLE_PROPERTY_SOURCE("EnumerablePropertySource"),
    ORIGIN_LOOKUP_PROPERTY_SOURCE("OriginLookup"), UNKNOWN("unknown type");

    private final String className;

    PropertySourceType(String className) {
        this.className = className;
    }

    private static final LocalLogger log = new LocalLogger(PropertySourceType.class);

    public static PropertySourceType of(PropertySource<?> propertySource) {
        PropertySourceType propertySourceType = isEnumerable(propertySource) ? ENUMERABLE_PROPERTY_SOURCE : ofNonEnumerablePropertySource(propertySource);
        propertySourceType.log(propertySource);
        return propertySourceType;
    }

    private static PropertySourceType ofNonEnumerablePropertySource(PropertySource<?> propertySource) {
        return propertySource instanceof OriginLookup<?> ? ORIGIN_LOOKUP_PROPERTY_SOURCE : ofOtherPropertySource(propertySource);
    }

    private static PropertySourceType ofOtherPropertySource(PropertySource<?> propertySource) {
        return isAnsiPropertySource(propertySource) ? ANSI_PROPERTY_SOURCE : ofIgnoredPropertySource(propertySource);
    }

    private static PropertySourceType ofIgnoredPropertySource(PropertySource<?> propertySource) {
        PropertySourceType propertySourceType;
        if (propertySource instanceof PropertySource.StubPropertySource) {
            propertySourceType = STUB_PROPERTY_SOURCE;
        } else if (propertySource instanceof RandomValuePropertySource) {
            propertySourceType = RANDOM_VALUE_PROPRERTY_SOURCE;
        } else if (propertySource instanceof JndiPropertySource) {
            propertySourceType = JDNI_PROPERTY_SOURCE_TYPE;
        } else {
            propertySourceType = UNKNOWN;
        }
        return propertySourceType;
    }

    private static boolean isAnsiPropertySource(PropertySource<?> propertySource) {
        return propertySource instanceof AnsiPropertySource;
    }

    private static boolean isEnumerable(PropertySource<?> propertySource) {
        return propertySource instanceof EnumerablePropertySource<?>;
    }

    private void log(PropertySource<?> propertySource) {
        withLogLevel().accept(() -> logMessage(propertySource));
    }

    private String logMessage(PropertySource<?> propertySource) {
        return switch (this) {
            case ANSI_PROPERTY_SOURCE ->
                    "Processing of AnsiPropertySource " + propertySource + " not yet implemented : properties exclusively from this property source will be ignored";
            case STUB_PROPERTY_SOURCE ->
                    propertySource + " is a stub property source : it does not contain properties : will be ignored";
            case RANDOM_VALUE_PROPRERTY_SOURCE, ORIGIN_LOOKUP_PROPERTY_SOURCE, JDNI_PROPERTY_SOURCE_TYPE,
                 UNKNOWN -> propertySource + " is a " + this.className + " : it is not enumerable : will be ignored";
            case ENUMERABLE_PROPERTY_SOURCE ->
                    propertySource + " is a EnumerablePropertySource : is a candidate to find keys";
        };
    }

    private Consumer<Supplier<String>> withLogLevel() {
        return switch (this) {
            case ANSI_PROPERTY_SOURCE, JDNI_PROPERTY_SOURCE_TYPE, UNKNOWN -> log::warn;
            case STUB_PROPERTY_SOURCE, RANDOM_VALUE_PROPRERTY_SOURCE, ORIGIN_LOOKUP_PROPERTY_SOURCE -> log::debug;
            case ENUMERABLE_PROPERTY_SOURCE -> log::trace;
        };
    }

    public boolean isEnumerable() {
        return switch (this) {
            case ANSI_PROPERTY_SOURCE, STUB_PROPERTY_SOURCE, RANDOM_VALUE_PROPRERTY_SOURCE, JDNI_PROPERTY_SOURCE_TYPE,
                 ORIGIN_LOOKUP_PROPERTY_SOURCE, UNKNOWN -> false;
            case ENUMERABLE_PROPERTY_SOURCE -> true;
        };
    }
}
