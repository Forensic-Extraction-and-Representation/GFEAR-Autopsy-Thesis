/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.fearlanguage.knowledgegraph.gui;

import com.fearlanguage.knowledgegraph.FearEndpoints;
import com.fearlanguage.knowledgegraph.KnowledgeGraphController;
import com.fearlanguage.knowledgegraph.KnowledgeGraphPreferences;
import com.fearlanguage.knowledgegraph.utils;
import com.fearlanguage.knowledgegraph.gui.ArtifactTreeNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.RetainLocation;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.autopsy.coreutils.ThreadConfined;
import org.sleuthkit.datamodel.TskCoreException;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import static org.apache.commons.lang3.ObjectUtils.notEqual;
import org.openide.util.Exceptions;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.BlackboardArtifact.Type;
import org.sleuthkit.datamodel.SleuthkitCase;
import org.sleuthkit.datamodel.TskDataException;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

@TopComponent.Description(
        preferredID = "KnowledgeGraphTopComponent",
        //iconBase = "com/fearlanguage/gfear/images/FEAR-Logo-24.png", /*use this to put icon in window title area*/
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@RetainLocation("KnowledgeGraph")
@TopComponent.Registration(mode = "KnowledgeGraph", openAtStartup = false)
public final class KnowledgeGraphTopComponent extends TopComponent {    
    private volatile KnowledgeGraphController controller;
    private static final long serialVersionUID = 1L;
    private static final String PREFERRED_ID = "KnowledgeGraphTopComponent"; // NON-NLS
    private static final Logger logger = Logger.getLogger(KnowledgeGraphTopComponent.class.getName());

    private static final String VIEW_KNOWLEDGE_GRAPH = resolveViewName();

    private static String resolveViewName() {
        try {
            return NbBundle.getMessage(KnowledgeGraphTopComponent.class, "CTL_OpenAction");
        } catch (RuntimeException ex) {
            com.fearlanguage.knowledgegraph.Diagnostics.reportError(
                    "KnowledgeGraphTopComponent: failed to resolve CTL_OpenAction; using a default name.", ex);
            return "Knowledge Graph";
        }
    }

    private javax.swing.JLabel submissionStatusLabel;
    private javax.swing.JProgressBar submissionProgressBar;
    private javax.swing.JLabel authStatusLabel;
    private javax.swing.JButton loginButton;
    private javax.swing.JCheckBox limitCheck;
    private javax.swing.JSpinner limitSpinner;
    private javax.swing.JButton submitAllButton;
    private javax.swing.JButton exportButton;
    private javax.swing.JTextArea infoTextArea;
    private static KnowledgeGraphTopComponent instance;

    public KnowledgeGraphTopComponent(){
        com.fearlanguage.knowledgegraph.Diagnostics.breadcrumb("KnowledgeGraphTopComponent constructor: start.");
        try {
            setName(NbBundle.getMessage(KnowledgeGraphTopComponent.class, "CTL_KGWindow"));

            initComponents();
            com.fearlanguage.knowledgegraph.Diagnostics.breadcrumb("KnowledgeGraphTopComponent: initComponents done (GraphHomePanel built).");
            initUiComponents();
            buildArtifactDataUi();
            buildInfoUi();
            com.fearlanguage.knowledgegraph.Diagnostics.breadcrumb("KnowledgeGraphTopComponent constructor: completed.");
        } catch (Throwable ex) {
            com.fearlanguage.knowledgegraph.Diagnostics.reportError(
                    "Failed to construct the Knowledge Graph window.", ex);
        }
    }
    
    private void initUiComponents(){
        jTextArea1.getActionMap().put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = jTextArea1.getText();
                StringSelection selection = new StringSelection(text);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);
            }
        });

        jTextArea1.getActionMap().put("paste", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = jTextArea1.getText();
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable clipboardContent = clipboard.getContents(null);
                try{
                String content = (String)clipboardContent.getTransferData(DataFlavor.stringFlavor);
                jTextArea1.setText(content);
                }
                catch(Exception ex){
                    Exceptions.printStackTrace(ex);
                }
            }
        });
    }
    
    /**
     * Rebuilds the "Artifact Data" tab with a split pane (artefact-type tree and
     * JSON preview) above a status bar (selection / submission status, a progress
     * bar, and the Submit button). Built in code so the NetBeans form is left
     * intact; reuses the form's tree, preview, and button components.
     */
    private void buildArtifactDataUi() {
        submissionStatusLabel = new javax.swing.JLabel(" ");
        submissionProgressBar = new javax.swing.JProgressBar(0, 100);
        submissionProgressBar.setStringPainted(true);
        submissionProgressBar.setVisible(false);
        submissionProgressBar.setPreferredSize(new java.awt.Dimension(
                200, submissionProgressBar.getPreferredSize().height));

        jTextArea1.setEditable(false);
        jTextArea1.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));

        javax.swing.JSplitPane split = new javax.swing.JSplitPane(
                javax.swing.JSplitPane.HORIZONTAL_SPLIT, jScrollPane1, jScrollPane2);
        split.setResizeWeight(0.28);
        split.setContinuousLayout(true);
        split.setBorder(null);

        authStatusLabel = new javax.swing.JLabel();
        loginButton = new javax.swing.JButton("Login");
        loginButton.addActionListener(e -> triggerLogin());

        javax.swing.JPanel westControls = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        westControls.add(authStatusLabel);
        westControls.add(loginButton);

        limitCheck = new javax.swing.JCheckBox("Limit to first");
        limitCheck.setToolTipText("Submit only the first N artefacts of the selected type.");
        limitSpinner = new javax.swing.JSpinner(
                new javax.swing.SpinnerNumberModel(5, 1, Integer.MAX_VALUE, 1));
        limitSpinner.setPreferredSize(new java.awt.Dimension(
                72, limitSpinner.getPreferredSize().height));
        limitSpinner.setEnabled(false);
        limitCheck.addActionListener(e -> limitSpinner.setEnabled(limitCheck.isSelected()));

        jButton1.setText("Submit Selected");
        jButton1.setToolTipText("Submit the selected artefact type (honouring the limit, if set).");
        submitAllButton = new javax.swing.JButton("Submit All");
        submitAllButton.setToolTipText("Submit every artefact across all categories.");
        submitAllButton.addActionListener(e -> submitAllArtifacts());

        exportButton = new javax.swing.JButton("Export…");
        exportButton.setToolTipText("Export artefacts to a JSON file (choose a category and how many).");
        exportButton.addActionListener(e -> exportArtifacts());

        javax.swing.JPanel eastControls = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        eastControls.add(exportButton);
        eastControls.add(limitCheck);
        eastControls.add(limitSpinner);
        eastControls.add(submissionProgressBar);
        eastControls.add(jButton1);
        eastControls.add(submitAllButton);

        javax.swing.JPanel statusBar = new javax.swing.JPanel(new java.awt.BorderLayout(8, 0));
        statusBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));
        statusBar.add(westControls, java.awt.BorderLayout.WEST);
        statusBar.add(submissionStatusLabel, java.awt.BorderLayout.CENTER);
        statusBar.add(eastControls, java.awt.BorderLayout.EAST);

        jPanel1.removeAll();
        jPanel1.setLayout(new java.awt.BorderLayout());
        jPanel1.add(split, java.awt.BorderLayout.CENTER);
        jPanel1.add(statusBar, java.awt.BorderLayout.SOUTH);
        jPanel1.revalidate();
        jPanel1.repaint();

        refreshAuthStatus();
    }

    /**
     * Updates the authentication status indicator and the login button label to
     * reflect whether the module is currently authenticated against the FEAR
     * Framework. Safe to call before a case is bound (shows "Not authenticated").
     */
    private void refreshAuthStatus() {
        boolean authenticated = controller != null && controller.isAuthenticated();
        if (authenticated) {
            authStatusLabel.setText("Authenticated");
            authStatusLabel.setForeground(new java.awt.Color(0, 128, 0));
            loginButton.setText("Re-authenticate");
        } else {
            authStatusLabel.setText("Not authenticated");
            authStatusLabel.setForeground(java.awt.Color.RED);
            loginButton.setText("Login");
        }
    }

    /**
     * Builds the "Info" tab: a read-only, monospaced view of the effective
     * connection configuration --- the case name in use and the full URLs the
     * module composes for the API, artefact submission, the query Web UI, and
     * OAuth. Values are read from the same {@link FearEndpoints} helper the live
     * requests use, so the tab reflects exactly what the module sends. A Refresh
     * button re-reads the current case/options; Copy places the report on the
     * clipboard for pasting into bug reports.
     */
    private void buildInfoUi() {
        infoTextArea = new javax.swing.JTextArea();
        infoTextArea.setEditable(false);
        infoTextArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        infoTextArea.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));

        javax.swing.JButton refreshButton = new javax.swing.JButton("Refresh");
        refreshButton.setToolTipText("Re-read the current case name, configuration, and composed URLs.");
        refreshButton.addActionListener(e -> refreshInfo());

        javax.swing.JButton copyButton = new javax.swing.JButton("Copy");
        copyButton.setToolTipText("Copy this report to the clipboard.");
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(infoTextArea.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        });

        javax.swing.JPanel toolbar = new javax.swing.JPanel(
                new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 4));
        toolbar.add(refreshButton);
        toolbar.add(copyButton);

        javax.swing.JPanel infoPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
        infoPanel.add(toolbar, java.awt.BorderLayout.NORTH);
        infoPanel.add(new javax.swing.JScrollPane(infoTextArea), java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab("Info", infoPanel);
        // Re-read configuration whenever the user switches to the Info tab, so a
        // change made in Tools -> Options is reflected without reopening the window.
        jTabbedPane1.addChangeListener(e -> {
            if (jTabbedPane1.getSelectedComponent() == infoPanel) {
                refreshInfo();
            }
        });
        refreshInfo();
    }

    /** Re-renders the Info tab from the current case, configuration, and options. */
    private void refreshInfo() {
        if (infoTextArea == null) {
            return;
        }
        infoTextArea.setText(buildInfoText());
        infoTextArea.setCaretPosition(0);
    }

    /**
     * Composes the human-readable connection report shown in the Info tab.
     */
    private String buildInfoText() {
        String configuredCase = KnowledgeGraphPreferences.getFearCaseName();
        String effectiveCase = KnowledgeGraphPreferences.getEffectiveFearCaseName();
        String caseSource;
        if (!utils.isBlank(configuredCase)) {
            caseSource = "configured FEAR case name";
        } else if (Case.isCaseOpen()) {
            caseSource = "Autopsy case display name (no FEAR case name configured)";
        } else {
            caseSource = "none (no case open and no FEAR case name configured)";
        }

        boolean authenticated = controller != null && controller.isAuthenticated();

        StringBuilder sb = new StringBuilder();
        sb.append("FEAR Knowledge Graph — connection info\n");
        sb.append("======================================\n\n");
        sb.append(row("Authentication:", authenticated ? "Authenticated" : "Not authenticated"));
        sb.append(row("Effective case name:", orNotSet(effectiveCase)));
        sb.append(row("  case name source:", caseSource));
        sb.append("\n");
        sb.append(row("API base path:", orNotSet(FearEndpoints.apiBase())));
        sb.append(row("UI base path:", orNotSet(FearEndpoints.uiBase())));
        sb.append("\n");
        sb.append("Composed URLs\n");
        sb.append(row("  Open case:", FearEndpoints.openCaseUrl(effectiveCase)));
        sb.append(row("  Submit artefacts:", FearEndpoints.postArtifactsUrl(effectiveCase)));
        sb.append(row("  Web UI (query):", FearEndpoints.webUiUrl(effectiveCase)));
        sb.append("\n");
        sb.append("OAuth\n");
        sb.append(row("  Authorize endpoint:", FearEndpoints.authorizeEndpointUrl()));
        sb.append(row("  Token endpoint:", FearEndpoints.tokenEndpointUrl()));
        sb.append(row("  Client ID:", orNotSet(KnowledgeGraphPreferences.getOAuthClientId())));
        sb.append(row("  Scope:", orNotSet(KnowledgeGraphPreferences.getOAuthScope())));
        sb.append(row("  Redirect URI:", FearEndpoints.REDIRECT_URI));
        sb.append("\n");
        sb.append("Options\n");
        sb.append(row("  Allow insecure SSL:", String.valueOf(KnowledgeGraphPreferences.getAllowInsecureSsl())));
        sb.append("\n");
        sb.append("Note: URLs are composed from the effective case name and current options.\n");
        sb.append("      The submit URL used at runtime carries the case name returned by Open case.\n");
        return sb.toString();
    }

    /** Formats a left-padded label followed by its value, as one report line. */
    private static String row(String label, String value) {
        int width = 24;
        StringBuilder b = new StringBuilder(label);
        while (b.length() < width) {
            b.append(' ');
        }
        return b.append(value).append('\n').toString();
    }

    /** @return {@code value}, or a "(not set)" placeholder when blank. */
    private static String orNotSet(String value) {
        return utils.isBlank(value) ? "(not set)" : value;
    }

    /**
     * Opens the OAuth login window. On completion (success or failure) the
     * authentication status indicator is refreshed.
     */
    private void triggerLogin() {
        try {
            if (Case.isCaseOpen() == false) {
                submissionStatusLabel.setText("Open a case before authenticating.");
                return;
            }
            LoginPanel loginPanel = new LoginPanel();
            loginPanel.setCloseAction(() -> SwingUtilities.invokeLater(this::refreshAuthStatus));
            loginPanel.loadLoginPage();
            loginPanel.setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
            loginPanel.pack();
            loginPanel.setLocationRelativeTo(this);
            loginPanel.setVisible(true);
        } catch (Throwable ex) {
            com.fearlanguage.knowledgegraph.Diagnostics.reportError(
                    "Failed to open the login window.", ex);
        }
    }

    @Override
    public String getName() {
        return VIEW_KNOWLEDGE_GRAPH;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected void componentShowing() {
        super.componentShowing();
        // Refresh the authentication indicator whenever the component is launched
        // or re-shown (the other trigger is the Login button itself).
        refreshAuthStatus();
        // The Info tab reflects live configuration/case state, so refresh it too.
        refreshInfo();
    }

    public static boolean isKnowledgeGraphOpen(){
        return getTopComponent().isOpened();
    }
    
    /**
     * Returns the single Knowledge Graph top component instance.
     *
     * <p>The component is registered with {@code PERSISTENCE_NEVER}, so
     * {@link WindowManager#findTopComponent(String)} returns {@code null} until an
     * instance has been created — dereferencing that result caused a
     * {@link NullPointerException} when opening from the toolbar. This method
     * caches a singleton and creates the instance on first use when the window
     * system has none, which also prevents duplicate construction of the embedded
     * {@code GraphHomePanel}/JCEF runtime.</p>
     */
    public static synchronized KnowledgeGraphTopComponent getTopComponent() {
        if (instance == null) {
            TopComponent found = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
            if (found instanceof KnowledgeGraphTopComponent) {
                instance = (KnowledgeGraphTopComponent) found;
            } else {
                instance = new KnowledgeGraphTopComponent();
            }
        }
        return instance;
    }
    
    @ThreadConfined(type = ThreadConfined.ThreadType.AWT)
    public static void openTopComponent() throws TskCoreException {
        final KnowledgeGraphTopComponent topComponent = getTopComponent();
        if (topComponent.isOpened()) {
            showTopComponent();
        } else {
            topComponent.openForCurrentCase();
        }
    }
    
    @ThreadConfined(type = ThreadConfined.ThreadType.AWT)
    private static void showTopComponent() {
        final KnowledgeGraphTopComponent topComponent = getTopComponent();
        if (topComponent.isOpened() == false) {
            topComponent.open();
        }
        topComponent.toFront();
        topComponent.requestActive();
    }
    
    public static void closeTopComponent() {
        KnowledgeGraphTopComponent topComponent = getTopComponent();
        topComponent.closeForCurrentCase();
        topComponent.close();
    }
        
    public void openForCurrentCase() throws TskCoreException {
        // Run on the Swing EDT (not JavaFX's Platform.runLater, which does nothing
        // when the JavaFX toolkit is not started --- as in a packaged Autopsy).
        SwingUtilities.invokeLater(() -> {
            try {
                resetComponents();
                openForDataSelectedSources();
            } catch (Throwable ex) {
                com.fearlanguage.knowledgegraph.Diagnostics.reportError(
                        "Failed to prepare the Knowledge Graph window for the current case.", ex);
            }
        });
    }
        
    private void resetComponents() {
        Case currentCase = Case.getCurrentCase();
        KnowledgeGraphController controllerForCase = KnowledgeGraphController.getController(currentCase);
        if (notEqual(controller, controllerForCase)) {
            if(controller != null){
                controller.getCase().getSleuthkitCase().unregisterForEvents(controller);
            }

            controller = controllerForCase;
            SleuthkitCase skc = controller.getCase().getSleuthkitCase();
            skc.registerForEvents(controller);
            //initComponents();
        }
        SwingUtilities.invokeLater(this::refreshAuthStatus);
    }
     
    private void openForDataSelectedSources() {

        new Thread(() -> {
            final Iterable<Type> typesInUse;
            try {
                typesInUse = controller.getCase().getSleuthkitCase().getArtifactTypesInUse();
            } catch (Throwable ex) {
                com.fearlanguage.knowledgegraph.Diagnostics.reportError(
                        "Unable to read the artefact types in use for the current case.", ex);
                return;
            }
            SwingUtilities.invokeLater(() -> {
                try {
                    DefaultTreeModel model = (DefaultTreeModel) jTree1.getModel();
                    DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
                    root.removeAllChildren();
                    for (Type t : typesInUse) {
                        root.add(new ArtifactTreeNode(t));
                    }
                    model.reload();
                    showTopComponent();
                } catch (Throwable ex) {
                    com.fearlanguage.knowledgegraph.Diagnostics.reportError(
                            "Failed to populate the artefact-type tree and show the window.", ex);
                }
            });
        }, "KG-load-artifact-types").start();
    }
     
    public void closeForCurrentCase() {
        // No per-case UI state to release at present.
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jTabbedPane1 = new javax.swing.JTabbedPane();
        graphHomePanel1 = new com.fearlanguage.knowledgegraph.gui.GraphHomePanel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButton1 = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(KnowledgeGraphTopComponent.class, "KnowledgeGraphTopComponent.graphHomePanel1.TabConstraints.tabTitle"), graphHomePanel1); // NOI18N

        jPanel1.setAlignmentX(1.0F);
        jPanel1.setAlignmentY(1.0F);
        jPanel1.setAutoscrolls(true);
        java.awt.GridBagLayout jPanel1Layout = new java.awt.GridBagLayout();
        jPanel1Layout.columnWidths = new int[] {0, 5, 0};
        jPanel1Layout.rowHeights = new int[] {0, 5, 0, 5, 0};
        jPanel1.setLayout(jPanel1Layout);

        jTree1.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jTree1ValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jTree1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 15.0;
        gridBagConstraints.weighty = 99.0;
        jPanel1.add(jScrollPane1, gridBagConstraints);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane2.setViewportView(jTextArea1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 85.0;
        gridBagConstraints.weighty = 99.0;
        jPanel1.add(jScrollPane2, gridBagConstraints);

        jButton1.setLabel(org.openide.util.NbBundle.getMessage(KnowledgeGraphTopComponent.class, "KnowledgeGraphTopComponent.jButton1.label")); // NOI18N
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButton1MouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.PAGE_END;
        jPanel1.add(jButton1, gridBagConstraints);

        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(KnowledgeGraphTopComponent.class, "KnowledgeGraphTopComponent.jPanel1.TabConstraints.tabTitle"), jPanel1); // NOI18N

        add(jTabbedPane1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void jTree1ValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jTree1ValueChanged
        // TODO add your handling code here:
        this.treeSelectionEvent(evt);
    }//GEN-LAST:event_jTree1ValueChanged

    private void jButton1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButton1MouseClicked
        // TODO add your handling code here:
        this.postArtifactsBtn(evt);
    }//GEN-LAST:event_jButton1MouseClicked
    
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private void treeSelectionEvent(javax.swing.event.TreeSelectionEvent event){
        Object selected = jTree1.getLastSelectedPathComponent();
        if (!(selected instanceof ArtifactTreeNode) || controller == null) {
            return;
        }
        Type artifactType = ((ArtifactTreeNode) selected).getArtifactType();
        String json = "";
        try {
            SleuthkitCase skc = controller.getCase().getSleuthkitCase();
            ArrayList<BlackboardArtifact> artifacts = skc.getBlackboardArtifacts(artifactType.getTypeName());
            int total = artifacts.size();
            int previewCount = Math.min(50, total);
            json = getJsonForArtifacts(artifacts.subList(0, previewCount), skc, artifactType);
            submissionStatusLabel.setText(String.format("%s: %,d artefact(s). Previewing first %d.",
                    artifactType.getDisplayName(), total, previewCount));
        } catch (TskCoreException | TskDataException ex) {
            Exceptions.printStackTrace(ex);
            submissionStatusLabel.setText("Unable to read artefacts for the selected type — see the log.");
        }
        jTextArea1.setText(json);
        jTextArea1.setCaretPosition(0);
    }

    /**
     * Handles the "Submit Selected" button: submits the artefacts of the selected
     * type, capped to the first N if the limit option is enabled.
     */
    private void postArtifactsBtn(java.awt.event.MouseEvent evt){
        Object selected = jTree1.getLastSelectedPathComponent();
        if (!(selected instanceof ArtifactTreeNode)) {
            submissionStatusLabel.setText("Select an artefact type to submit.");
            return;
        }
        if (controller == null) {
            submissionStatusLabel.setText("No case is open.");
            return;
        }
        if (!ensureConfiguredForSubmission()) {
            return;
        }
        final Type artifactType = ((ArtifactTreeNode) selected).getArtifactType();
        final int limit = getSubmissionLimit();
        final String label = (limit > 0)
                ? String.format("%s (first %,d)", artifactType.getDisplayName(), limit)
                : artifactType.getDisplayName();

        runSubmission(label, () -> {
            SleuthkitCase skc = controller.getCase().getSleuthkitCase();
            ArrayList<BlackboardArtifact> artifacts = skc.getBlackboardArtifacts(artifactType.getTypeName());
            if (limit > 0 && artifacts.size() > limit) {
                return new ArrayList<>(artifacts.subList(0, limit));
            }
            return artifacts;
        });
    }

    /**
     * Handles the "Submit All" button: submits every artefact across all
     * categories. The first-N limit does not apply to this action.
     */
    private void submitAllArtifacts() {
        if (controller == null) {
            submissionStatusLabel.setText("No case is open.");
            return;
        }
        if (!ensureConfiguredForSubmission()) {
            return;
        }
        runSubmission("all categories", () -> controller.getAllArtifacts());
    }

    /** How many artefacts an export should include. */
    private enum ExportCount { ALL, FIRST_50, FIRST_20_EACH }

    /**
     * Handles the "Export…" button: prompts for a category (or all categories)
     * and how many artefacts to include, then writes the translated artefacts to
     * a user-chosen JSON file. The "First 20 of each category" option is only
     * offered when exporting all categories.
     */
    private void exportArtifacts() {
        if (controller == null) {
            submissionStatusLabel.setText("No case is open.");
            return;
        }

        // Build the category list from the artefact-type tree, preserving order.
        final LinkedHashMap<String, Type> categories = new LinkedHashMap<>();
        DefaultTreeModel model = (DefaultTreeModel) jTree1.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            javax.swing.tree.TreeNode child = root.getChildAt(i);
            if (child instanceof ArtifactTreeNode) {
                Type t = ((ArtifactTreeNode) child).getArtifactType();
                categories.put(t.getDisplayName(), t);
            }
        }
        if (categories.isEmpty()) {
            submissionStatusLabel.setText("No artefacts available to export.");
            return;
        }

        final String allCategories = "All categories";
        final String countAll = "All items";
        final String countFirst50 = "First 50";
        final String countFirst20Each = "First 20 of each category";

        final javax.swing.JComboBox<String> categoryCombo = new javax.swing.JComboBox<>();
        categoryCombo.addItem(allCategories);
        for (String name : categories.keySet()) {
            categoryCombo.addItem(name);
        }

        final javax.swing.JComboBox<String> countCombo = new javax.swing.JComboBox<>();
        // Rebuild the count options whenever the category changes so that
        // "First 20 of each category" is only present for an all-categories export.
        Runnable rebuildCountItems = () -> {
            Object current = countCombo.getSelectedItem();
            countCombo.removeAllItems();
            countCombo.addItem(countAll);
            countCombo.addItem(countFirst50);
            if (allCategories.equals(categoryCombo.getSelectedItem())) {
                countCombo.addItem(countFirst20Each);
            }
            if (current != null) {
                countCombo.setSelectedItem(current); // ignored if no longer present
            }
        };
        rebuildCountItems.run();
        categoryCombo.addActionListener(e -> rebuildCountItems.run());

        javax.swing.JPanel form = new javax.swing.JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.insets = new java.awt.Insets(4, 4, 4, 4);
        c.anchor = java.awt.GridBagConstraints.LINE_START;
        c.gridx = 0; c.gridy = 0;
        form.add(new javax.swing.JLabel("Category:"), c);
        c.gridx = 1; c.fill = java.awt.GridBagConstraints.HORIZONTAL; c.weightx = 1.0;
        form.add(categoryCombo, c);
        c.gridx = 0; c.gridy = 1; c.fill = java.awt.GridBagConstraints.NONE; c.weightx = 0;
        form.add(new javax.swing.JLabel("Number of items:"), c);
        c.gridx = 1; c.fill = java.awt.GridBagConstraints.HORIZONTAL; c.weightx = 1.0;
        form.add(countCombo, c);

        int choice = javax.swing.JOptionPane.showConfirmDialog(this, form,
                "Export Artefacts to JSON", javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (choice != javax.swing.JOptionPane.OK_OPTION) {
            return;
        }

        final Object selectedCategoryName = categoryCombo.getSelectedItem();
        final Type selectedCategory = allCategories.equals(selectedCategoryName)
                ? null : categories.get(selectedCategoryName);
        final Object selectedCountName = countCombo.getSelectedItem();
        final ExportCount count;
        if (countFirst50.equals(selectedCountName)) {
            count = ExportCount.FIRST_50;
        } else if (countFirst20Each.equals(selectedCountName)) {
            count = ExportCount.FIRST_20_EACH;
        } else {
            count = ExportCount.ALL;
        }

        // Let the user choose where to save.
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Artefact Export");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
        chooser.setSelectedFile(new File(suggestExportFileName(String.valueOf(selectedCategoryName), count)));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith(".json")) {
            target = new File(target.getParentFile(), target.getName() + ".json");
        }
        if (target.exists()) {
            int overwrite = javax.swing.JOptionPane.showConfirmDialog(this,
                    "\"" + target.getName() + "\" already exists. Overwrite?",
                    "Confirm Overwrite", javax.swing.JOptionPane.YES_NO_OPTION,
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            if (overwrite != javax.swing.JOptionPane.YES_OPTION) {
                return;
            }
        }

        runExport(selectedCategory, count, target);
    }

    /** Derives a sensible default file name for an export from the chosen options. */
    private static String suggestExportFileName(String categoryName, ExportCount count) {
        String base = categoryName == null ? "artefacts" : categoryName;
        base = base.replaceAll("[^a-zA-Z0-9._-]+", "_").replaceAll("^_+|_+$", "");
        if (base.isEmpty()) {
            base = "artefacts";
        }
        String suffix;
        switch (count) {
            case FIRST_50: suffix = "_first50"; break;
            case FIRST_20_EACH: suffix = "_first20each"; break;
            default: suffix = ""; break;
        }
        return base + suffix + ".json";
    }

    /**
     * Collects the artefacts to export according to the chosen category and count.
     *
     * @param category the artefact type to export, or {@code null} for all types.
     * @param count    how many artefacts to include.
     *
     * @return the artefacts to export (never {@code null}).
     */
    private List<BlackboardArtifact> gatherArtifactsForExport(Type category, ExportCount count)
            throws TskCoreException {
        SleuthkitCase skc = controller.getCase().getSleuthkitCase();
        List<BlackboardArtifact> result = new ArrayList<>();
        if (category == null) {
            if (count == ExportCount.FIRST_20_EACH) {
                for (Type t : skc.getArtifactTypesInUse()) {
                    List<BlackboardArtifact> a = skc.getBlackboardArtifacts(t.getTypeName());
                    result.addAll(a.subList(0, Math.min(20, a.size())));
                }
            } else {
                List<BlackboardArtifact> all = controller.getAllArtifacts();
                result.addAll(count == ExportCount.FIRST_50
                        ? all.subList(0, Math.min(50, all.size()))
                        : all);
            }
        } else {
            List<BlackboardArtifact> a = skc.getBlackboardArtifacts(category.getTypeName());
            // FIRST_20_EACH is not offered for a single category; treat it as ALL.
            result.addAll(count == ExportCount.FIRST_50
                    ? a.subList(0, Math.min(50, a.size()))
                    : a);
        }
        return result;
    }

    /**
     * Gathers, translates, and writes the selected artefacts to {@code target} as
     * pretty-printed JSON, off the EDT, reporting the outcome on the status label.
     */
    private void runExport(final Type category, final ExportCount count, final File target) {
        exportButton.setEnabled(false);
        submissionStatusLabel.setText("Exporting…");

        javax.swing.SwingWorker<Integer, Void> worker = new javax.swing.SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                List<BlackboardArtifact> artifacts = gatherArtifactsForExport(category, count);
                Collection<Map<String, Object>> objects =
                        controller.translateObjectToAttributeLists(artifacts);
                String json = gson.toJson(objects);
                Files.write(target.toPath(), json.getBytes(StandardCharsets.UTF_8));
                return artifacts.size();
            }

            @Override
            protected void done() {
                exportButton.setEnabled(true);
                try {
                    int exported = get();
                    submissionStatusLabel.setText(String.format(
                            "Exported %,d artefact(s) to %s", exported, target.getName()));
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                    submissionStatusLabel.setText("Export failed — see the log.");
                }
            }
        };
        worker.execute();
    }

    /**
     * Verifies the module is configured for the current case before submitting,
     * showing a guidance message (and updating the status label) if not.
     *
     * @return true if configured and submission may proceed.
     */
    private boolean ensureConfiguredForSubmission() {
        if (com.fearlanguage.knowledgegraph.KnowledgeGraphPreferences.isConfigured()) {
            return true;
        }
        submissionStatusLabel.setText("Not configured — set the FEAR API base path in Tools → Options.");
        com.fearlanguage.knowledgegraph.Diagnostics.userMessage(
                "FEAR Knowledge Graph — Not Configured",
                "The FEAR API base path is not set for this case.\n"
                + "Open Tools → Options → Knowledge Graph (with the case open) and configure it "
                + "before submitting artefacts.");
        return false;
    }

    /**
     * @return the configured submission cap (first N), or -1 when no limit is set.
     */
    private int getSubmissionLimit() {
        if (limitCheck != null && limitCheck.isSelected()) {
            Object value = limitSpinner.getValue();
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        return -1;
    }

    /**
     * Runs an artefact submission on a background worker, reporting per-batch
     * progress and a final outcome. The artefacts to submit are produced by
     * {@code artifactSupplier} (called off the EDT).
     *
     * @param label            Human-readable description of what is being
     *                         submitted (used in the status messages).
     * @param artifactSupplier Supplies the artefacts to submit; invoked on the
     *                         worker thread.
     */
    private void runSubmission(final String label,
            final java.util.concurrent.Callable<List<BlackboardArtifact>> artifactSupplier) {
        jButton1.setEnabled(false);
        submitAllButton.setEnabled(false);
        submissionProgressBar.setValue(0);
        submissionProgressBar.setVisible(true);
        submissionStatusLabel.setText("Preparing submission…");

        javax.swing.SwingWorker<KnowledgeGraphController.SubmissionResult, String> worker
                = new javax.swing.SwingWorker<KnowledgeGraphController.SubmissionResult, String>() {
            @Override
            protected KnowledgeGraphController.SubmissionResult doInBackground() throws Exception {
                List<BlackboardArtifact> artifacts = artifactSupplier.call();
                return controller.submitArtifacts(artifacts, (batchesDone, batchCount, submitted, total) -> {
                    publish(String.format("Submitting %s: %,d / %,d artefact(s) (batch %d/%d)…",
                            label, submitted, total, batchesDone, batchCount));
                    setProgress(batchCount == 0 ? 100 : (int) ((batchesDone * 100L) / batchCount));
                });
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    submissionStatusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                jButton1.setEnabled(true);
                submitAllButton.setEnabled(true);
                submissionProgressBar.setVisible(false);
                try {
                    KnowledgeGraphController.SubmissionResult result = get();
                    if (result.isCompleteSuccess()) {
                        submissionStatusLabel.setText(String.format(
                                "Submitted %,d artefact(s) (%s) in %d batch(es).",
                                result.getArtefactsSubmitted(), label, result.getBatchCount()));
                    } else {
                        int failed = result.getBatchCount() - result.getBatchesSucceeded();
                        submissionStatusLabel.setText(String.format(
                                "Submitted %,d of %,d artefact(s) (%s); %d of %d batch(es) failed — see the log.",
                                result.getArtefactsSubmitted(), result.getTotalArtefacts(), label, failed, result.getBatchCount()));
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                    submissionStatusLabel.setText("Submission failed — see the log.");
                }
            }
        };
        worker.addPropertyChangeListener(e -> {
            if ("progress".equals(e.getPropertyName())) {
                submissionProgressBar.setValue((Integer) e.getNewValue());
            }
        });
        worker.execute();
    }
    
    private String getJsonForArtifacts(List<BlackboardArtifact> artifacts, SleuthkitCase skc, Type artifactType) throws TskCoreException, TskDataException {
        String json = gson.toJson(controller.translateObjectToAttributeLists(artifacts));
        return json;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.fearlanguage.knowledgegraph.gui.GraphHomePanel graphHomePanel1;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTree jTree1;
    // End of variables declaration//GEN-END:variables
}
