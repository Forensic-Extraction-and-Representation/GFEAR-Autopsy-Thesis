/*
 * FEAR Knowledge Graph module for Autopsy.
 */
package com.fearlanguage.knowledgegraph;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.coreutils.Logger;

/**
 * Reads and writes the module's settings as a {@code .properties} file inside the
 * <em>case directory</em>, so that configuration (FEAR endpoints, OAuth details,
 * case name, etc.) travels with the case rather than living in the global
 * NetBeans userdir.
 *
 * <p>The file is stored at
 * {@code <caseDirectory>/gfear-autopsy/knowledgegraph-settings.properties}.</p>
 *
 * <p>This class is intentionally small and stateless; callers (see
 * {@link KnowledgeGraphPreferences}) decide how to combine per-case values with
 * global defaults.</p>
 */
public final class PerCaseProperties {

    private static final Logger logger = Logger.getLogger(PerCaseProperties.class.getName());

    private static final String SETTINGS_DIR = "gfear-autopsy"; //NON-NLS
    private static final String SETTINGS_FILE = "knowledgegraph-settings.properties"; //NON-NLS

    private PerCaseProperties() {
    }

    private static Path settingsPath(Case theCase) {
        return Paths.get(theCase.getCaseDirectory(), SETTINGS_DIR, SETTINGS_FILE);
    }

    /**
     * Loads the per-case settings for the given case. Returns an empty
     * {@link Properties} if the file does not yet exist or cannot be read.
     */
    public static synchronized Properties load(Case theCase) {
        Properties props = new Properties();
        Path path = settingsPath(theCase);
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to read per-case settings from " + path, ex); //NON-NLS
            }
        }
        return props;
    }

    /**
     * Persists the given settings to the case directory, creating the containing
     * folder if necessary.
     */
    public static synchronized void store(Case theCase, Properties props) {
        Path path = settingsPath(theCase);
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) {
                props.store(out, "FEAR Knowledge Graph per-case settings"); //NON-NLS
            }
        } catch (IOException ex) {
            Diagnostics.reportError("Failed to write per-case settings to " + path, ex);
        }
    }

    /**
     * Gets a single per-case value.
     *
     * @param theCase The case.
     * @param key     The setting key.
     * @param def     Value to return when the key is absent (may be {@code null}).
     *
     * @return The stored value, or {@code def} when absent.
     */
    public static String get(Case theCase, String key, String def) {
        return load(theCase).getProperty(key, def);
    }

    /**
     * Sets a single per-case value, preserving the other settings already stored.
     */
    public static void set(Case theCase, String key, String value) {
        Properties props = load(theCase);
        if (value == null) {
            props.remove(key);
        } else {
            props.setProperty(key, value);
        }
        store(theCase, props);
    }
}
