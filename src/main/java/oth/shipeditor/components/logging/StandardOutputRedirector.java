package oth.shipeditor.components.logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.PrintStream;

@Log4j2
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2", "MS_EXPOSE_REP"})
public final class StandardOutputRedirector {

    @Getter
    private static PrintStream outStreamProxy;

    @Getter
    private static PrintStream errorStreamProxy;

    private StandardOutputRedirector() {}

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void redirectStandardStreams() {
        PrintStream out = System.out;
        outStreamProxy = StandardOutputRedirector.createLoggingProxy(out);
        System.setOut(outStreamProxy);

        PrintStream err = System.err;
        errorStreamProxy = StandardOutputRedirector.createLoggingProxy(err);
        System.setErr(errorStreamProxy);
    }

    @SuppressWarnings("ImplicitDefaultCharsetUsage")
    private static PrintStream createLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
            public void print(final String s) {
                realPrintStream.print(s);
                LogsPanel.append(s + System.lineSeparator());
            }
        };
    }

}
