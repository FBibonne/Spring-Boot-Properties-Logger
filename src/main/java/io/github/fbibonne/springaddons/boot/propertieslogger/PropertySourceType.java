package io.github.fbibonne.springaddons.boot.propertieslogger;

import org.springframework.boot.ansi.AnsiPropertySource;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.jndi.JndiPropertySource;

public enum PropertySourceType {
    ANSI_PROPERTY_SOURCE,
    STUB_PROPERTY_SOURCE, RANDOM_VALUE_PROPRERTY_SOURCE, JDNI_PROPERTY_SOURCE_TYPE, ENUMERABLE_PROPERTY_SOURCE,
    ORIGIN_FINDER_ORIGIN_LOOKUP_PROPERTY_SOURCE, SIMPLE_ORIGIN_LOOKUP_PROPERTY_SOURCE, UNKNOWN;

    private static final LocalLogger log = new LocalLogger(PropertySourceType.class);

    public static PropertySourceType of(PropertySource<?> propertySource) {
        PropertySourceType propertySourceType;
        if (propertySource instanceof AnsiPropertySource) {
            propertySourceType = ANSI_PROPERTY_SOURCE;
        } else if (propertySource instanceof PropertySource.StubPropertySource) {
            propertySourceType = STUB_PROPERTY_SOURCE;
        } else if (propertySource instanceof RandomValuePropertySource) {
            propertySourceType = RANDOM_VALUE_PROPRERTY_SOURCE;
        } else if (propertySource instanceof JndiPropertySource){
            propertySourceType = JDNI_PROPERTY_SOURCE_TYPE;
        } else if (propertySource instanceof EnumerablePropertySource<?>){
            propertySourceType = ENUMERABLE_PROPERTY_SOURCE;
        } else if (propertySource instanceof OriginLookup<?>) {
            propertySourceType = ofOriginLookup((OriginLookup<?>) propertySource);
        } else {
            propertySourceType = UNKNOWN;
        }
        propertySourceType.log(propertySource);
        return propertySourceType;
    }

    private static PropertySourceType ofOriginLookup(OriginLookup<?> originLookupPropertySource) {
        if ("org.springframework.boot.context.properties.source.ConfigurationPropertySourcesPropertySource".equals(originLookupPropertySource.getClass().getCanonicalName())){
            return ORIGIN_FINDER_ORIGIN_LOOKUP_PROPERTY_SOURCE;
        }
        return SIMPLE_ORIGIN_LOOKUP_PROPERTY_SOURCE;
    }

    private void log(PropertySource<?> propertySource) {
        switch (this){
            case ANSI_PROPERTY_SOURCE -> log.warn(()->"Processing of AnsiPropertySource "+ propertySource +" not yet implemented : properties exclusively from this property source will be ignored");
            case STUB_PROPERTY_SOURCE -> log.debug(() -> propertySource + " is a stub property source : it does not contain properties : will be ignored");
            case RANDOM_VALUE_PROPRERTY_SOURCE -> log.debug(()-> propertySource + " is a RandomValuePropertySource : will be ignored");
            case JDNI_PROPERTY_SOURCE_TYPE -> log.warn(()-> propertySource + " is a JndiPropertySource : it is not enumerable : will be ignored");
            case ENUMERABLE_PROPERTY_SOURCE -> log.trace(() -> propertySource + " is a EnumerablePropertySource : is a candidate to find keys");
            case ORIGIN_FINDER_ORIGIN_LOOKUP_PROPERTY_SOURCE -> log.debug(()-> propertySource + " will be used as the originFinder but will be ignored as a property source");
            case SIMPLE_ORIGIN_LOOKUP_PROPERTY_SOURCE -> log.debug(()-> propertySource + " will be ignored as a property source and also as OriginLookup");
            case UNKNOWN -> log.warn(()-> propertySource + " is unknown : will be ignored");
            }
        }
    }

    /*
                this.originFinder=key->{
                var origin=originLookup.getOrigin(key);
                return origin==null?"":" ### FROM "+ origin+" ###";
            };
     */
    public boolean isOriginFinder(){
        return switch (this){
            case ORIGIN_FINDER_ORIGIN_LOOKUP_PROPERTY_SOURCE -> true;
            case ANSI_PROPERTY_SOURCE, STUB_PROPERTY_SOURCE, ENUMERABLE_PROPERTY_SOURCE, JDNI_PROPERTY_SOURCE_TYPE,
                 RANDOM_VALUE_PROPRERTY_SOURCE, SIMPLE_ORIGIN_LOOKUP_PROPERTY_SOURCE, -> false;
        };
    }

    public boolean isEnumerable() {
        return switch (this){
            case ANSI_PROPERTY_SOURCE, STUB_PROPERTY_SOURCE, RANDOM_VALUE_PROPRERTY_SOURCE, JDNI_PROPERTY_SOURCE_TYPE,
                 ORIGIN_FINDER_ORIGIN_LOOKUP_PROPERTY_SOURCE, SIMPLE_ORIGIN_LOOKUP_PROPERTY_SOURCE -> false;
            case ENUMERABLE_PROPERTY_SOURCE -> true;
        };
    }
}
