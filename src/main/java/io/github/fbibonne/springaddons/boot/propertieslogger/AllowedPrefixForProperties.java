package io.github.fbibonne.springaddons.boot.propertieslogger;

import java.util.Set;
import java.util.function.Predicate;

record AllowedPrefixForProperties(Set<String> prefixes) {

    public boolean anyMatch(Predicate<String> matcher) {
        return prefixes.stream().anyMatch(matcher);
    }
}
