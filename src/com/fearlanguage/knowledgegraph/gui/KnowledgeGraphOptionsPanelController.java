/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.fearlanguage.knowledgegraph.gui;

import com.fearlanguage.knowledgegraph.gui.KnowledgeGraphOptionsPanel;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

/**
 * OptionsPanelController for the ImageGalleryOptionPanel
 */
@OptionsPanelController.TopLevelRegistration(
        categoryName = "#OptionsCategory_Name_Options",
        iconBase = "com/fearlanguage/knowledgegraph/images/FEAR-Logo-32.png",
        keywords = "#OptionsCategory_Keywords_Options",
        keywordsCategory = "Options",
        position = 16
)
@org.openide.util.NbBundle.Messages({"OptionsCategory_Name_Options=Knowledge Graph", "OptionsCategory_Keywords_Options=knowledge graph category"})
public final class KnowledgeGraphOptionsPanelController extends OptionsPanelController {

    private KnowledgeGraphOptionsPanel panel;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private boolean changed;

    @Override
    public void update() {
        getPanel().load();
        changed = false;
    }

    @Override
    public void applyChanges() {
        SwingUtilities.invokeLater(() -> {
            getPanel().store();
            changed = false;
        });
    }

    @Override
    public void cancel() {
        // need not do anything special, if no changes have been persisted yet
    }

    @Override
    public boolean isValid() {
        return getPanel().valid();
    }

    @Override
    public boolean isChanged() {
        return changed;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null; // new HelpCtx("...ID") if you have a help set
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return getPanel();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    private KnowledgeGraphOptionsPanel getPanel() {
        if (panel == null) {
            panel = new KnowledgeGraphOptionsPanel(this);
        }
        return panel;
    }

    void changed() {
        if (!changed) {
            changed = true;
            pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }
}
