package io.github.fbibonne.springaddons.boot.propertieslogger;

import org.springframework.core.env.PropertySource;

import java.util.Set;
import java.util.stream.Stream;

record IgnoredPropertySources(Set<String> sources) {
    private Stream<String> stream() {
        return sources.stream();
    }

    /**
     * Return true if the library is configured to ignore the propertySource passed as a parameter.
     * <br/>
     * The propertySource must be ignored if its name is listed in the configuration property whose key is
     * {@link EnvironmentPreparedEventForPropertiesLogging#KEY_FOR_SOURCES_IGNORED} (default names are
     * {@link EnvironmentPreparedEventForPropertiesLogging#DEFAULT_SOURCES_IGNORED}
     * @param propertySource : propertySource to be ignored or not
     * @return true if propertySource must be ignored
     */
    public boolean isIgnored(PropertySource<?> propertySource) {
        return stream().anyMatch(propertySource.getName()::contains);
    }
}
