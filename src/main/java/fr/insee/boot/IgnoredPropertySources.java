package fr.insee.boot;

import java.util.Set;
import java.util.stream.Stream;

public record IgnoredPropertySources(Set<String> sources) {
    public Stream<String> stream() {
        return sources.stream();
    }
}
