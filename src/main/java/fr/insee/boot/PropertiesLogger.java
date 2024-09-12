package fr.insee.boot;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public record PropertiesLogger() implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final LocalLogger log = new LocalLogger(PropertiesLogger.class);

    public static final String SEPARATION_LINE = "================================================================================";

    private static final Set<String> DEFAULT_PROPS_WITH_HIDDEN_VALUES = Set.of("password", "pwd", "token", "secret", "credential", "pw");
    private static final Set<String> DEFAULT_PREFIX_FOR_PROPERTIES = Set.of("debug", "trace", "info", "logging", "spring", "server", "management", "springdoc", "properties");
    private static final Set<String> DEFAULT_SOURCES_IGNORED = Set.of(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);

    private static final String KEY_FOR_PROPS_WITH_HIDDEN_VALUES = "properties.logger.with-hidden-values";
    private static final String KEY_FOR_PREFIX_FOR_PROPERTIES = "properties.logger.prefix-for-properties";
    private static final String KEY_FOR_SOURCES_IGNORED = "properties.logger.sources-ignored";
    public static final String MASK = "******";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        log.debug(() -> "Starting PropertiesLogger on ApplicationEnvironmentPreparedEvent");
        final Environment environement = event.getEnvironment();
        log.trace(() -> "Collecting properties to configure PropertiesLogger");
        final Set<String> propertiesWithHiddenValues = getPropertyOrDefaultAndTrace(environement, KEY_FOR_PROPS_WITH_HIDDEN_VALUES, DEFAULT_PROPS_WITH_HIDDEN_VALUES);
        final Set<String> allowedPrefixForProperties = getPropertyOrDefaultAndTrace(environement, KEY_FOR_PREFIX_FOR_PROPERTIES, DEFAULT_PREFIX_FOR_PROPERTIES);
        final Set<String> ignoredPropertySources = getPropertyOrDefaultAndTrace(environement, KEY_FOR_SOURCES_IGNORED, DEFAULT_SOURCES_IGNORED);
        final Set<String> propertySourceNames = new HashSet<>();

        log.debug(() -> "Start logging properties with prefix " + allowedPrefixForProperties + " for all properties sources except " + ignoredPropertySources + ". Values masked for properties whose keys contain " + propertiesWithHiddenValues);

        var stringWithPropertiesToDisplay = new StringBuilder();

        ((AbstractEnvironment) environement).getPropertySources().stream()
                .filter(source -> willBeProcessed(source, ignoredPropertySources))
                .flatMap(source -> rememberPropertySourceNameThenFlatPropertiesNames(source, propertySourceNames))
                .distinct()
                .filter(key -> nonNullKeyWithPrefix(key, allowedPrefixForProperties))
                .forEach(key -> resolveValueThenAppendToDisplay(key, stringWithPropertiesToDisplay, propertiesWithHiddenValues, environement));

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

    private boolean willBeProcessed(PropertySource<?> propertySource, Set<String> ignoredPropertySources) {
        return isEnumerable(propertySource) && isNotIgnored(propertySource, ignoredPropertySources);
    }

    private boolean isNotIgnored(PropertySource<?> propertySource, Set<String> ignoredPropertySources) {
        if (ignoredPropertySources.stream().anyMatch(propertySource.getName()::contains)) {
            log.trace(() -> propertySource + " is listed to be ignored");
            return false;
        }
        return true;
    }

    private void resolveValueThenAppendToDisplay(String key, StringBuilder stringWithPropertiesToDisplay, Set<String> propertiesWithHiddenValues, Environment environement) {
        stringWithPropertiesToDisplay.append(key).append(" = ")
                .append(resolveValueThenMaskItIfSecret(key, propertiesWithHiddenValues, environement))
                .append(System.lineSeparator());
    }

    private String resolveValueThenMaskItIfSecret(String key, Set<String> propertiesWithHiddenValues, Environment environement) {
        if (propertiesWithHiddenValues.stream().anyMatch(key::contains)) {
            return MASK;
        }
        return environement.getProperty(key);
    }

    private Stream<String> rememberPropertySourceNameThenFlatPropertiesNames(PropertySource<?> propertySource, Set<String> propertySourceNames) {
        String propertySourceName = propertySource.getName();
        log.trace(() -> "Flat properties for " + propertySourceName);
        propertySourceNames.add(propertySourceName);
        return Arrays.stream(((EnumerablePropertySource<?>) propertySource).getPropertyNames());
    }

    private boolean isEnumerable(PropertySource<?> propertySource) {
        if (!(propertySource instanceof EnumerablePropertySource)) {
            log.debug(() -> propertySource + " is not EnumerablePropertySource : unable to list");
            return false;
        }
        return true;
    }

    private Set<String> getPropertyOrDefaultAndTrace(Environment environement, String key, Set<String> defaultValue) {
        Set<String> result = environement.getProperty(key, Set.class, defaultValue);
        log.trace(() -> key + " -> " + result);
        return result;
    }

    private boolean nonNullKeyWithPrefix(String key, Set<String> allowedPrefixForProperties) {
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
