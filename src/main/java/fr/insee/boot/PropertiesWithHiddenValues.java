package fr.insee.boot;

import java.util.Set;
import java.util.stream.Stream;

public record PropertiesWithHiddenValues(Set<String> properties) {
    public Stream<String> stream() {
        return properties.stream();
    }
}
