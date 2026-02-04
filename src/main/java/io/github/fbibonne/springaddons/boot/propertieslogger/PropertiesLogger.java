package io.github.fbibonne.springaddons.boot.propertieslogger;

import org.jspecify.annotations.Nullable;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class doing the real stuff for logging properties. The collect of properties to log, their value and the logging is done
 * inside the <code>doLogProperties</code> method.
 * The state of the class stores {@link PropertySource} found, and a reference to an operator providing the origin of a property
 * which is found while scanning propertySources
 */
public class PropertiesLogger {

    public static final String SEPARATION_LINE = "================================================================================";
    public static final String MASK = "******";
    private static final LocalLogger log = new LocalLogger(PropertiesLogger.class);
    public static final String ANSI_CYAN_BOLD_SEQUENCE = "\u001B[1;36m";
    public static final String ANSI_NORMAL_SEQUENCE = "\u001B[0m";
    public static final String ANSI_BROWN_UNDERLINE_SEQUENCE = "\u001B[4;33m";
    public static final String AINSI_PURPLE_ITALIC_SEQUENCE = "\u001B[1;3;35m";
    public static final String AINSI_GREEN_BOLD_SEQUENCE = "\u001B[1;32m";

    final PropertiesWithHiddenValues propertiesWithHiddenValues;
    final AllowedPrefixForProperties allowedPrefixForProperties;
    final IgnoredPropertySources ignoredPropertySources;
    final EnvironmentPreparedEventForPropertiesLogging.CustomAbstractEnvironment abstractEnvironment;
    final OriginFinder originFinder;


    PropertiesLogger(PropertiesWithHiddenValues propertiesWithHiddenValues, AllowedPrefixForProperties allowedPrefixForProperties, IgnoredPropertySources ignoredPropertySources, EnvironmentPreparedEventForPropertiesLogging.CustomAbstractEnvironment abstractEnvironment) {
        this.propertiesWithHiddenValues = propertiesWithHiddenValues;
        this.allowedPrefixForProperties = allowedPrefixForProperties;
        this.ignoredPropertySources = ignoredPropertySources;
        this.abstractEnvironment = abstractEnvironment;
        this.originFinder = new OriginFinder(abstractEnvironment.getPropertySources());
    }

    /**
     * This method :
     * <ol>
     *     <li>lists all propertySources of the environment and exclude all those which won't be processed :
     *       <ul>
     *           <li>propertySources which will be processed must satisfy two conditions : be a subtype of {@link EnumerablePropertySource} AND
     *           must not be ignored (see {@link IgnoredPropertySources#isIgnored(PropertySource)}</li>
     *           <li>debug message is logged if the propertySource is ignored</li>
     *           <li>warn message is logged if the propertySource is a {@link org.springframework.boot.ansi.AnsiPropertySource} (whose processing is not implmented).
     *           in such case, it will be ignored</li>
     *
     *           <li>If propertySource has an unknown type, a warning is logged</li>
     *       </ul>
     *     </li>
     *     <li>for each propertySource not excluded, list all property keys then exclude {@code null} keys and non-allowed prefixed ones (see property {@code properties.logger.prefix-for-properties}</li>
     *     <li>order distinct keys with alphabetical order (natural order of {@link String}</li>
     *     <li>for each key, compute an expression {@code key = value ### FROM value_origin ###}  where {@code value} is the value of the key resolved against
     *     the environment ({@link PropertiesLogger#abstractEnvironment}) or masked if key is listed in property {@code properties.logger.with-hidden-values}. </li>
     *     <li>log the list of used propertySources to find keys, the ordered list of properties and their values and origin when available</li>
     * </ol>
     *
     */
    void doLogProperties() {
        debugStarting();
        final StringBuilder stringWithPropertiesToDisplay = new StringBuilder();

        Map<String, String[]> propertyNamesBySource = propertyNamesBySourceFromEnvironment();
        final Set<String> propertySourceNames = propertyNamesBySource.keySet();

        stringWithPropertiesToDisplay.append(headerBlock(propertySourceNames))
                .append(toKeyValuesBlock(propertyNamesBySource))
                .append(System.lineSeparator())
                .append(SEPARATION_LINE);

        log.info(stringWithPropertiesToDisplay::toString);
    }

    private String headerBlock(Set<String> propertySourceNames) {
        return """
                
                %1$s
                                        %2$sValues of properties from sources :%3$s
                %4$s
                                                     ====
                """.formatted(SEPARATION_LINE, AINSI_GREEN_BOLD_SEQUENCE, ANSI_NORMAL_SEQUENCE, propretySourceNamesOnePerLine(propertySourceNames));
    }

    private String toKeyValuesBlock(Map<String, String[]> propertyNamesBySource) {
        return distinctPropertiesNames(propertyNamesBySource)
                .filter(this::keyWithAllowedPrefix)
                .sorted()
                .map(this::toDisplayedLine)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private static Stream<String> distinctPropertiesNames(Map<String, String[]> propertyNamesBySource) {
        return propertyNamesBySource.values().stream()
                .flatMap(Arrays::stream)
                .distinct();
    }

    private Map<String, String[]> propertyNamesBySourceFromEnvironment() {
        Map<String, String[]> propertyNamesBySource = new HashMap<>();
        for (PropertySource<?> propertySource : this.abstractEnvironment.getPropertySources()) {
            asEnumerablePropertySourceIfHasToBeProcessed(propertySource)
                    .ifPresent(enumerablePropertySource ->
                            propertyNamesBySource.put(enumerablePropertySource.getName(), enumerablePropertySource.getPropertyNames())
                    );
        }
        return propertyNamesBySource;
    }

    private void debugStarting() {
        log.debug(() -> "Start logging properties with prefix " + allowedPrefixForProperties + " for all properties sources except "
                + ignoredPropertySources + ". Values masked for properties whose keys contain " + propertiesWithHiddenValues);
    }

    private String propretySourceNamesOnePerLine(Set<String> propertySourceNames) {
        return propertySourceNames.stream().sorted().map(name -> "- " + name).collect(Collectors.joining(System.lineSeparator()));
    }

    private Optional<EnumerablePropertySource<?>> asEnumerablePropertySourceIfHasToBeProcessed(PropertySource<?> propertySource) {
        if (mustBeProcessed(propertySource)) {
            return Optional.of((EnumerablePropertySource<?>) propertySource);
        }
        return Optional.empty();
    }

    private boolean mustBeProcessed(PropertySource<?> propertySource) {
        return PropertySourceType.isEnumerable(propertySource) && isNotIgnored(propertySource);
    }

    private boolean isNotIgnored(PropertySource<?> propertySource) {
        if (ignoredPropertySources.isIgnored(propertySource)) {
            traceIgnored(propertySource);
            return false;
        }
        return true;
    }

    private static void traceIgnored(PropertySource<?> propertySource) {
        log.trace(() -> propertySource + " is listed to be ignored");
    }

    private String toDisplayedLine(String key) {
        return ANSI_CYAN_BOLD_SEQUENCE + key + ANSI_NORMAL_SEQUENCE + " = "
                + ANSI_BROWN_UNDERLINE_SEQUENCE + resolveValueThenMaskItIfSecret(key, this.abstractEnvironment) + ANSI_NORMAL_SEQUENCE
                + this.originFinder.findOriginFor(key).map(PropertiesLogger::originAsLine).orElse("");
    }

    private static String originAsLine(String origin) {
        return " ### " + AINSI_PURPLE_ITALIC_SEQUENCE + origin + ANSI_NORMAL_SEQUENCE + " ###";
    }

    private @Nullable String resolveValueThenMaskItIfSecret(String key, EnvironmentPreparedEventForPropertiesLogging.CustomAbstractEnvironment environment) {
        if (mustBeMasked(key)) {
            return MASK;
        }
        return environment.getPropertySafely(key);
    }

    private boolean mustBeMasked(String key) {
        return propertiesWithHiddenValues.stream().anyMatch(isValueContainedIgnoringCaseIn(key));
    }

    /**
     * Implementation of a String#containsIgnoreCase based the algorithm of String#contains
     * but using String#equalsIgnoreCase to test equality
     *
     * @param container the string which is supposed to contain the argument passed to the predicate.
     *                  Must be not null
     * @return a predicate such as <code>container::containsIgnoreCase</code>
     * (if String#containsIgnoreCase would exist)
     */
    Predicate<? super String> isValueContainedIgnoringCaseIn(String container) {
        return value -> {
            if (value.isEmpty()) {
                return true;
            }
            int valueLength = value.length();
            for (int i = 0; i <= container.length() - valueLength; i++) {
                if (container.substring(i, i + valueLength).equalsIgnoreCase(value)) {
                    return true;
                }
            }
            return false;
        };
    }

    private boolean keyWithAllowedPrefix(String key) {
        log.trace(() -> "Check if property " + key + " can be displayed");
        boolean keyWithAllowedPrefix = isKeyWithAllowedPrefix(key);
        if (!keyWithAllowedPrefix) {
            debugNotAllowedPrefix(key);
        }
        return keyWithAllowedPrefix;
    }

    private static void debugNotAllowedPrefix(String key) {
        log.debug(() -> key + " doesn't start with a logable prefix");
    }

    private boolean isKeyWithAllowedPrefix(String key) {
        return allowedPrefixForProperties.anyMatch(key::startsWith);
    }

}
