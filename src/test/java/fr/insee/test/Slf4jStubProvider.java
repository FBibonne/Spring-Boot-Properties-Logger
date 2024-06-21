package fr.insee.test;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.simple.SimpleLogger;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

import java.util.HashMap;
import java.util.Map;

public class Slf4jStubProvider implements SLF4JServiceProvider {

    public static String REQUESTED_API_VERSION = "2.0.99";

    private ILoggerFactory loggerFactory;
    private IMarkerFactory markerFactory;
    private MDCAdapter mdcAdapter;

    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    @Override
    public String getRequestedApiVersion() {
        return REQUESTED_API_VERSION;
    }

    @Override
    public void initialize() {

        loggerFactory = new ILoggerFactory() {
                final Map<String, Logger> loggerMap =  new HashMap<>();

                /**
                 * Return an appropriate {@link SimpleLogger} instance by name.
                 *
                 * This method will call {@link #createLogger(String)} if the logger
                 * has not been created yet.
                 */
                public Logger getLogger(String name) {
                    return loggerMap.computeIfAbsent(name, this::createLogger);
                }

                /**
                 * Actually creates the logger for the given name.
                 */
                protected Logger createLogger(String name) {
                    return new Slf4jStub(name);
                }

                /**
                 * Clear the internal logger cache.
                 *
                 * This method is intended to be called by classes (in the same package or
                 * subclasses) for testing purposes. This method is internal. It can be
                 * modified, renamed or removed at any time without notice.
                 *
                 * You are strongly discouraged from calling this method in production code.
                 */
                protected void reset() {
                    loggerMap.clear();
                }
            };
        markerFactory = new BasicMarkerFactory();
        mdcAdapter = new NOPMDCAdapter();
    }


}
