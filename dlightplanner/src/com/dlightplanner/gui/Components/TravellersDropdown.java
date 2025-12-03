package com.dlightplanner.gui.Components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TravellersDropdown extends JPanel {
    private JLabel displayLabel;
    private JSpinner adultsSpinner, childrenSpinner, infantsSpinner;
    private JPanel popupPanel;
    private JWindow popup;
    private int adults = 1, children = 0, infants = 0;

    public TravellersDropdown() {
        setLayout(new BorderLayout());
        setOpaque(false);

        displayLabel = new JLabel("1 Adult");
        displayLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Slightly bigger text
        displayLabel.setForeground(new Color(50, 60, 80));
        displayLabel.setOpaque(true);
        displayLabel.setBackground(Color.WHITE);
        displayLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        displayLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(10, 12, 10, 12)
        ));
        displayLabel.setPreferredSize(new Dimension(131, 28));
        displayLabel.setMaximumSize(new Dimension(131, 28));
        
        add(displayLabel, BorderLayout.CENTER);

        createPopupPanel();

        displayLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showPopup();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                displayLabel.setBackground(new Color(250, 250, 255));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                displayLabel.setBackground(Color.WHITE);
            }
        });
    }
    
    private void createPopupPanel() {
        popupPanel = new JPanel();
        popupPanel.setLayout(new BoxLayout(popupPanel, BoxLayout.Y_AXIS));
        popupPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        popupPanel.setBackground(Color.WHITE);

        JPanel adultsPanel = createTravellerRow("Adults (15+)", 1, 20);
        adultsSpinner = (JSpinner) adultsPanel.getComponent(1);
        popupPanel.add(adultsPanel);
        
        popupPanel.add(Box.createVerticalStrut(15));

        JPanel childrenPanel = createTravellerRow("Children (3-14)", 0, 10);
        childrenSpinner = (JSpinner) childrenPanel.getComponent(1);
        popupPanel.add(childrenPanel);
        
        popupPanel.add(Box.createVerticalStrut(15));

        JPanel infantsPanel = createTravellerRow("Infants (under 2)", 0, 5);
        infantsSpinner = (JSpinner) infantsPanel.getComponent(1);
        popupPanel.add(infantsPanel);

        adultsSpinner.addChangeListener(e -> updateDisplay());
        childrenSpinner.addChangeListener(e -> updateDisplay());
        infantsSpinner.addChangeListener(e -> updateDisplay());
    }
    
    private JPanel createTravellerRow(String label, int min, int max) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        labelComponent.setForeground(new Color(60, 70, 90));
        
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(min, min, max, 1));
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        spinner.setPreferredSize(new Dimension(80, 30));
        
        row.add(labelComponent, BorderLayout.WEST);
        row.add(spinner, BorderLayout.EAST);
        
        return row;
    }
    
    private void updateDisplay() {
        adults = (Integer) adultsSpinner.getValue();
        children = (Integer) childrenSpinner.getValue();
        infants = (Integer) infantsSpinner.getValue();
        
        StringBuilder text = new StringBuilder();
        if (adults > 0) {
            text.append(adults).append(adults == 1 ? " Adult" : " Adults");
        }
        if (children > 0) {
            if (text.length() > 0) text.append(", ");
            text.append(children).append(children == 1 ? " Child" : " Children");
        }
        if (infants > 0) {
            if (text.length() > 0) text.append(", ");
            text.append(infants).append(infants == 1 ? " Infant" : " Infants");
        }
        
        if (text.length() == 0) {
            text.append("1 Adult");
            adultsSpinner.setValue(1);
        }
        
        displayLabel.setText(text.toString());
    }
    
    private void showPopup() {
        if (popup != null && popup.isVisible()) {
            popup.setVisible(false);
            return;
        }

        popup = new JWindow((Frame) SwingUtilities.getWindowAncestor(this));
        popup.getContentPane().add(popupPanel);
        popup.pack();

        Point location = displayLabel.getLocationOnScreen();
        popup.setLocation(location.x, location.y + displayLabel.getHeight());

        popup.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 0) {
                    Point p = SwingUtilities.convertPoint(popup, e.getPoint(), popupPanel);
                    if (!popupPanel.contains(p)) {
                        popup.setVisible(false);
                    }
                }
            }
        });

        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            if (e.getID() == MouseEvent.MOUSE_PRESSED && popup != null && popup.isVisible()) {
                MouseEvent me = (MouseEvent) e;
                Point p = me.getPoint();
                SwingUtilities.convertPointToScreen(p, (Component) me.getSource());
                Point popupLoc = popup.getLocationOnScreen();
                Rectangle popupBounds = new Rectangle(popupLoc, popup.getSize());
                if (!popupBounds.contains(p) && !displayLabel.getBounds().contains(
                    SwingUtilities.convertPoint((Component) me.getSource(), me.getPoint(), displayLabel))) {
                    popup.setVisible(false);
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
        
        popup.setVisible(true);
    }
    
    public int getAdults() {
        return adults;
    }
    
    public int getChildren() {
        return children;
    }
    
    public int getInfants() {
        return infants;
    }
    
    public void setAdults(int adults) {
        this.adults = adults;
        if (adultsSpinner != null) {
            adultsSpinner.setValue(adults);
        }
        updateDisplay();
    }
    
    public void setChildren(int children) {
        this.children = children;
        if (childrenSpinner != null) {
            childrenSpinner.setValue(children);
        }
        updateDisplay();
    }
    
    public void setInfants(int infants) {
        this.infants = infants;
        if (infantsSpinner != null) {
            infantsSpinner.setValue(infants);
        }
        updateDisplay();
    }
}

