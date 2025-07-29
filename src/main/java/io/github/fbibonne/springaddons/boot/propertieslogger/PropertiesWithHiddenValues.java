package io.github.fbibonne.springaddons.boot.propertieslogger;

import java.util.Set;
import java.util.stream.Stream;

record PropertiesWithHiddenValues(Set<String> properties) {
    public Stream<String> stream() {
        return properties.stream();
    }
}
