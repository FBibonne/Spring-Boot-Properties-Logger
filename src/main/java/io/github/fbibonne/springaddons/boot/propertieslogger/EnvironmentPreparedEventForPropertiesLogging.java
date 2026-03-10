package io.github.fbibonne.springaddons.boot.propertieslogger;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.StandardEnvironment;

import java.util.Optional;
import java.util.Set;

/**
 * Spring ApplicationListener which triggers on {@link ApplicationEnvironmentPreparedEvent} to start properties logging process.
 * If the logging is enabled (with property {@code properties.logger.disabled} at {@code false} (which is default value) ) and the
 * environment associated with the applicationEnvironmentPreparedEvent is an {@link ConfigurableEnvironment}, then the listener
 * will log properties using provided configuration. The responsibility of this class is only to trigger the process and collect the
 * configuration for logging properties (properties starting with {@code properties.logger}) in the environment. It delegates the
 * logging process to an instance of {@link PropertiesLogger}
 */
public record EnvironmentPreparedEventForPropertiesLogging() implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final LocalLogger log = new LocalLogger(EnvironmentPreparedEventForPropertiesLogging.class);

    private static final Set<String> DEFAULT_PROPS_WITH_HIDDEN_VALUES = Set.of("password", "pwd", "token", "secret", "credential", "pw");
    private static final Set<String> DEFAULT_PREFIX_FOR_PROPERTIES = Set.of("debug", "trace", "info", "logging", "spring", "server", "management", "springdoc", "properties");
    public static final Set<String> DEFAULT_SOURCES_IGNORED = Set.of(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    private static final boolean DEFAULT_PROPERTIES_LOGGER_DISABLED = false;

    private static final String KEY_FOR_PROPS_WITH_HIDDEN_VALUES = "properties.logger.with-hidden-values";
    private static final String KEY_FOR_PREFIX_FOR_PROPERTIES = "properties.logger.prefix-for-properties";
    public static final String KEY_FOR_SOURCES_IGNORED = "properties.logger.sources-ignored";
    public static final String KEY_FOR_DISABLED = "properties.logger.disabled";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        final Environment environment = event.getEnvironment();
        if (loggingDisabled(environment)) {
            log.debug(() -> "PropertiesLogger is disabled");
            return;
        }
        configurableEnvironment(environment).ifPresent(this::doLogProperties);
    }

    private Optional<ConfigurableEnvironment> configurableEnvironment(Environment environment) {
        if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
            return Optional.of(configurableEnvironment);
        }
        log.info(()->"Environment "+environment+" is not instance of ConfigurableEnvironment : PropertiesLogger WILL NOT LOG PROPERTIES");
        return Optional.empty();
    }

    private boolean loggingDisabled(Environment environment) {
        return getPropertyOrDefaultAndTrace(environment, KEY_FOR_DISABLED, boolean.class, DEFAULT_PROPERTIES_LOGGER_DISABLED);
    }

    private void doLogProperties(ConfigurableEnvironment configurableEnvironment) {
        log.debug(() -> "Starting PropertiesLogger on ApplicationEnvironmentPreparedEvent");
        log.trace(() -> "Collecting properties to configure PropertiesLogger");

        configurableEnvironment.setIgnoreUnresolvableNestedPlaceholders(true);
        final PropertiesWithHiddenValues propertiesWithHiddenValues = new PropertiesWithHiddenValues(getPropertyOrDefaultAndTrace(configurableEnvironment, KEY_FOR_PROPS_WITH_HIDDEN_VALUES, Set.class, DEFAULT_PROPS_WITH_HIDDEN_VALUES));
        final AllowedPrefixForProperties allowedPrefixForProperties = new AllowedPrefixForProperties(getPropertyOrDefaultAndTrace(configurableEnvironment, KEY_FOR_PREFIX_FOR_PROPERTIES, Set.class, DEFAULT_PREFIX_FOR_PROPERTIES));
        final IgnoredPropertySources ignoredPropertySources = new IgnoredPropertySources(getPropertyOrDefaultAndTrace(configurableEnvironment, KEY_FOR_SOURCES_IGNORED,  Set.class, DEFAULT_SOURCES_IGNORED));

        PropertiesLogger propertiesLogger = new PropertiesLogger(propertiesWithHiddenValues, allowedPrefixForProperties, ignoredPropertySources, configurableEnvironment);
        propertiesLogger.doLogProperties();
    }

    private <T> T getPropertyOrDefaultAndTrace(PropertyResolver environment, String key, Class<T> clazz, T defaultValue) {
        T result;
        try {
            result = getPropertyOrDefaultAndTraceThrowingException(environment, key, clazz, defaultValue);
        } catch (RuntimeException e) {
            logExceptionWhenGettingProperty(key, e);
            result= defaultValue;
        }
        return result;
    }

    private static void logExceptionWhenGettingProperty(String key, RuntimeException e) {
        log.info(()-> "Error while getting property " + key + " : " + e.getMessage()+System.lineSeparator()+"Will use default value");
    }

    private static <T>  T getPropertyOrDefaultAndTraceThrowingException(PropertyResolver environment, String key, Class<T> clazz, T defaultValue) throws RuntimeException{
        T result = environment.getProperty(key, clazz, defaultValue);
        log.trace(() -> key + " -> " + result);
        return result;
    }

}
