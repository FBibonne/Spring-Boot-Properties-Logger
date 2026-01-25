package io.github.fbibonne.springaddons.boot.propertieslogger;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class doing the real stuff for logging properties. The collect of properties to log, their value and the logging is done
 * inside the {@link this#doLogProperties()} method.
 * The state of the class stores {@link PropertySource} found, and a reference to an operator providing the origin of a property
 * which is found while scanning propertySources
 */
class PropertiesLogger {

    public static final String SEPARATION_LINE = "================================================================================";
    public static final String MASK = "******";
    private static final LocalLogger log = new LocalLogger(PropertiesLogger.class);

    final PropertiesWithHiddenValues propertiesWithHiddenValues;
    final AllowedPrefixForProperties allowedPrefixForProperties;
    final IgnoredPropertySources ignoredPropertySources;
    final EnvironmentPreparedEventForPropertiesLogging.CustomAbstractEnvironment abstractEnvironment;

    UnaryOperator<String> originFinder = k -> "";


    PropertiesLogger(PropertiesWithHiddenValues propertiesWithHiddenValues, AllowedPrefixForProperties allowedPrefixForProperties, IgnoredPropertySources ignoredPropertySources, EnvironmentPreparedEventForPropertiesLogging.CustomAbstractEnvironment abstractEnvironment) {
        this.propertiesWithHiddenValues = propertiesWithHiddenValues;
        this.allowedPrefixForProperties = allowedPrefixForProperties;
        this.ignoredPropertySources = ignoredPropertySources;
        this.abstractEnvironment = abstractEnvironment;
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
     *           <li>propertySource of type <code>org.springframework.boot.context.properties.source.ConfigurationPropertySourcesPropertySource</code>
     *           won't be processed but will be used to resolve origin of property values later (see {@link this#originFinder}</li>
     *           <li>If propertySource has an unknown type, a warning is logged</li>
     *       </ul>
     *     </li>
     *     <li>for each propertySource not excluded, list all property keys then exclude {@code null} keys and non-allowed prefixed ones (see property {@code properties.logger.prefix-for-properties}</li>
     *     <li>order distinct keys with alphabetical order (natural order of {@link String}</li>
     *     <li>for each key, compute an expression {@code key = value ### FROM value_origin ###}  where {@code value} is the value of the key resolved against
     *     the environment ({@link PropertiesLogger#abstractEnvironment}) or masked if key is listed in property {@code properties.logger.with-hidden-values}. And {@code value_origin} is the
     *     propertySource which contains the value to which the key is resolved (if field {@link this#originFinder} can resolve it)</li>
     *     <li>log the list of used propertySources to find keys, the ordered list of properties and their values and origin when available</li>
     * </ol>
     *
     */
    public void doLogProperties() {
        debugStarting();
        final StringBuilder stringWithPropertiesToDisplay = new StringBuilder();

        Map<String, String[]> propertyNamesBySource = propertyNamesBySourceFromEnvironment();
        final Set<String> propertySourceNames = propertyNamesBySource.keySet();

        stringWithPropertiesToDisplay.append(toKeyValuesBlock(propertyNamesBySource))
                .append(System.lineSeparator())
                .append(SEPARATION_LINE);

        stringWithPropertiesToDisplay.insert(0, headerBlock(propertySourceNames));


        log.info(stringWithPropertiesToDisplay::toString);
    }

    private String headerBlock(Set<String> propertySourceNames) {
        return """
                
                %1$s
                                        Values of properties from sources :
                %2$s
                                                     ====
                """.formatted(SEPARATION_LINE, insertPropretySourceNamesOnePerLine(propertySourceNames));
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
            PropertySourceType propertySourceType = PropertySourceType.of(propertySource);
            asEnumerablePropertySourceIfHasToBeProcessed(propertySourceType, propertySource)
                    .ifPresent(enumerablePropertySource ->
                            propertyNamesBySource.put(enumerablePropertySource.getName(), enumerablePropertySource.getPropertyNames())
                    );
            processOriginFinder(propertySource, propertySourceType);
        }
        return propertyNamesBySource;
    }

    private void processOriginFinder(PropertySource<?> propertySource, PropertySourceType propertySourceType) {
        if (propertySourceType.isOriginFinder()) {
            final OriginLookup<String> originLookup = (OriginLookup<String>) propertySource;
            this.originFinder = asOriginFinder(originLookup);
        }
    }

    private static UnaryOperator<String> asOriginFinder(OriginLookup<String> originLookup) {
        return key -> {
            var origin = originLookup.getOrigin(key);
            return originAsLine(origin);
        };
    }

    private static String originAsLine(@Nullable Origin origin) {
        return origin == null ? "" : " ### FROM " + origin + " ###";
    }

    private void debugStarting() {
        log.debug(() -> "Start logging properties with prefix " + allowedPrefixForProperties + " for all properties sources except "
                + ignoredPropertySources + ". Values masked for properties whose keys contain " + propertiesWithHiddenValues);
    }

    private String insertPropretySourceNamesOnePerLine(Set<String> propertySourceNames) {
        return propertySourceNames.stream().sorted().map(name -> "- " + name).collect(Collectors.joining(System.lineSeparator()));
    }

    private Optional<EnumerablePropertySource<?>> asEnumerablePropertySourceIfHasToBeProcessed(PropertySourceType propertySourceType, PropertySource<?> propertySource) {
        if (mustBeProcessed(propertySourceType, propertySource)) {
            return Optional.of((EnumerablePropertySource<?>) propertySource);
        }
        return Optional.empty();
    }

    private boolean mustBeProcessed(PropertySourceType propertySourceType, PropertySource<?> propertySource) {
        return propertySourceType.isEnumerable() && isNotIgnored(propertySource);
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
        return key + " = " + resolveValueThenMaskItIfSecret(key, this.abstractEnvironment) + originFinder.apply(key);
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
     * @param container the string which is supposed to contain the argument passed to the predicate.
     *                  Must be not null
     * @return a predicate such as <code>container::containsIgnoreCase</code>
     * (if String#containsIgnoreCase would exist)
     */
    Predicate<? super String> isValueContainedIgnoringCaseIn(String container) {
        return value -> {
            if (value==null){
                return false;
            }
            if (value.isEmpty()){
                return true;
            }
            int valueLength = value.length();
            for(int i = 0; i <= container.length()- valueLength; i++) {
                if (container.substring(i, i+valueLength).equalsIgnoreCase(value)){
                    return true;
                }
            }
            return false;
        };
    }

    private Stream<String> toPropertyNames(EnumerablePropertySource<?> propertySource) {
        log.trace(() -> "Flat properties for " + propertySource.getName());
        return Arrays.stream(propertySource.getPropertyNames());
    private boolean mustBeMasked(String key) {
        return propertiesWithHiddenValues.stream().anyMatch(key::contains);
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
