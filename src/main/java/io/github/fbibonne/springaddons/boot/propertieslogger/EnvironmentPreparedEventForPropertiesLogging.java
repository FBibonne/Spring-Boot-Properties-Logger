package io.github.fbibonne.springaddons.boot.propertieslogger;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.*;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Objects;
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
    private static final Set<String> DEFAULT_SOURCES_IGNORED = Set.of(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    private static final boolean DEFAULT_PROPERTIES_LOGGER_DISABLED = false;

    private static final String KEY_FOR_PROPS_WITH_HIDDEN_VALUES = "properties.logger.with-hidden-values";
    private static final String KEY_FOR_PREFIX_FOR_PROPERTIES = "properties.logger.prefix-for-properties";
    private static final String KEY_FOR_SOURCES_IGNORED = "properties.logger.sources-ignored";
    public static final String KEY_FOR_DISABLED = "properties.logger.disabled";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        final Environment environment = event.getEnvironment();
        if (loggingDisabled(environment)) {
            log.debug(() -> "PropertiesLogger is disabled");
            return;
        }
        abstractEnvironment(environment).ifPresent(this::doLogProperties);
    }

    private Optional<CustomAbstractEnvironment> abstractEnvironment(Environment environment) {
        if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
            return Optional.of(new CustomAbstractEnvironment(configurableEnvironment));
        }
        log.info(()->"Environment "+environment+" is not instance of ConfigurableEnvironment : PropertiesLogger WILL NOT LOG PROPERTIES");
        return Optional.empty();
    }

    private boolean loggingDisabled(Environment environment) {
        return getPropertyOrDefaultAndTrace(environment, KEY_FOR_DISABLED, boolean.class, DEFAULT_PROPERTIES_LOGGER_DISABLED);
    }

    private void doLogProperties(CustomAbstractEnvironment abstractEnvironment) {
        log.debug(() -> "Starting PropertiesLogger on ApplicationEnvironmentPreparedEvent");
        log.trace(() -> "Collecting properties to configure PropertiesLogger");
        final PropertiesWithHiddenValues propertiesWithHiddenValues = new PropertiesWithHiddenValues(getPropertyOrDefaultAndTrace(abstractEnvironment, KEY_FOR_PROPS_WITH_HIDDEN_VALUES, Set.class, DEFAULT_PROPS_WITH_HIDDEN_VALUES));
        final AllowedPrefixForProperties allowedPrefixForProperties = new AllowedPrefixForProperties(getPropertyOrDefaultAndTrace(abstractEnvironment, KEY_FOR_PREFIX_FOR_PROPERTIES, Set.class, DEFAULT_PREFIX_FOR_PROPERTIES));
        final IgnoredPropertySources ignoredPropertySources = new IgnoredPropertySources(getPropertyOrDefaultAndTrace(abstractEnvironment, KEY_FOR_SOURCES_IGNORED,  Set.class, DEFAULT_SOURCES_IGNORED));

        PropertiesLogger propertiesLogger = new PropertiesLogger(propertiesWithHiddenValues, allowedPrefixForProperties, ignoredPropertySources);
        propertiesLogger.doLogProperties(abstractEnvironment);
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

    static final class CustomAbstractEnvironment implements PropertyResolver {
        private static final Method getPropertyResolver = resolveMethod(AbstractEnvironment.class, "getPropertyResolver");
        private static final Method getPropertyAsRawString = resolveMethod(AbstractPropertyResolver.class, "getPropertyAsRawString", String.class);
        
        private final ConfigurableEnvironment delegate;
        @Nullable
        private AbstractPropertyResolver propertyResolver;


        private static Method resolveMethod(Class<?> targetType, String methodName, Class<?>... parameterTypes) {
            Method method = Objects.requireNonNull(ReflectionUtils.findMethod(targetType, methodName, parameterTypes));
            ReflectionUtils.makeAccessible(method);
            return method;
        }

        private CustomAbstractEnvironment(ConfigurableEnvironment delegate) {
            this.delegate = delegate;
        }

        @Nullable
        private AbstractPropertyResolver invokeGetPropertyResolver(ConfigurableEnvironment delegate) {
            if (delegate instanceof AbstractEnvironment abstractEnvironment) {
                var abstractPropertyResolverCondidate = ReflectionUtils.invokeMethod(getPropertyResolver, abstractEnvironment);
                if (abstractPropertyResolverCondidate instanceof AbstractPropertyResolver abstractPropertyResolver) {
                    return abstractPropertyResolver;
                }
            }
            return null;
        }

        @Override
        public boolean containsProperty(String key) {
            return delegate.containsProperty(key);
        }

        @Override
        public @Nullable String getProperty(String key) {
            return delegate.getProperty(key);
        }

        @Override
        public String getProperty(String key, String defaultValue) {
            return delegate.getProperty(key, defaultValue);
        }

        @Override
        public <T> T getProperty(String key, Class<T> targetType) {
            return delegate.getProperty(key, targetType);
        }

        @Override
        public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
            return delegate.getProperty(key, targetType, defaultValue);
        }

        @Override
        public String getRequiredProperty(String key) throws IllegalStateException {
            return delegate.getRequiredProperty(key);
        }

        @Override
        public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
            return delegate.getRequiredProperty(key, targetType);
        }

        @Override
        public String resolvePlaceholders(String text) {
            return delegate.resolvePlaceholders(text);
        }

        @Override
        public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
            return delegate.resolveRequiredPlaceholders(text);
        }

        public MutablePropertySources getPropertySources() {
            return delegate.getPropertySources();
        }

        @Nullable
        public String getPropertySafely(String key) {
            try{
                return delegate.getProperty(key);
            }catch (IllegalArgumentException e) {
                // IllegalArgumentException thrown when unresolved placeholder occurs
                return getPropertyAsRawString(key);
            } catch (Exception e) {
                return "Error while getting property " + key + " : " + e.getMessage();
            }
        }

        @Nullable
        private String getPropertyAsRawString(String key) {
            return invokeGetPropertyAsRawString(key);
        }

        @Nullable
        private String invokeGetPropertyAsRawString(String key) {
                  if (this.propertyResolver == null) {
                this.propertyResolver = invokeGetPropertyResolver(this.delegate);
            }
            return (String) ReflectionUtils.invokeMethod(getPropertyAsRawString, this.propertyResolver, key);
        }
    }
}
