package fr.insee.boot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public interface LocalLogger {

    Logger log = LoggerFactory.getLogger(LocalLogger.class);

    static LocalLogger of(Class<?> clazz) {
        return new LocalLogger() {

            private final Logger logger = LoggerFactory. getLogger(clazz);

            @Override
            public void debug(Supplier<String> message) {
                logger.atDebug().log(message);
            }

            @Override
            public void trace(Supplier<String> message) {
                logger.atDebug().log(message);
            }

            @Override
            public void info(Supplier<String> message) {
                log.atInfo().log(message);
            }
        };
    }

    void debug(Supplier<String> message);

    void trace(Supplier<String> message);

    void info(Supplier<String> message);
}
