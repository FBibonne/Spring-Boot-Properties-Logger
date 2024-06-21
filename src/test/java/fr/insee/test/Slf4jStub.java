package fr.insee.test;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.simple.SimpleLogger;
import org.slf4j.spi.LocationAwareLogger;

public class Slf4jStub extends SimpleLogger {

    private final StringBuilder stringBuilder = new StringBuilder();

    protected Slf4jStub(String name) {
        super(name);
        super.currentLogLevel = LocationAwareLogger.INFO_INT;
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
        stringBuilder.append('[')
                .append(renderLevel(level.toInt()))
                .append("] ")
                .append(MessageFormatter.basicArrayFormat(messagePattern, arguments))
                .append(System.lineSeparator());
    }

    public StringBuilder getStringBuilder() {
        return stringBuilder;
    }
}
