package com.dlightplanner.utils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class FileUtils {

    public static final Color PRIMARY_COLOR = new Color(74, 144, 226);      // Soft blue
    public static final Color ACCENT_COLOR = new Color(245, 166, 35);       // Orange
    public static final Color BACKGROUND_COLOR = Color.WHITE;
    public static final Color FOREGROUND_COLOR = new Color(33, 33, 33); 
    public static final Color PANEL_BORDER_COLOR = Color.GRAY;
    public static final Color TABLE_HEADER_BG = PRIMARY_COLOR;
    public static final Color TABLE_HEADER_FG = Color.WHITE;

    public static final Font HEADING_FONT = new Font("Arial", Font.BOLD, 16);
    public static final Font LABEL_FONT = new Font("Arial", Font.PLAIN, 14);
    public static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 14);
    public static final Font TABLE_FONT = new Font("Arial", Font.PLAIN, 14);
    public static final Font TOTAL_FONT = new Font("Arial", Font.BOLD, 20);

    public static final Border PANEL_BORDER = BorderFactory.createLineBorder(PANEL_BORDER_COLOR, 1);

    public static void styleButton(JButton button) {
        button.setBackground(ACCENT_COLOR);
        button.setForeground(Color.WHITE);
        button.setFont(BUTTON_FONT);
    }

    public static void styleLabel(JLabel label) {
        label.setFont(LABEL_FONT);
        label.setForeground(Color.BLACK);
    }

    public static void styleHeading(JLabel label) {
        label.setFont(HEADING_FONT);
        label.setForeground(PRIMARY_COLOR);
    }

    public static void stylePanel(JPanel panel) {
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(PANEL_BORDER);
    }

    public static void styleTable(JTable table) {
        table.setFont(TABLE_FONT);
        table.setRowHeight(25);
        table.getTableHeader().setBackground(TABLE_HEADER_BG);
        table.getTableHeader().setForeground(TABLE_HEADER_FG);
        table.getTableHeader().setFont(HEADING_FONT);
    }
    public static void styleTotal(JLabel label) {
        label.setFont(TOTAL_FONT);
        label.setForeground(new Color(0, 100, 0)); // dark green
        label.setHorizontalAlignment(SwingConstants.CENTER);
    }
    public static void styleCheckBox(JCheckBox checkBox) {
    checkBox.setFont(new Font("Arial", Font.PLAIN, 14));
    checkBox.setBackground(BACKGROUND_COLOR);
    checkBox.setForeground(FOREGROUND_COLOR);
}
}
