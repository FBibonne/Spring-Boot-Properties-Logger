package io.github.fbibonne.springaddons.boot.propertieslogger;

import org.springframework.boot.ansi.AnsiPropertySource;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.jndi.JndiPropertySource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Class doing the real stuff for logging properties. The collect of properties to log, their value and the logging is done
 * inside the {@link this#doLogProperties(EnvironmentPreparedEventForPropertiesLogging.CustomAbstractEnvironment)} method.
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
    final Set<String> propertySourceNames = new HashSet<>();
    final StringBuilder stringWithPropertiesToDisplay = new StringBuilder();

    UnaryOperator<String> originFinder = k->"";


    PropertiesLogger(PropertiesWithHiddenValues propertiesWithHiddenValues, AllowedPrefixForProperties allowedPrefixForProperties, IgnoredPropertySources ignoredPropertySources) {
        this.propertiesWithHiddenValues = propertiesWithHiddenValues;
        this.allowedPrefixForProperties = allowedPrefixForProperties;
        this.ignoredPropertySources = ignoredPropertySources;
    }

    /**
     * This method :
     * <ol>
     *     <li>lists all propertySources of the environment and exclude all those which cannot be processed (see method {@link this#isEnumerable(PropertySource)}
     *     or are listed to be ignored (see property {@code properties.logger.sources-ignored}</li>
     *     <li>for each propertySource not excluded, list all property keys then exclude {@code null} keys and non-allowed prefixed ones (see property {@code properties.logger.prefix-for-properties}</li>
     *     <li>order distinct keys with alphabetical order (natural order of {@link String}</li>
     *     <li>for each key, compute an expression {@code key = value ### FROM value_origin ###}  where {@code value} is the value of the key resolved against
     *     the environment (or masked if key is listed in property {@code properties.logger.with-hidden-values}) and {@code value_origin} is the
     *     propertySource which contains the value to which the key is resolved (if field {@link this#originFinder} can resolve it)</li>
     *     <li>log the list of used propertySources to find keys, the ordered list of properties and their values and origin when available</li>
     * </ol>
     *
     * @param abstractEnvironment : the environment of the application wrapped as an {@link io.github.fbibonne.springaddons.boot.propertieslogger.EnvironmentPreparedEventForPropertiesLogging.CustomAbstractEnvironment}
     *                            to safely resolve properties with unknown placeholders
     */
    public void doLogProperties(final EnvironmentPreparedEventForPropertiesLogging.CustomAbstractEnvironment abstractEnvironment) {
        log.debug(() -> "Start logging properties with prefix " + allowedPrefixForProperties + " for all properties sources except " + ignoredPropertySources + ". Values masked for properties whose keys contain " + propertiesWithHiddenValues);

        abstractEnvironment.getPropertySources().stream()
                .mapMulti(this::asEnumerablePropertySourceIfHasToBeProcessed)
                .flatMap(this::toPropertyNames)
                .distinct()
                .filter(this::keyWithAllowedPrefix)
                .sorted()
                .forEach(key -> resolveValueThenAppendToDisplay(key, abstractEnvironment));

        stringWithPropertiesToDisplay
                .append(SEPARATION_LINE)
                .insert(0, System.lineSeparator())
                .insert(0, "                                     ====");
        insertPropretySourceNamesOnePerLine(propertySourceNames, stringWithPropertiesToDisplay);
        stringWithPropertiesToDisplay.insert(0, """
                        
                                                Values of properties from sources :
                        """
                )
                .insert(0, SEPARATION_LINE)
                .insert(0, System.lineSeparator());


        log.info(stringWithPropertiesToDisplay::toString);
    }

    private void insertPropretySourceNamesOnePerLine(Set<String> propertySourceNames, StringBuilder stringWithPropertiesToDisplay) {
        propertySourceNames.forEach(name -> stringWithPropertiesToDisplay.insert(0, System.lineSeparator()).insert(0, name).insert(0, "- "));
    }

    private void asEnumerablePropertySourceIfHasToBeProcessed(PropertySource<?> propertySource, Consumer<EnumerablePropertySource<?>> downstream) {
        var mustBeProcessed = isEnumerable(propertySource) && isNotIgnored(propertySource);
        if (mustBeProcessed) {
            propertySourceNames.add(propertySource.getName());
            downstream.accept((EnumerablePropertySource<?>) propertySource);
        }
    }

    private boolean isNotIgnored(PropertySource<?> propertySource) {
        if (ignoredPropertySources.stream().anyMatch(propertySource.getName()::contains)) {
            log.trace(() -> propertySource + " is listed to be ignored");
            return false;
        }
        return true;
    }

    private void resolveValueThenAppendToDisplay(String key, EnvironmentPreparedEventForPropertiesLogging.CustomAbstractEnvironment environement) {
        stringWithPropertiesToDisplay.append(key).append(" = ")
                .append(resolveValueThenMaskItIfSecret(key, environement))
                .append(originFinder.apply(key))
                .append(System.lineSeparator());
    }

    private String resolveValueThenMaskItIfSecret(String key, EnvironmentPreparedEventForPropertiesLogging.CustomAbstractEnvironment environment) {
        if (propertiesWithHiddenValues.stream().anyMatch(isValueContainedIgnoringCaseIn(key))) {
            return MASK;
        }
        return environment.getPropertySafely(key);
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
    }

    /**
     * Method which filter propertySource which can be processed to find properties to log.
     * propertySource which can be processed are a subtype of {@link EnumerablePropertySource} : if not method log a
     * debug message or a warning message for cases not implemented yet (AnsiPropertySource and JndiPropertySource). If
     * propertySource is of type {@link org.springframework.boot.context.properties.source.ConfigurationPropertySourcesPropertySource},
     * it will be referenced to resolve origin of property values later (see {@link this#originFinder}. If propertySource
     * has an unknown type, a warning is logged.
     * @param propertySource : instance of {@link PropertySource} which is checked for being processed or not.
     * @return true if the propertySource can be processed.
     */
    private boolean isEnumerable(PropertySource<?> propertySource) {
        if (propertySource instanceof AnsiPropertySource ansiPropertySource) {
            log.warn(()->"Processing of AnsiPropertySource "+ ansiPropertySource+" not yet implemented : properties exclusively from this property source will be ignored");
            return false;
        }
        if (propertySource instanceof PropertySource.StubPropertySource) {
            log.debug(() -> propertySource + " is a stub property source : it does not contain properties : will be ignored");
            return false;
        }
        if (propertySource instanceof RandomValuePropertySource) {
            log.debug(()-> propertySource + " is a RandomValuePropertySource : will be ignored");
            return false;
        }
        if (propertySource instanceof JndiPropertySource){
            log.warn(()-> propertySource + " is a JndiPropertySource : it is not enumerable : will be ignored");
            return false;
        }
        if (propertySource instanceof EnumerablePropertySource<?>){
            log.trace(() -> propertySource + " is a EnumerablePropertySource : is a candidate to find keys");
            return true;
        }
        if (propertySource instanceof OriginLookup<?>) {
            if ("org.springframework.boot.context.properties.source.ConfigurationPropertySourcesPropertySource".equals(propertySource.getClass().getCanonicalName())){
                log.debug(()-> propertySource + " will be used as the originFinder but will be ignored as a property source");
                final OriginLookup<String> originLookup = (OriginLookup<String>) propertySource;
                this.originFinder=key->{
                    var origin=originLookup.getOrigin(key);
                    return origin==null?"":" ### FROM "+ origin+" ###";
                };
            }else{
                log.debug(()-> propertySource + " will be ignored as a property source and also as OriginLookup");
            }
            return false;
        }
        log.warn(()-> propertySource + " is unknown : will be ignored");
        return false;
    }

    private boolean keyWithAllowedPrefix(String key) {
        log.trace(() -> "Check if property " + key + " can be displayed");
        if (allowedPrefixForProperties.anyMatch(key::startsWith)) {
            return true;
        }
        log.debug(() -> key + " doesn't start with a logable prefix");
        return false;
    }

}
