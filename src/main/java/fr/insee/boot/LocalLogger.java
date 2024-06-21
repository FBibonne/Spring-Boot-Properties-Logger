package fr.insee.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public record LocalLogger(Logger logger) {

    public LocalLogger(Class<?> clazz) {
        this(LoggerFactory.getLogger(clazz));
    }

    public void debug(Supplier<String> message) {
        logger.atDebug().log(message);
    }

    public void trace(Supplier<String> message) {
        logger.atDebug().log(message);
    }

    public void info(Supplier<String> message) {
        logger.atInfo().log(message);
    }

}
