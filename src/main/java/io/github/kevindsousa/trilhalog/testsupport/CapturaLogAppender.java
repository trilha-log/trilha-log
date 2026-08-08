package io.github.kevindsousa.trilhalog.testsupport;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Appender de teste que se anexa ao root logger e captura eventos em memoria,
 * para asserir logs sem depender de captura de stdout.
 * <p>
 * Nao da pra confiar no nivel ambiente do root logger: como os testes rodam
 * todos no mesmo processo Surefire, o nivel efetivo depende de qual teste
 * rodou antes (pode ter deixado o contexto em WARN). Por isso {@link #anexar()}
 * forca {@code Level.ALL} e {@link #desanexar()} restaura o nivel original
 * salvo, chamando {@code LoggerContext.updateLoggers()} apos cada mudanca.
 */
public class CapturaLogAppender extends AbstractAppender {

    private final List<LogEvent> eventos = new CopyOnWriteArrayList<>();
    private Level nivelOriginal;

    public CapturaLogAppender() {
        super("CapturaLogAppender", null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY);
    }

    public void anexar() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        LoggerConfig rootConfig = config.getRootLogger();

        this.nivelOriginal = rootConfig.getLevel();
        start();
        rootConfig.addAppender(this, Level.ALL, null);
        rootConfig.setLevel(Level.ALL);
        context.updateLoggers();
    }

    public void desanexar() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        LoggerConfig rootConfig = config.getRootLogger();

        rootConfig.removeAppender(getName());
        if (nivelOriginal != null) {
            rootConfig.setLevel(nivelOriginal);
        }
        context.updateLoggers();
        stop();
        eventos.clear();
    }

    @Override
    public void append(LogEvent event) {
        eventos.add(event.toImmutable());
    }

    public List<LogEvent> getEventos() {
        return new ArrayList<>(eventos);
    }

    public boolean contemMensagem(String trecho) {
        return eventos.stream().anyMatch(e -> e.getMessage().getFormattedMessage().contains(trecho));
    }
}
