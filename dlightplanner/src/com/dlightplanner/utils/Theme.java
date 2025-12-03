package com.dlightplanner.utils;

import javax.swing.*;
import java.awt.*;

public class Theme {



    public static final Color BACKGROUND_COLOR = new Color(163, 210, 202); // Soft Sky Blue
    public static final Color PANEL_COLOR = new Color(246, 226, 179);      // Sand Beige
    public static final Color BUTTON_COLOR = new Color(242, 140, 140);     // Sunset Coral
    public static final Color BUTTON_HOVER = new Color(59, 122, 87);       // Forest Green
    public static final Color TEXT_COLOR = new Color(80, 80, 80);          // Slate Gray
    public static final Color HEADER_COLOR = new Color(59, 122, 87);       // Forest Green



    public static final Font HEADING_FONT = new Font("Montserrat", Font.BOLD, 24);
    public static final Font SUBHEADING_FONT = new Font("Poppins", Font.BOLD, 18);
    public static final Font BODY_FONT = new Font("Lato", Font.PLAIN, 14);
    public static final Font BUTTON_FONT = new Font("Poppins", Font.BOLD, 16);
    public static final Font TOTAL_FONT = new Font("Montserrat", Font.BOLD, 20);




    public static void stylePanel(JPanel panel) {
        panel.setBackground(PANEL_COLOR);
        panel.setForeground(TEXT_COLOR);
    }

    public static void styleLabel(JLabel label) {
        label.setFont(BODY_FONT);
        label.setForeground(TEXT_COLOR);
    }

    public static void styleHeading(JLabel label) {
        label.setFont(HEADING_FONT);
        label.setForeground(HEADER_COLOR);
    }

    public static void styleSubHeading(JLabel label) {
        label.setFont(SUBHEADING_FONT);
        label.setForeground(HEADER_COLOR);
    }

    public static void styleButton(JButton button) {
        button.setBackground(BUTTON_COLOR);
        button.setForeground(Color.WHITE);
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_COLOR);
            }
        });
    }

    public static void styleTotal(JLabel label) {
        label.setFont(TOTAL_FONT);
        label.setForeground(HEADER_COLOR);
    }
}
