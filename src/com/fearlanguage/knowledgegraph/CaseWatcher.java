/*
 * FEAR Knowledge Graph module for Autopsy.
 */
package com.fearlanguage.knowledgegraph;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.autopsy.ingest.IngestServices;

/**
 * Watches for case open/close events and creates or tears down the per-case
 * {@link KnowledgeGraphController} accordingly. Tearing the controller down on
 * case close unregisters its event listeners and removes it from the per-case
 * registry, preventing controllers (and their listeners) from accumulating
 * across cases.
 */
public class CaseWatcher implements PropertyChangeListener {

    private static CaseWatcher caseWatcherInstance = null;

    public static CaseWatcher getInstance() {
        if (caseWatcherInstance == null) {
            caseWatcherInstance = new CaseWatcher();
        }
        return caseWatcherInstance;
    }

    private CaseWatcher() {
        Case.addEventSubscriber(Case.Events.CURRENT_CASE.toString(), this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!Case.Events.CURRENT_CASE.toString().equals(evt.getPropertyName())) {
            return;
        }
        Logger logger = IngestServices.getInstance().getLogger(CaseWatcher.class.getName());
        if (evt.getNewValue() != null) {
            // A case was opened.
            Case openedCase = (Case) evt.getNewValue();
            logger.log(Level.INFO, "Case opened: {0}", openedCase.getName()); //NON-NLS
            KnowledgeGraphController.createController(openedCase);
        } else if (evt.getOldValue() != null) {
            // A case was closed.
            Case closedCase = (Case) evt.getOldValue();
            logger.log(Level.INFO, "Case closed: {0}", closedCase.getName()); //NON-NLS
            KnowledgeGraphController.shutDownController(closedCase);
        }
    }
}
