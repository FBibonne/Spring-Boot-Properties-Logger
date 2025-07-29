package io.github.fbibonne.springaddons.boot.propertieslogger;

import java.util.Set;
import java.util.stream.Stream;

record IgnoredPropertySources(Set<String> sources) {
    public Stream<String> stream() {
        return sources.stream();
    }
}
