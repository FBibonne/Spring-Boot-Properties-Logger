package fr.insee.boot;

import java.util.Iterator;
import java.util.Set;

public record AllowedPrefixForProperties(Set<String> prefixes) implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
        return prefixes().iterator();
    }
}
