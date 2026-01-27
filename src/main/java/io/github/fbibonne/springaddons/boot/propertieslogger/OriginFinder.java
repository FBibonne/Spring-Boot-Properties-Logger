package io.github.fbibonne.springaddons.boot.propertieslogger;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyNameException;
import org.springframework.core.env.MutablePropertySources;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record OriginFinder(List<ConfigurationPropertySource> configurationPropertySources) {

    public OriginFinder(MutablePropertySources propertySources) {
        this(propertySources.stream()
                .map(ConfigurationPropertySource::from)
                .filter(Objects::nonNull)
                .toList());
    }

    public Optional<String> findOriginFor(String key) {
        try {
            final ConfigurationPropertyName configurationPropertyName = ConfigurationPropertyName.of(key);
            return configurationPropertySources.stream()
                    .map(c -> c.getConfigurationProperty(configurationPropertyName))
                    .filter(Objects::nonNull)
                    .map(ConfigurationProperty::getOrigin)
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map("FROM "::concat)
                    .findFirst();
        } catch (InvalidConfigurationPropertyNameException e) {
            return Optional.of("WARNING ! "+e.getMessage() + " : see org.springframework.boot.context.properties.source.ConfigurationPropertyName");
        }
    }
}
