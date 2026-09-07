/*
 * FEAR Knowledge Graph module for Autopsy.
 */
package com.fearlanguage.knowledgegraph;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.sleuthkit.autopsy.coreutils.Logger;

/**
 * Lightweight diagnostics helper.
 *
 * <p>Logs to the Autopsy log and, when enabled, surfaces a message dialog. Its
 * purpose is to make failures visible when the module is run inside a
 * <em>packaged</em> Autopsy installation, where exceptions on the various
 * Swing/JCEF/JavaFX threads would otherwise be swallowed silently and leave the
 * UI in a half-initialised state (no login window, missing toolbar action,
 * etc.).</p>
 *
 * <p>Dialogs are gated by
 * {@link KnowledgeGraphPreferences#getShowDiagnosticDialogs()} (default on) so
 * they can be switched off once debugging is complete, without a rebuild.</p>
 */
public final class Diagnostics {

    private static final Logger logger = Logger.getLogger(Diagnostics.class.getName());

    private static final String BREADCRUMB_FILE =
            System.getProperty("user.home") + File.separator + "gfear-autopsy-diagnostic.log";

    private Diagnostics() {
    }

    /**
     * Appends a timestamped line to the breadcrumb file (and the Autopsy log).
     * Never throws. Use this to trace how far module start-up progresses when a
     * failure occurs before any dialog can be shown (class-load / static-init /
     * module-resolution failures).
     *
     * @param step Short description of the start-up step that was reached.
     */
    public static void breadcrumb(String step) {
        try {
            logger.log(Level.INFO, "[breadcrumb] {0}", step);
        } catch (Throwable ignored) {
            // logging must never break start-up tracing
        }
        try (PrintWriter out = new PrintWriter(new FileWriter(BREADCRUMB_FILE, true))) {
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
            out.println(ts + "  " + step);
        } catch (Throwable ignored) {
            // Best effort only; never let breadcrumb writing break the caller.
        }
    }

    /**
     * Logs an error and, when dialogs are enabled, shows it with the throwable's
     * full stack trace.
     *
     * @param context Human-readable description of what was being attempted.
     * @param t       The error (may be {@code null}).
     */
    public static void reportError(String context, Throwable t) {
        logger.log(Level.SEVERE, context, t);
        breadcrumb("ERROR: " + context + " :: " + (t == null ? "(no exception)" : t.toString()));
        if (!dialogsEnabled()) {
            return;
        }
        String details = context
                + "\n\n" + (t == null ? "(no exception)" : t.toString())
                + "\n\n" + stackTraceToString(t);
        showOnEdt("FEAR Knowledge Graph — Error", details, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Logs and, when dialogs are enabled, shows a warning message (no stack
     * trace). Useful for non-fatal conditions such as a missing resource.
     */
    public static void reportWarning(String message) {
        logger.log(Level.WARNING, message);
        breadcrumb("WARNING: " + message);
        if (!dialogsEnabled()) {
            return;
        }
        showOnEdt("FEAR Knowledge Graph — Warning", message, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Logs and, when dialogs are enabled, shows an informational message. Handy
     * for confirming that a given step on the start-up path was reached.
     */
    public static void reportInfo(String message) {
        logger.log(Level.INFO, message);
        if (!dialogsEnabled()) {
            return;
        }
        showOnEdt("FEAR Knowledge Graph", message, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows a normal user-facing message (always displayed, independent of the
     * diagnostic-dialog toggle, since this is guidance rather than debug output).
     */
    public static void userMessage(String title, String message) {
        logger.log(Level.INFO, "{0}: {1}", new Object[]{title, message});
        showOnEdt(title, message, JOptionPane.INFORMATION_MESSAGE);
    }

    private static boolean dialogsEnabled() {
        try {
            return KnowledgeGraphPreferences.getShowDiagnosticDialogs();
        } catch (Exception e) {
            // Never let the diagnostics path itself break the caller.
            return true;
        }
    }

    private static void showOnEdt(String title, String message, int type) {
        Runnable r = () -> {
            try {
                JTextArea area = new JTextArea(message);
                area.setEditable(false);
                area.setColumns(72);
                area.setRows(Math.min(24, Math.max(4, message.split("\n").length + 1)));
                JScrollPane scroll = new JScrollPane(area);
                JOptionPane.showMessageDialog(null, scroll, title, type);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to display diagnostic dialog", e); //NON-NLS
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private static String stackTraceToString(Throwable t) {
        if (t == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
