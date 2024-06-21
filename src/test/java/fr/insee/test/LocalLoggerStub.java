package fr.insee.test;

import fr.insee.boot.LocalLogger;

import java.util.function.Supplier;

public record LocalLoggerStub(StringBuilder logs) implements LocalLogger {

    public LocalLoggerStub(){
        this(new StringBuilder());
    }

    @Override
    public void debug(Supplier<String> message) {
        appendToLogs("[DEBUG]", message);
    }

    private void appendToLogs(String level, Supplier<String> message) {
        logs.append(level).append(" ").append(message.get()).append("\n");
    }

    @Override
    public void trace(Supplier<String> message) {
        appendToLogs("[TRACE]", message);
    }

    @Override
    public void info(Supplier<String> message) {
        appendToLogs("[INFO]", message);
    }
}
