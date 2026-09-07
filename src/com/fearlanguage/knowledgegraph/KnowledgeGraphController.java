/*
 * FEAR Knowledge Graph module for Autopsy.
 */
package com.fearlanguage.knowledgegraph;

import com.fearlanguage.knowledgegraph.gui.KnowledgeGraphTopComponent;
import com.google.common.eventbus.Subscribe;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.swing.SwingUtilities;
import org.openide.util.Exceptions;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.events.ContentTagAddedEvent;
import org.sleuthkit.autopsy.casemodule.events.ContentTagDeletedEvent;
import org.sleuthkit.autopsy.casemodule.events.DataSourceDeletedEvent;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.autopsy.events.AutopsyEvent;
import org.sleuthkit.autopsy.ingest.IngestManager;
import org.sleuthkit.autopsy.ingest.ModuleDataEvent;
import org.sleuthkit.autopsy.ingest.events.DataSourceAnalysisEvent;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.Blackboard;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.BlackboardArtifact.Type;
import org.sleuthkit.datamodel.BlackboardAttribute;
import org.sleuthkit.datamodel.Content;
import org.sleuthkit.datamodel.DataSource;
import org.sleuthkit.datamodel.Image;
import org.sleuthkit.datamodel.SleuthkitCase;
import org.sleuthkit.datamodel.TskCoreException;
import org.sleuthkit.datamodel.TskDataException;

/**
 * Per-case controller for the FEAR Knowledge Graph module.
 *
 * <p>Responsibilities: maintain one controller instance per case; translate
 * Autopsy Blackboard artefacts into the self-describing attribute maps that the
 * FEAR Framework's GFEAR scripts consume; and submit them, in batches, to the
 * framework via {@link GFearBridge}.</p>
 *
 * <p>The case and ingest event listeners below are intentionally retained as the
 * foundation for <em>future automated ingestion</em> (forwarding artefacts to the
 * framework as they are produced during analysis). Submission is currently
 * user-triggered, so the listener bodies are largely placeholders marked
 * {@code TODO(future)}.</p>
 */
public final class KnowledgeGraphController {

    private static final Logger logger = Logger.getLogger(KnowledgeGraphController.class.getName());

    /** Maximum number of artefacts submitted to the FEAR Framework per request. */
    private static final int SUBMISSION_BATCH_SIZE = 500;

    /** Algorithm labels reported alongside a host hash in {@code HostHashType}. */
    private static final String HASH_TYPE_SHA1 = "SHA-1";
    private static final String HASH_TYPE_MD5 = "MD5";

    private static final Set<IngestManager.IngestJobEvent> INGEST_JOB_EVENTS_OF_INTEREST = EnumSet.of(
            IngestManager.IngestJobEvent.DATA_SOURCE_ANALYSIS_STARTED,
            IngestManager.IngestJobEvent.DATA_SOURCE_ANALYSIS_COMPLETED);
    private static final Set<IngestManager.IngestModuleEvent> INGEST_MODULE_EVENTS_OF_INTEREST = EnumSet.of(
            IngestManager.IngestModuleEvent.DATA_ADDED,
            IngestManager.IngestModuleEvent.FILE_DONE);
    private static final Set<Case.Events> CASE_EVENTS_OF_INTEREST = EnumSet.of(
            Case.Events.CURRENT_CASE,
            Case.Events.DATA_SOURCE_ADDED,
            Case.Events.CONTENT_TAG_ADDED,
            Case.Events.CONTENT_TAG_DELETED,
            Case.Events.DATA_SOURCE_DELETED);

    /*
     * One controller per case, created when case resources are opened and removed
     * when they are closed.
     */
    private static final Object controllersByCaseLock = new Object();
    private static final Map<String, KnowledgeGraphController> controllersByCase = new HashMap<>();

    private final Case theCase;
    private final SleuthkitCase caseDb;
    private final GFearBridge gfearBridge;

    /** When false, the case/ingest event listeners take no action. */
    private volatile boolean listeningEnabled;

    private final CaseEventListener caseEventListener = new CaseEventListener();
    private final IngestJobEventListener ingestJobEventListener = new IngestJobEventListener();
    private final IngestModuleEventListener ingestModuleEventListener = new IngestModuleEventListener();
    private volatile KnowledgeGraphTopComponent topComponent;

    /**
     * Creates the controller for a case, if one does not already exist.
     */
    static void createController(Case theCase) {
        synchronized (controllersByCaseLock) {
            if (!controllersByCase.containsKey(theCase.getName())) {
                KnowledgeGraphController controller = new KnowledgeGraphController(theCase);
                controller.startUp();
                controllersByCase.put(theCase.getName(), controller);
            }
        }
    }

    /**
     * Gets the controller for a case, creating it if necessary.
     */
    public static KnowledgeGraphController getController(Case theCase) {
        synchronized (controllersByCaseLock) {
            if (!controllersByCase.containsKey(theCase.getName())) {
                createController(theCase);
            }
            return controllersByCase.get(theCase.getName());
        }
    }

    /**
     * Removes and shuts down the controller for a case.
     */
    static void shutDownController(Case theCase) {
        KnowledgeGraphController controller;
        synchronized (controllersByCaseLock) {
            controller = controllersByCase.remove(theCase.getName());
        }
        if (controller != null) {
            controller.shutDown();
        }
    }

    KnowledgeGraphController(@Nonnull Case theCase) {
        this.theCase = Objects.requireNonNull(theCase);
        this.caseDb = theCase.getSleuthkitCase();
        this.listeningEnabled = KnowledgeGraphModule.isEnabledforCase(theCase);

        this.gfearBridge = new GFearBridge();
    }

    void startUp() {
        Case.addEventTypeSubscriber(CASE_EVENTS_OF_INTEREST, caseEventListener);
        IngestManager.getInstance().addIngestJobEventListener(INGEST_JOB_EVENTS_OF_INTEREST, ingestJobEventListener);
        IngestManager.getInstance().addIngestModuleEventListener(INGEST_MODULE_EVENTS_OF_INTEREST, ingestModuleEventListener);

        SwingUtilities.invokeLater(() -> topComponent = KnowledgeGraphTopComponent.getTopComponent());
    }

    public synchronized void shutDown() {
        logger.log(Level.INFO, "Shutting down knowledge graph controller for case {0}", theCase.getName()); //NON-NLS
        Case.removeEventTypeSubscriber(CASE_EVENTS_OF_INTEREST, caseEventListener);
        IngestManager.getInstance().removeIngestJobEventListener(ingestJobEventListener);
        IngestManager.getInstance().removeIngestModuleEventListener(ingestModuleEventListener);
    }

    public void setAccessToken(String token) {
        this.gfearBridge.setAccessToken(token);
    }

    /**
     * @return true if the module has authenticated against the FEAR Framework.
     */
    public boolean isAuthenticated() {
        return this.gfearBridge.isAuthenticated();
    }

    public Case getCase() {
        return theCase;
    }

    public SleuthkitCase getCaseDatabase() {
        return caseDb;
    }

    public void setListeningEnabled(boolean enabled) {
        this.listeningEnabled = enabled;
    }

    public boolean isListeningEnabled() {
        return listeningEnabled;
    }

    /**
     * Handles the SleuthKit Blackboard "artefacts posted" event (delivered while
     * the controller is registered for case events), forwarding the artefacts to
     * the FEAR Framework.
     */
    @Subscribe
    public void artifactsPostedEventHandler(Blackboard.ArtifactsPostedEvent event) {
        logger.log(Level.INFO, "Received {0} artefact(s) from {1}", new Object[]{event.getArtifacts().size(), event.getModuleName()}); //NON-NLS
        artifactsPostedEventHandler(event.getArtifacts());
    }

    /**
     * Collects every artefact in the case, across all artefact types currently in
     * use. Used by the "submit all categories" action.
     *
     * @return All Blackboard artefacts for the case.
     */
    public List<BlackboardArtifact> getAllArtifacts() throws TskCoreException {
        SleuthkitCase skc = getCaseDatabase();
        List<BlackboardArtifact> all = new ArrayList<>();
        for (Type type : skc.getArtifactTypesInUse()) {
            all.addAll(skc.getBlackboardArtifacts(type.getTypeName()));
        }
        return all;
    }

    /**
     * Splits a list into batches of at most {@code batchSize} elements.
     */
    public static <T> List<Collection<T>> getBatches(List<T> collection, int batchSize) {
        List<Collection<T>> batches = new ArrayList<>();
        int i = 0;
        while (i < collection.size()) {
            int nextInc = Math.min(collection.size() - i, batchSize);
            batches.add(collection.subList(i, i + nextInc));
            i += nextInc;
        }
        return batches;
    }

    /**
     * Translates the given artefacts and submits them to the FEAR Framework in
     * batches. Convenience entry point used by the event path; outcome and
     * progress are discarded.
     */
    public void artifactsPostedEventHandler(Collection<BlackboardArtifact> artifacts) {
        try {
            submitArtifacts(artifacts, null);
        } catch (TskCoreException | TskDataException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * Translates and submits the given artefacts to the FEAR Framework in
     * batches, reporting progress per batch and returning a summary outcome.
     *
     * @param artifacts The artefacts to submit.
     * @param listener  Optional progress listener; may be {@code null}.
     *
     * @return A summary of the submission outcome.
     */
    public SubmissionResult submitArtifacts(Collection<BlackboardArtifact> artifacts, SubmissionProgressListener listener) throws TskCoreException, TskDataException {
        List<Collection<BlackboardArtifact>> batches = getBatches(new ArrayList<>(artifacts), SUBMISSION_BATCH_SIZE);
        int total = artifacts.size();
        int submitted = 0;
        int batchesSucceeded = 0;
        int batchesDone = 0;
        for (Collection<BlackboardArtifact> chunk : batches) {
            Collection<Map<String, Object>> objects = translateObjectToAttributeLists(chunk);
            boolean ok = true;
            if (objects != null && !objects.isEmpty()) {
                ok = gfearBridge.postArtifacts(objects);
            }
            batchesDone++;
            if (ok) {
                batchesSucceeded++;
                submitted += chunk.size();
            }
            if (listener != null) {
                listener.onProgress(batchesDone, batches.size(), submitted, total);
            }
        }
        return new SubmissionResult(total, submitted, batches.size(), batchesSucceeded);
    }

    /**
     * Callback for reporting artefact submission progress.
     */
    public interface SubmissionProgressListener {
        void onProgress(int batchesDone, int batchCount, int artefactsSubmitted, int totalArtefacts);
    }

    /**
     * Summary outcome of an artefact submission.
     */
    public static final class SubmissionResult {
        private final int totalArtefacts;
        private final int artefactsSubmitted;
        private final int batchCount;
        private final int batchesSucceeded;

        public SubmissionResult(int totalArtefacts, int artefactsSubmitted, int batchCount, int batchesSucceeded) {
            this.totalArtefacts = totalArtefacts;
            this.artefactsSubmitted = artefactsSubmitted;
            this.batchCount = batchCount;
            this.batchesSucceeded = batchesSucceeded;
        }

        public int getTotalArtefacts() {
            return totalArtefacts;
        }

        public int getArtefactsSubmitted() {
            return artefactsSubmitted;
        }

        public int getBatchCount() {
            return batchCount;
        }

        public int getBatchesSucceeded() {
            return batchesSucceeded;
        }

        public boolean isCompleteSuccess() {
            return batchesSucceeded == batchCount;
        }
    }

    /**
     * Converts Blackboard artefacts into the self-describing attribute maps that
     * GFEAR scripts consume, augmenting each with host and source-file
     * provenance.
     */
    public Collection<Map<String, Object>> translateObjectToAttributeLists(Collection<BlackboardArtifact> artifacts) throws TskCoreException, TskDataException {
        SleuthkitCase skc = getCaseDatabase();
        Collection<Map<String, Object>> objects = new ArrayList<>();

        for (BlackboardArtifact artifact : artifacts) {
            Type artifactType = artifact.getType();
            Map<String, Object> attributeList = new HashMap<>();
            attributeList.put("Category", artifactType.getDisplayName());
            attributeList.put("TypeName", artifactType.getTypeName());

            if (artifact.getArtifactTypeID() == BlackboardArtifact.Type.TSK_ASSOCIATED_OBJECT.getTypeID()) {
                BlackboardAttribute associatedAttribute = artifact.getAttribute(BlackboardAttribute.Type.TSK_ASSOCIATED_ARTIFACT);
                if (associatedAttribute != null) {
                    BlackboardArtifact associatedObjectArtifact = skc.getArtifactByArtifactId(associatedAttribute.getValueLong());
                    if (associatedObjectArtifact != null) {
                        attributeList.put("AssociatedWith", associatedObjectArtifact.getDisplayName());
                    }
                }
            } else {
                BlackboardArtifactHelpers.getAttributeListFromBlackboardArtifact(artifact, attributeList);
            }

            // Host provenance: the hash of the originating data source. Only disk
            // images expose hashes; other data-source types (logical files,
            // reports, etc.) do not, so guard the type rather than casting blindly.
            // A SHA-1 is also not always recorded for an image --- where it is
            // absent, fall back to the image MD5 so the host still carries a hash
            // identifier.
            //
            // Two representations are emitted. HostSha1 is retained for
            // compatibility with existing GFEAR scripts and carries the MD5 under
            // the SHA-1 key when no SHA-1 exists. HostHash and HostHashType are the
            // generic, self-describing form, added to support further hash
            // algorithms without overloading an algorithm-specific key.
            DataSource dataSource = skc.getDataSource(artifact.getDataSourceObjectID());
            String hostHash = "";
            String hostHashType = "";
            if (dataSource instanceof Image) {
                Image image = (Image) dataSource;
                String sha1 = image.getSha1();
                String md5 = image.getMd5();
                if (sha1 != null && !sha1.trim().isEmpty()) {
                    hostHash = sha1.trim();
                    hostHashType = HASH_TYPE_SHA1;
                } else if (md5 != null && !md5.trim().isEmpty()) {
                    hostHash = md5.trim();
                    hostHashType = HASH_TYPE_MD5;
                }
            }
            attributeList.put("HostSha1", hostHash);
            attributeList.put("HostHash", hostHash);
            attributeList.put("HostHashType", hostHashType);

            AbstractFile sourceObject = skc.getAbstractFileById(artifact.getObjectID());
            if (sourceObject != null) {
                attributeList.put("SourceFile", sourceObject.getParentPath() + sourceObject.getName());
                attributeList.put("SourceFileMd5", sourceObject.getMd5Hash());
                attributeList.put("SourceMime", sourceObject.getMIMEType());
            }

            objects.add(attributeList);
        }

        return objects;
    }

    // ---- Event listeners (retained for future automated ingestion) ----------

    /**
     * Listener for ingest module events. Retained as a hook for future automated
     * ingestion (e.g. forwarding artefacts as individual files are analysed).
     */
    private class IngestModuleEventListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (!isListeningEnabled()) {
                return;
            }
            // Only act on local, in-process ingest; remote updates are deferred.
            if (((AutopsyEvent) event).getSourceType() != AutopsyEvent.SourceType.LOCAL) {
                return;
            }
            switch (IngestManager.IngestModuleEvent.valueOf(event.getPropertyName())) {
                case FILE_DONE:
                    AbstractFile file = (AbstractFile) event.getNewValue();
                    if (file == null || !file.isFile()) {
                        return;
                    }
                    // TODO(future): automated per-file artefact forwarding.
                    break;
                case DATA_ADDED:
                    ModuleDataEvent artifactAddedEvent = (ModuleDataEvent) event.getOldValue();
                    // TODO(future): automated forwarding of newly added artefacts
                    //               (artifactAddedEvent carries the posted artefacts).
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Listener for case events. Handles case-close (closing the top component)
     * and retains hooks for data-source / tag events for future automation.
     */
    private class CaseEventListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            Case.Events eventType = Case.Events.valueOf(event.getPropertyName());
            if (eventType == Case.Events.CURRENT_CASE) {
                if (event.getOldValue() != null) { // case closed
                    if (topComponent != null) {
                        topComponent.closeForCurrentCase();
                    }
                    SwingUtilities.invokeLater(KnowledgeGraphTopComponent::closeTopComponent);
                }
                return;
            }
            switch (eventType) {
                case DATA_SOURCE_ADDED:
                    if (((AutopsyEvent) event).getSourceType() == AutopsyEvent.SourceType.LOCAL && isListeningEnabled()) {
                        Content newDataSource = (Content) event.getNewValue();
                        // TODO(future): trigger automated ingestion when a data source is added.
                    }
                    break;
                case DATA_SOURCE_DELETED:
                    if (((AutopsyEvent) event).getSourceType() == AutopsyEvent.SourceType.LOCAL) {
                        DataSourceDeletedEvent dataSourceDeletedEvent = (DataSourceDeletedEvent) event;
                        long dataSourceObjId = dataSourceDeletedEvent.getDataSourceId();
                        // TODO(future): handle data-source removal.
                    }
                    break;
                case CONTENT_TAG_ADDED:
                    ContentTagAddedEvent tagAddedEvent = (ContentTagAddedEvent) event;
                    // TODO(future): handle content tagging.
                    break;
                case CONTENT_TAG_DELETED:
                    ContentTagDeletedEvent tagDeletedEvent = (ContentTagDeletedEvent) event;
                    // TODO(future): handle content tag removal.
                    break;
                default:
                    logger.log(Level.WARNING, "Received {0} event with no subscription", event.getPropertyName()); //NON-NLS
                    break;
            }
        }
    }

    /**
     * Listener for ingest job events. Retained as a hook for future automated
     * ingestion at the data-source-analysis level.
     */
    private class IngestJobEventListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (!(event instanceof DataSourceAnalysisEvent)) {
                return;
            }
            DataSourceAnalysisEvent dataSourceEvent = (DataSourceAnalysisEvent) event;
            Content dataSource = dataSourceEvent.getDataSource();
            if (dataSource == null) {
                logger.log(Level.WARNING, "Failed to handle {0} event", event.getPropertyName()); //NON-NLS
                return;
            }
            try {
                switch (IngestManager.IngestJobEvent.valueOf(event.getPropertyName())) {
                    case DATA_SOURCE_ANALYSIS_STARTED:
                        handleDataSourceAnalysisStarted(dataSourceEvent);
                        break;
                    case DATA_SOURCE_ANALYSIS_COMPLETED:
                        handleDataSourceAnalysisCompleted(dataSourceEvent);
                        break;
                    default:
                        break;
                }
            } catch (TskCoreException | SQLException ex) {
                logger.log(Level.SEVERE, String.format("Failed to handle %s event for %s", event.getPropertyName(), dataSource.getName()), ex);
            }
        }
    }

    /**
     * Hook for a data-source-analysis-started event (future automated ingestion).
     */
    private void handleDataSourceAnalysisStarted(DataSourceAnalysisEvent event) throws TskCoreException, SQLException {
        // TODO(future): begin automated artefact forwarding for this data source.
    }

    /**
     * Hook for a data-source-analysis-completed event (future automated ingestion).
     */
    private void handleDataSourceAnalysisCompleted(DataSourceAnalysisEvent event) throws TskCoreException, SQLException {
        // TODO(future): finalise automated artefact forwarding for this data source.
    }
}
