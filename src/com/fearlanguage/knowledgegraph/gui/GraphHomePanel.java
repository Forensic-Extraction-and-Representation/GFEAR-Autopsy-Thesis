/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/AWTForms/Panel.java to edit this template
 */
package com.fearlanguage.knowledgegraph.gui;

import com.fearlanguage.knowledgegraph.CertificateProvider.InsecureProvider;
import com.fearlanguage.knowledgegraph.Diagnostics;
import com.fearlanguage.knowledgegraph.FearEndpoints;
import com.fearlanguage.knowledgegraph.KnowledgeGraphController;
import com.fearlanguage.knowledgegraph.KnowledgeGraphPreferences;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.sleuthkit.autopsy.casemodule.Case;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.handler.CefContextMenuHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;

/**
 *
 * @author Allan
 */
public class GraphHomePanel extends java.awt.Panel {
     
    /**
     * Creates new form GraphHomePanel
     */
    public GraphHomePanel() {
        Diagnostics.breadcrumb("GraphHomePanel constructor: start.");
        try {
            ensureCefInitialised();
            Diagnostics.breadcrumb("GraphHomePanel: CEF initialised.");
        } catch (Throwable ex) {
            Diagnostics.reportError("Failed to initialise the embedded browser (JCEF). "
                    + "The query/login views will not be available.", ex);
        }
        try {
            loginPrompt();
        } catch (Throwable ex) {
            Diagnostics.reportError("Failed to open the FEAR login window.", ex);
        }
        initComponents();
    }

    /** Guards {@link #ensureCefInitialised()} so JCEF is initialised only once. */
    private static boolean cefInitialised = false;

    /**
     * Initialises the embedded Chromium (JCEF) runtime exactly once for the
     * lifetime of the module, and returns the shared {@link CefApp}.
     *
     * <p>JCEF requires that any application handler is registered <em>before</em>
     * {@code CefApp} is first initialised (i.e. before the first
     * {@code getInstance(...)} call), and that initialisation occurs only once.
     * Constructing more than one {@code GraphHomePanel} or login window
     * previously re-ran this sequence and threw
     * {@code IllegalStateException: Must be called before CefApp is initialized}.
     * Centralising it here behind a one-shot guard makes repeated construction
     * safe and guarantees the insecure-SSL command-line switch (when opted in) is
     * registered before initialisation.</p>
     */
    static synchronized CefApp ensureCefInitialised() throws Exception {
        Diagnostics.breadcrumb("ensureCefInitialised: entered (cefInitialised=" + cefInitialised + ").");
        boolean allowInsecure = KnowledgeGraphPreferences.getAllowInsecureSsl();
        if (allowInsecure) {
            // Required for the FEAR Framework's self-signed certificate. Applied to
            // the JSON/HTTP transports here, and to JCEF via the switch below.
            InsecureProvider.install();
        }
        if (!cefInitialised) {
            CefApp.startup(new String[] {});
            if (allowInsecure) {
                // Tell the embedded Chromium to ignore certificate errors. Must be
                // registered before the first CefApp.getInstance(...) call below.
                CefApp.addAppHandler(new org.cef.handler.CefAppHandlerAdapter(new String[] {}) {
                    @Override
                    public void onBeforeCommandLineProcessing(String processType, org.cef.callback.CefCommandLine commandLine) {
                        commandLine.appendSwitch("ignore-certificate-errors");
                    }
                });
            }
            CefSettings settings = new CefSettings();
            settings.windowless_rendering_enabled = false;
            CefApp.getInstance(settings);
            cefInitialised = true;
        }
        return CefApp.getInstance();
    }
    
    private Boolean loginPrompt(){

        SwingUtilities.invokeLater(()-> {
            try {
                Case current = Case.getCurrentCase();
                KnowledgeGraphController kgc = KnowledgeGraphController.getController(current);

                LoginPanel newFrame = new LoginPanel();
                newFrame.setCloseAction(() -> {
                    newFrame.setVisible(false);
                });

                newFrame.loadLoginPage();
                newFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Close only this window
                newFrame.setSize(300,400);

                // Set the size and make the frame visible
                newFrame.pack(); // Sizes the frame to its components' preferred sizes
                newFrame.setLocationRelativeTo(null); // Center the window on the screen
                newFrame.setVisible(true);
            } catch (Throwable ex) {
                Diagnostics.reportError("Failed to display the FEAR login window.", ex);
            }
        });

        return true;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        homeLinkPanel = new javax.swing.JPanel();
        leadingLogo = new javax.swing.JLabel();
        leadingTitle = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();

        setLayout(new java.awt.BorderLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 15, 1));
        jPanel1.setMaximumSize(new java.awt.Dimension(48, 48));
        jPanel1.setMinimumSize(new java.awt.Dimension(48, 48));
        jPanel1.setPreferredSize(new java.awt.Dimension(48, 48));
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fearlanguage/knowledgegraph/images/info-icon-16.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(GraphHomePanel.class, "GraphHomePanel.jButton1.text")); // NOI18N
        jButton1.setToolTipText(org.openide.util.NbBundle.getMessage(GraphHomePanel.class, "GraphHomePanel.jButton1.toolTipText")); // NOI18N
        jButton1.setHideActionText(true);
        jButton1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton1.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton1);

        add(jPanel1, java.awt.BorderLayout.SOUTH);

        homeLinkPanel.setLayout(new java.awt.GridBagLayout());

        leadingLogo.setFont(new java.awt.Font("Calibri", 1, 18)); // NOI18N
        leadingLogo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        leadingLogo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/fearlanguage/knowledgegraph/images/FEAR-Logo-32.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(leadingLogo, org.openide.util.NbBundle.getMessage(GraphHomePanel.class, "GraphHomePanel.leadingLogo.text")); // NOI18N
        leadingLogo.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        leadingLogo.setBorder(javax.swing.BorderFactory.createEmptyBorder(25, 1, 1, 1));
        leadingLogo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        leadingLogo.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(7, 0, 0, 0);
        homeLinkPanel.add(leadingLogo, gridBagConstraints);

        leadingTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(leadingTitle, org.openide.util.NbBundle.getMessage(GraphHomePanel.class, "GraphHomePanel.leadingTitle.text")); // NOI18N
        leadingTitle.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(7, 0, 8, 0);
        homeLinkPanel.add(leadingTitle, gridBagConstraints);

        add(homeLinkPanel, java.awt.BorderLayout.NORTH);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.Y_AXIS));

        jLabel1.setForeground(new java.awt.Color(102, 153, 255));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(GraphHomePanel.class, "GraphHomePanel.jLabel1.text")); // NOI18N
        jLabel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 1, 4, 5));
        jLabel1.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jLabel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel1MouseClicked(evt);
            }
        });
        jPanel4.add(jLabel1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        jPanel3.add(jPanel4, gridBagConstraints);

        jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5, javax.swing.BoxLayout.Y_AXIS));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        jPanel3.add(jPanel5, gridBagConstraints);

        add(jPanel3, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    
    private void jLabel1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel1MouseClicked
        javax.swing.JTabbedPane jTabbedPane1 = (javax.swing.JTabbedPane)this.getParent();
        int index = jTabbedPane1.getTabCount();

        MouseListener close = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Component obj = e.getComponent();
                java.awt.Container panel = obj.getParent();
                ButtonClose buttonClose = (ButtonClose)panel.getParent();
                javax.swing.JTabbedPane tabPanel = buttonClose.getOwnerPanel();
                tabPanel.remove(buttonClose.getTabComponent());
                tabPanel.remove(buttonClose);
                
                Boolean success = true;
                //your code to remove component
            }
        };
        
        String caseName = KnowledgeGraphPreferences.getEffectiveFearCaseName();
        String url = FearEndpoints.webUiUrl(caseName);
        
        final int showDevToolsCommandId = CefMenuModel.MenuId.MENU_ID_USER_FIRST;

        CefApp cefApp = CefApp.getInstance();
        CefClient client = cefApp.createClient();
        client.addMessageRouter(CefMessageRouter.create());

        // DevTools is opened on demand via the "Show DevTools" entry in the
        // browser's right-click context menu, added below.
        client.addContextMenuHandler(new CefContextMenuHandlerAdapter() {
            @Override
            public void onBeforeContextMenu(CefBrowser menuBrowser, CefFrame frame,
                    CefContextMenuParams params, CefMenuModel model) {
                model.addSeparator();
                model.addItem(showDevToolsCommandId, "Show DevTools");
            }

            @Override
            public boolean onContextMenuCommand(CefBrowser menuBrowser, CefFrame frame,
                    CefContextMenuParams params, int commandId, int eventFlags) {
                if (commandId == showDevToolsCommandId) {
                    menuBrowser.openDevTools();
                    return true;
                }
                return false;
            }
        });

        CefBrowser browser = client.createBrowser(url, false, false);
        Component comp = browser.getUIComponent();

        jTabbedPane1.add(comp, index);

        ButtonClose buttonClose  = new ButtonClose("New Query Tab", null,jTabbedPane1, comp, close);

        jTabbedPane1.setTabComponentAt(index, buttonClose);
        jTabbedPane1.validate();
        jTabbedPane1.setSelectedIndex(index);
    }//GEN-LAST:event_jLabel1MouseClicked

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        showAboutDialog();
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * Displays an "About" dialog describing the FEAR Autopsy module and
     * crediting its authors.
     */
    private void showAboutDialog() {
        String about = "FEAR Framework - Autopsy Module\n\n"
                + "The FEAR (Forensic Extraction and Representation) Framework transforms\n"
                + "digital forensic artefacts into a semantic knowledge graph, and provides\n"
                + "querying, visualisation, and AI-assisted analysis from within Autopsy.\n\n"
                + "Developed by Allan Korol and Leslie F. Sikos.";

        javax.swing.Icon icon = null;
        try {
            icon = new javax.swing.ImageIcon(getClass().getResource(
                    "/com/fearlanguage/knowledgegraph/images/FEAR-Logo-32.png"));
        } catch (Throwable ignored) {
            // Fall back to the default information icon if the logo cannot be loaded.
        }

        javax.swing.JOptionPane.showMessageDialog(this, about, "About FEAR",
                javax.swing.JOptionPane.INFORMATION_MESSAGE, icon);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel homeLinkPanel;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JLabel leadingLogo;
    private javax.swing.JLabel leadingTitle;
    // End of variables declaration//GEN-END:variables
}
