/*
 * FEAR Knowledge Graph module for Autopsy.
 */
package com.fearlanguage.knowledgegraph;

import javax.annotation.Nonnull;
import org.openide.util.NbBundle;
import org.sleuthkit.autopsy.casemodule.Case;

/**
 * Tracks module-level state for the FEAR Knowledge Graph module.
 */
@NbBundle.Messages({"KnowledgeGraphModule.moduleName=Knowledge Graph"})
public final class KnowledgeGraphModule {

    private static final String MODULE_NAME = Bundle.KnowledgeGraphModule_moduleName();

    /**
     * Gets the knowledge graph module name.
     *
     * @return The module name.
     */
    public static String getModuleName() {
        return MODULE_NAME;
    }

    /**
     * Indicates whether the knowledge graph module is enabled for a given case.
     * The module is currently always enabled; this method is retained as the
     * hook for a future per-case enable/disable setting.
     *
     * @param theCase The case.
     *
     * @return True.
     */
    public static boolean isEnabledforCase(@Nonnull Case theCase) {
        return true;
    }

    private KnowledgeGraphModule() {
    }
}
