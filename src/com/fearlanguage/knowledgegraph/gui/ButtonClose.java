/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.fearlanguage.knowledgegraph.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.metal.MetalIconFactory;

/**
 *
 * @author Allan
 */
public class ButtonClose extends JPanel {

    JTabbedPane owner = null;
    Component panelComponent = null;
    public JTabbedPane getOwnerPanel(){
        return owner;
    }
    
    public Component getTabComponent(){
        return panelComponent;
    }
    
    public ButtonClose(final String title, Icon icon, JTabbedPane ownerPane, Component panelComp, MouseListener e) {
        panelComponent= panelComp;
        owner = ownerPane;
        JLabel ic =null;
        if(icon != null){
            ic = new JLabel(icon);
            ic.setSize(icon.getIconWidth(), icon.getIconHeight());
        }

        JLabel text= new JLabel(title);
        text.setOpaque(false);
        //text.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

        ButtonTab button = new ButtonTab();
        button.addMouseListener(e);
        //button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        if(icon != null)
            p.setSize(getWidth() - icon.getIconWidth(), 15);
        p.add(text);
        p.add(button);

        if(ic != null)
            add(ic);

        setOpaque(false);
        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0,0));
        setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        add(p);
    }

    private class ButtonTab extends JButton {
        public ButtonTab() {
            int size = 13;
            setPreferredSize(new Dimension(size, size));
            setToolTipText("Close");

            setUI(new BasicButtonUI());

            setFocusable(false);
            setBorderPainted(false);

            addMouseListener(listener);
            setRolloverEnabled(true);
        }

        @Override
        public void updateUI() {
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int BUTTON_SIZE = getWidth();
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw the button background
            if (getModel().isPressed()) {
                g2d.setColor(Color.RED);
            g2d.fillRect(0, 0, BUTTON_SIZE, BUTTON_SIZE);
            } else if (getModel().isRollover()) {
                g2d.setColor(Color.RED);
            g2d.fillRect(0, 0, BUTTON_SIZE, BUTTON_SIZE);
            }
            
            // Draw the red 'X'
            if (getModel().isRollover())
                g2d.setColor(Color.WHITE);
            else
                g2d.setColor(Color.DARK_GRAY);
            
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(3, 3, BUTTON_SIZE - 3, BUTTON_SIZE - 3);
            g2d.drawLine(3, BUTTON_SIZE - 3, BUTTON_SIZE - 3, 3);
        }
    }

    private final MouseListener listener = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setContentAreaFilled(true);
                button.setBackground(new Color(215, 65, 35));
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setContentAreaFilled(false); //transparent
            }
        }
    };
}