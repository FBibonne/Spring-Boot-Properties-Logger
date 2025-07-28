package fr.insee.boot;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.*;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record EnvironmentPreparedEventForPropertiesLogging() implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final LocalLogger log = new LocalLogger(EnvironmentPreparedEventForPropertiesLogging.class);

    private static final PropertiesWithHiddenValues DEFAULT_PROPS_WITH_HIDDEN_VALUES = new PropertiesWithHiddenValues(Set.of("password", "pwd", "token", "secret", "credential", "pw"));
    private static final AllowedPrefixForProperties DEFAULT_PREFIX_FOR_PROPERTIES = new AllowedPrefixForProperties(Set.of("debug", "trace", "info", "logging", "spring", "server", "management", "springdoc", "properties"));
    private static final IgnoredPropertySources DEFAULT_SOURCES_IGNORED = new IgnoredPropertySources(Set.of(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME));
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
        final PropertiesWithHiddenValues propertiesWithHiddenValues = getPropertyOrDefaultAndTrace(abstractEnvironment, KEY_FOR_PROPS_WITH_HIDDEN_VALUES, PropertiesWithHiddenValues.class, DEFAULT_PROPS_WITH_HIDDEN_VALUES);
        final AllowedPrefixForProperties allowedPrefixForProperties = getPropertyOrDefaultAndTrace(abstractEnvironment, KEY_FOR_PREFIX_FOR_PROPERTIES, AllowedPrefixForProperties.class, DEFAULT_PREFIX_FOR_PROPERTIES);
        final IgnoredPropertySources ignoredPropertySources = getPropertyOrDefaultAndTrace(abstractEnvironment, KEY_FOR_SOURCES_IGNORED, IgnoredPropertySources.class, DEFAULT_SOURCES_IGNORED);

        PropertiesLogger propertiesLogger = new PropertiesLogger(propertiesWithHiddenValues, allowedPrefixForProperties, ignoredPropertySources);
        propertiesLogger.doLogProperties(abstractEnvironment);
    }

    private <T> T getPropertyOrDefaultAndTrace(PropertyResolver environment, String key, Class<T> clazz, T defaultValue) {
        try {
            T result = environment.getProperty(key, clazz, defaultValue);
            log.trace(() -> key + " -> " + result);
            return result;
        } catch (Exception e) {
            log.info(()-> "Error while getting property " + key + " : " + e.getMessage()+System.lineSeparator()+"Will use default value");
            return defaultValue;
        }
    }

    static final class CustomAbstractEnvironment implements PropertyResolver {
        private static final Method getPropertyResolver = resolveMethod(AbstractEnvironment.class, "getPropertyResolver");
        private static final Method getPropertyAsRawString = resolveMethod(AbstractPropertyResolver.class, "getPropertyAsRawString", String.class);
        
        private final ConfigurableEnvironment delegate;
        private AbstractPropertyResolver propertyResolver;


        private static Method resolveMethod(Class<?> targetType, String methodName, Class<?>... parameterTypes) {
            Method method = Objects.requireNonNull(ReflectionUtils.findMethod(targetType, methodName, parameterTypes));
            ReflectionUtils.makeAccessible(method);
            return method;
        }

        private CustomAbstractEnvironment(ConfigurableEnvironment delegate) {
            this.delegate = delegate;
        }

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
        public String getProperty(String key) {
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

        private String getPropertyAsRawString(String key) {
            return invokeGetPropertyAsRawString(key);
        }

        private String invokeGetPropertyAsRawString(String key) {
                  if (this.propertyResolver == null) {
                this.propertyResolver = invokeGetPropertyResolver(this.delegate);
            }
            return (String) ReflectionUtils.invokeMethod(getPropertyAsRawString, this.propertyResolver, key);
        }
    }
}
