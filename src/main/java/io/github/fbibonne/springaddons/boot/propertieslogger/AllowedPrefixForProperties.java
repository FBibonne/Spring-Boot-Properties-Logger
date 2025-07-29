package io.github.fbibonne.springaddons.boot.propertieslogger;

import java.util.Iterator;
import java.util.Set;

record AllowedPrefixForProperties(Set<String> prefixes) implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
        return prefixes().iterator();
    }
}
