package fr.insee.boot;

import lombok.CustomLog;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

@CustomLog
public record PropertiesLogger() implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String SEPARATION_LINE="================================================================================";

    private static final Set<String> DEFAULT_PROPS_WITH_HIDDEN_VALUES = Set.of("password", "pwd", "token", "secret", "credential", "pw");
    private static final Set<String> DEFAULT_PREFIX_FOR_PROPERTIES = Set.of("debug", "info", "logging", "spring", "server","management", "keycloak", "springdoc");
    private static final Set<String> DEFAULT_SOURCES_IGNORED = Set.of("systemProperties", "systemEnvironment");

    private static final String KEY_FOR_PROPS_WITH_HIDDEN_VALUES = "properties.logger.with-hidden-values";
    private static final String KEY_FOR_PREFIX_FOR_PROPERTIES = "properties.logger.prefix-for-properties";
    private static final String KEY_FOR_SOURCES_IGNORED = "properties.logger.sources-ignored";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        //TODO log info
        //TODO log debug
        final Environment environement = event.getEnvironment();
        final Set<String> propertiesWithHiddenValues = getPropertyOrDefaultAndTrace(environement, KEY_FOR_PROPS_WITH_HIDDEN_VALUES, DEFAULT_PROPS_WITH_HIDDEN_VALUES);
        final PropertyNameNonNullFilterWithPrefix nonNullPropertyNameFilterWithPrefix = new PropertyNameNonNullFilterWithPrefix(getPropertyOrDefaultAndTrace(environement, KEY_FOR_PREFIX_FOR_PROPERTIES, DEFAULT_PREFIX_FOR_PROPERTIES));
        final Set<String> ignoredPropertySources = getPropertyOrDefaultAndTrace(environement, KEY_FOR_SOURCES_IGNORED, DEFAULT_SOURCES_IGNORED);
        final Set<String> propertySourceNames = new HashSet<>();

        var stringWithPropertiesToDisplay= new StringBuilder();

        ((AbstractEnvironment) environement).getPropertySources().stream()
                .filter(source->willBeProcessed(source, ignoredPropertySources))
                .flatMap(source->rememberPropertySourceNameThenFlatPropertiesNames(source, propertySourceNames))
                .distinct()
                .filter(nonNullPropertyNameFilterWithPrefix)
                .forEach(key-> resolveValueThenAppendToDisplay(key, stringWithPropertiesToDisplay, propertiesWithHiddenValues, environement));

        stringWithPropertiesToDisplay
                .append(SEPARATION_LINE)
                .insert(0, System.lineSeparator())
                .insert(0,"                                     ====");
        insertPropretySourceNamesOnePerLine(propertySourceNames, stringWithPropertiesToDisplay);
        stringWithPropertiesToDisplay.insert(0,"""
                
                                        Values of properties from sources :
                """
        )
                .insert(0, SEPARATION_LINE)
                .insert(0, System.lineSeparator());


        log.info(stringWithPropertiesToDisplay::toString);

    }

    private void insertPropretySourceNamesOnePerLine(Set<String> propertySourceNames, StringBuilder stringWithPropertiesToDisplay) {
        propertySourceNames.forEach(name->stringWithPropertiesToDisplay.insert(0, System.lineSeparator()).insert(0, name).insert(0, "- "));
    }

    private boolean willBeProcessed(PropertySource<?> propertySource, Set<String> ignoredPropertySources) {
        return isEnumerable(propertySource) && isNotIgnored(propertySource, ignoredPropertySources);
    }

    private boolean isNotIgnored(PropertySource<?> propertySource, Set<String> ignoredPropertySources) {
        if (ignoredPropertySources.contains(propertySource.getName())){
            log.debug(()->propertySource+ " is listed to be ignored");
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
            return "******";
        }
        return environement.getProperty(key);
    }

    private Stream<String> rememberPropertySourceNameThenFlatPropertiesNames(PropertySource<?> propertySource, Set<String> propertySourceNames) {
        propertySourceNames.add(propertySource.getName());
        return Arrays.stream(((EnumerablePropertySource<?>)propertySource).getPropertyNames());
    }

    private boolean isEnumerable(PropertySource<?> propertySource) {
        if (! (propertySource instanceof EnumerablePropertySource)){
            log.debug(()->propertySource+ " is not EnumerablePropertySource : unable to list");
            return false;
        }
        return true;
    }

    private Set<String> getPropertyOrDefaultAndTrace(Environment environement, String key, Set<String> defaultValue) {
        Set<String> result = environement.getProperty(key, Set.class, defaultValue);
        log.trace(()->key+" -> "+result);
        return result;
    }

    private record PropertyNameNonNullFilterWithPrefix(Set<String> prefixes) implements Predicate<String>{

        @Override
        public boolean test(String key){
            log.trace(()->"Check if property "+key+" can be displayed");
            if(key==null){
                return false;
            }
            for(String prefix:prefixes){
                if(key.startsWith(prefix)){
                    return true;
                }
            }
            log.debug(()->key+ " doesn't start with a logable prefix");
           return false;
        }
    }
}
