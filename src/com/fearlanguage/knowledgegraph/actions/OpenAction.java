/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.fearlanguage.knowledgegraph.actions;
import com.fearlanguage.knowledgegraph.CaseWatcher;
import com.fearlanguage.knowledgegraph.Diagnostics;
import com.fearlanguage.knowledgegraph.KnowledgeGraphPreferences;
import com.fearlanguage.knowledgegraph.gui.KnowledgeGraphTopComponent;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.sleuthkit.autopsy.core.RuntimeProperties;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.datamodel.TskCoreException;

@ActionID(category = "Tools", id = "com.fearlanguage.knowledgegraph.OpenAction")
@ActionReferences(value = {
    @ActionReference(path = "Menu/Tools", position = 101)
    ,
    @ActionReference(path = "Toolbars/Case", position = 101)})
@ActionRegistration(displayName = "#CTL_OpenAction", lazy = false)
public final class OpenAction extends CallableSystemAction {
    private static final CaseWatcher watcherInstance = CaseWatcher.getInstance();

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(OpenAction.class.getName());
    private static final String VIEW_KNOWLEDGE_GRAPH = resolveActionName();

    static {
        // Earliest point we control: confirms the action class is loaded by the
        // module system. If the breadcrumb file shows nothing, the module/class
        // never loaded (a packaging/dependency problem), not a runtime exception.
        Diagnostics.breadcrumb("OpenAction class loaded.");
    }

    private static String resolveActionName() {
        try {
            return NbBundle.getMessage(OpenAction.class, "CTL_OpenAction");
        } catch (RuntimeException ex) {
            // A missing bundle here would otherwise throw ExceptionInInitializerError
            // and silently prevent the action from being registered.
            Diagnostics.reportError("OpenAction: failed to resolve CTL_OpenAction from the bundle; "
                    + "falling back to a default name.", ex);
            return "Knowledge Graph";
        }
    }

    private final PropertyChangeListener pcl;
    private final JMenuItem menuItem;
    // Icon is loaded defensively in the constructor: a missing resource in a
    // packaged module would otherwise make ImageIcon(null) throw and prevent the
    // whole action (toolbar button + menu item) from being created.
    private final JButton toolbarButton = new JButton();

    // The single action instance, so the enabled state can be re-evaluated from
    // elsewhere (e.g. after the user saves settings in the options panel).
    private static volatile OpenAction instance;

    public OpenAction() {
        super();
        Diagnostics.breadcrumb("OpenAction constructor: start.");
        try {
            instance = this;
            toolbarButton.setText(getName());
            URL iconUrl = getClass().getResource("/com/fearlanguage/knowledgegraph/images/FEAR-Logo-24.png");
            if (iconUrl != null) {
                toolbarButton.setIcon(new ImageIcon(iconUrl));
            } else {
                Diagnostics.reportWarning("Toolbar icon resource not found: "
                        + "/com/fearlanguage/knowledgegraph/images/FEAR-Logo-24.png "
                        + "(the action will still appear, without an icon).");
            }
            toolbarButton.addActionListener(actionEvent -> performAction());
            menuItem = super.getMenuPresenter();
            pcl = (PropertyChangeEvent evt) -> {
                if (evt.getPropertyName().equals(Case.Events.CURRENT_CASE.toString())) {
                    setEnabled(computeEnabled());
                }
            };
            Case.addPropertyChangeListener(pcl);
            this.setEnabled(computeEnabled());
            Diagnostics.breadcrumb("OpenAction constructor: completed (action registered).");
        } catch (RuntimeException ex) {
            Diagnostics.reportError("Failed to initialise the Knowledge Graph toolbar/menu action.", ex);
            throw ex;
        }
    }

    /**
     * The action is enabled only when running with a GUI, a case with data is
     * open, and the module is configured for that case. (Configuration is
     * per-case; an unconfigured case leaves the action disabled.)
     */
    private boolean computeEnabled() {
        if (!RuntimeProperties.runningWithGUI()) {
            return false;
        }
        try {
            Case openCase = Case.getCurrentCaseThrows();
            if (!openCase.hasData()) {
                return false;
            }
        } catch (NoCurrentCaseException ex) {
            return false;
        }
        return KnowledgeGraphPreferences.isConfigured();
    }

    /**
     * Re-evaluates and applies the action's enabled state. Safe to call from any
     * thread; the update is applied on the EDT. Invoked, for example, after the
     * options panel saves new settings so the toolbar reflects them immediately.
     */
    public static void refreshEnabled() {
        OpenAction a = instance;
        if (a != null) {
            SwingUtilities.invokeLater(() -> a.setEnabled(a.computeEnabled()));
        }
    }

    @Override
    public boolean isEnabled() {
        return computeEnabled();
    }

    /**
     * Returns the toolbar component of this action
     *
     * @return component the toolbar button
     */
    @Override
    public Component getToolbarPresenter() {

        return toolbarButton;
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return menuItem;
    }

    /**
     * Set this action to be enabled/disabled
     *
     * @param value whether to enable this action or not
     */
    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        menuItem.setEnabled(value);
        toolbarButton.setEnabled(value);
    }

    @Override
    public void performAction() {
        Diagnostics.breadcrumb("OpenAction.performAction: invoked.");
        //check case
        try {
            Case.getCurrentCaseThrows();
        } catch (NoCurrentCaseException ex) {
            logger.log(Level.SEVERE, "No current case", ex);
            return;
        }
        openTopComponent();
    }

    private void openTopComponent() {
        SwingUtilities.invokeLater(() -> {
            try {
                Diagnostics.breadcrumb("OpenAction.openTopComponent: opening top component on EDT.");
                KnowledgeGraphTopComponent.openTopComponent();
            } catch (Throwable ex) {
                Diagnostics.reportError("Failed to open the Knowledge Graph window.", ex);
            }
        });
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
    public boolean asynchronous() {
        return true; // run off edt
    }
}
