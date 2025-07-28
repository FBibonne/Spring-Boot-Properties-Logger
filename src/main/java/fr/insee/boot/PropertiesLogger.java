package fr.insee.boot;

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
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

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

    public void doLogProperties(final EnvironmentPreparedEventForPropertiesLogging.CustomAbstractEnvironment abstractEnvironment) {
        log.debug(() -> "Start logging properties with prefix " + allowedPrefixForProperties + " for all properties sources except " + ignoredPropertySources + ". Values masked for properties whose keys contain " + propertiesWithHiddenValues);

        abstractEnvironment.getPropertySources().stream()
                .mapMulti(this::asEnumerablePropertySourceIfHasToBeProcessed)
                .flatMap(this::toPropertyNames)
                .distinct()
                .filter(this::nonNullKeyWithPrefix)
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
        if (propertiesWithHiddenValues.stream().anyMatch(key::contains)) {
            return MASK;
        }
        return environment.getPropertySafely(key);
    }

    private Stream<String> toPropertyNames(EnumerablePropertySource<?> propertySource) {
        log.trace(() -> "Flat properties for " + propertySource.getName());
        return Arrays.stream(propertySource.getPropertyNames());
    }

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
            log.trace(() -> propertySource + " is a EnumerablePropertySource : will be used to find keys");
            return true;
        }
        if (propertySource instanceof OriginLookup<?>) {
            if ("org.springframework.boot.context.properties.source.ConfigurationPropertySourcesPropertySource".equals(propertySource.getClass().getCanonicalName())){
                log.debug(()-> propertySource + " will be used as the originFinder but will be ignored as a property source");
                final OriginLookup<String> originLookup = (OriginLookup<String>) propertySource;
                this.originFinder=key->{
                    var origin=originLookup.getOrigin(key);
                    return origin==null?"":" "+ origin;
                };
            }else{
                log.debug(()-> propertySource + " will be ignored as a property source and also as OriginLookup");
            }
            return false;
        }
        log.warn(()-> propertySource + " is unknown : will be ignored");
        return false;
    }

    private boolean nonNullKeyWithPrefix(String key) {
        log.trace(() -> "Check if property " + key + " can be displayed");
        if (key == null) {
            return false;
        }
        for (String prefix : allowedPrefixForProperties) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        log.debug(() -> key + " doesn't start with a logable prefix");
        return false;
    }

}
