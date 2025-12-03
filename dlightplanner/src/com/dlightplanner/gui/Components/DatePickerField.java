package com.dlightplanner.gui.Components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Lightweight date picker component that shows a calendar dialog when the user
 * clicks the calendar button.
 */
public class DatePickerField extends JPanel {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final JTextField dateField;
    private final JButton calendarButton;
    private LocalDate selectedDate;

    public DatePickerField() {
        setLayout(new BorderLayout(4, 0));

        dateField = new JTextField(10);
        dateField.setEditable(false);
        dateField.setBackground(Color.WHITE);
        dateField.setFocusable(false);

        calendarButton = new JButton("\uD83D\uDCC5");
        calendarButton.setMargin(new Insets(2, 6, 2, 6));
        calendarButton.addActionListener(this::showCalendarDialog);

        add(dateField, BorderLayout.CENTER);
        add(calendarButton, BorderLayout.EAST);
    }

    private void showCalendarDialog(ActionEvent event) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Select Date", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));

        YearMonth[] visibleMonth = {YearMonth.from(selectedDate != null ? selectedDate : LocalDate.now())};

        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 16f));

        JButton prevBtn = new JButton("<");
        JButton nextBtn = new JButton(">");

        JPanel header = new JPanel(new BorderLayout());
        JPanel navPanel = new JPanel(new BorderLayout());
        navPanel.add(prevBtn, BorderLayout.WEST);
        navPanel.add(nextBtn, BorderLayout.EAST);
        header.add(navPanel, BorderLayout.EAST);
        header.add(monthLabel, BorderLayout.CENTER);

        JPanel calendarGrid = new JPanel(new GridLayout(0, 7, 4, 4));

        Runnable refreshCalendar = () -> {
            calendarGrid.removeAll();
            monthLabel.setText(visibleMonth[0].getMonth().name() + " " + visibleMonth[0].getYear());
            for (DayOfWeek day : DayOfWeek.values()) {
                JLabel dayLabel = new JLabel(day.name().substring(0, 3), SwingConstants.CENTER);
                dayLabel.setFont(dayLabel.getFont().deriveFont(Font.BOLD));
                calendarGrid.add(dayLabel);
            }

            int firstDayIndex = DayOfWeek.MONDAY.getValue();
            int leadingBlanks = (visibleMonth[0].atDay(1).getDayOfWeek().getValue() - firstDayIndex + 7) % 7;
            for (int i = 0; i < leadingBlanks; i++) {
                calendarGrid.add(new JLabel(""));
            }

            LocalDate today = LocalDate.now();
            for (int day = 1; day <= visibleMonth[0].lengthOfMonth(); day++) {
                LocalDate date = visibleMonth[0].atDay(day);
                boolean isPast = date.isBefore(today);
                
                JButton dayButton = new JButton();
                
                if (isPast) {

                    dayButton.setEnabled(false);
                    dayButton.setForeground(new Color(180, 180, 180));
                    dayButton.setBackground(new Color(245, 245, 245));

                    dayButton.setText("<html><strike>" + day + "</strike></html>");
                } else {
                    dayButton.setText(String.valueOf(day));
                    final int dayValue = day; // Capture for lambda
                    dayButton.addActionListener(e -> {
                        selectedDate = visibleMonth[0].atDay(dayValue);
                        dateField.setText(selectedDate.format(DISPLAY_FORMAT));
                        dialog.dispose();
                    });
                }
                calendarGrid.add(dayButton);
            }

            calendarGrid.revalidate();
            calendarGrid.repaint();
        };

        prevBtn.addActionListener(e -> {
            visibleMonth[0] = visibleMonth[0].minusMonths(1);
            refreshCalendar.run();
        });
        nextBtn.addActionListener(e -> {
            visibleMonth[0] = visibleMonth[0].plusMonths(1);
            refreshCalendar.run();
        });

        JButton todayButton = new JButton("Today");
        todayButton.addActionListener(e -> {
            selectedDate = LocalDate.now();
            dateField.setText(selectedDate.format(DISPLAY_FORMAT));
            visibleMonth[0] = YearMonth.from(selectedDate);
            refreshCalendar.run();
        });

        dialog.add(header, BorderLayout.NORTH);
        dialog.add(calendarGrid, BorderLayout.CENTER);
        dialog.add(todayButton, BorderLayout.SOUTH);

        refreshCalendar.run();

        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public String getFormattedDate() {
        return selectedDate != null ? selectedDate.format(DISPLAY_FORMAT) : "";
    }

    public void setSelectedDate(LocalDate date) {
        this.selectedDate = date;
        dateField.setText(date != null ? date.format(DISPLAY_FORMAT) : "");
    }
}

