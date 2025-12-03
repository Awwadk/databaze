package com.dlightplanner.gui;

import com.dlightplanner.gui.Components.DatePickerField;
import com.dlightplanner.gui.Components.TravellersDropdown;
import com.dlightplanner.models.Airport;
import com.dlightplanner.models.City;
import com.dlightplanner.models.FlightSearchRequest;
import com.dlightplanner.models.MultiCityTripRequest;
import com.dlightplanner.services.AirportService;
import com.dlightplanner.services.CityService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

public class HomePage extends JFrame {

    private AirportService airportService;
    private CityService cityService;

    private JComboBox<String> originCombo;
    private JComboBox<String> destinationCombo;
    private JRadioButton oneWayBtn, roundTripBtn, multiCityBtn;
    private DatePickerField startDatePicker, returnDatePicker;
    private TravellersDropdown travellersDropdown;
    private List<String> airportOptions;
    private float gradientProgress = 0.0f;
    
    private JPanel multiCityPanel;
    private JPanel destinationsContainer;
    private List<DestinationRow> destinationRows;
    private JComboBox<String> originComboMulti;
    private TravellersDropdown multiCityTravellers;

    public HomePage() {
        setTitle("Voya - Trip Planner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());
        
        addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                if ((e.getNewState() & JFrame.ICONIFIED) != 0) {
                    setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            }
        });

        airportService = new AirportService();
        airportService.loadAirportsFromJson("resources/airports.json");
        airportOptions = buildAirportOptions();

        cityService = new CityService();
        cityService.loadCitiesFromJson("resources/cities.json");

        ThemedBackgroundPanel bgPanel = new ThemedBackgroundPanel();
        bgPanel.setLayout(new BorderLayout());
        
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(0, 0, 0, 0));
        
        mainContent.add(createHeaderSection());
        mainContent.add(createSearchSection());
        mainContent.add(Box.createVerticalStrut(0));
        mainContent.add(createDiscoverSection());
        mainContent.add(createCityCardsPanel());
        mainContent.add(Box.createVerticalStrut(10));
        
        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        bgPanel.add(scrollPane, BorderLayout.CENTER);
        add(bgPanel, BorderLayout.CENTER);

        startBackgroundAnimation();

        setVisible(true);
    }
    
    private void startBackgroundAnimation() {
        javax.swing.Timer animationTimer = new javax.swing.Timer(16, e -> {
            gradientProgress += 0.002f;
            if (gradientProgress >= 1.0f) {
                gradientProgress = 0.0f;
            }
            repaint();
        });
        animationTimer.start();
    }

    private JPanel createHeaderSection() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(20, 20, 10, 20)); // Reduced side padding
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Book your Trip with Voya");
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 42));
        titleLabel.setForeground(new Color(40, 50, 70));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(titleLabel);
        
        header.add(Box.createVerticalStrut(20)); // 1 line space after title

        JPanel tripTypePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        tripTypePanel.setOpaque(false);
        tripTypePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        oneWayBtn = createStyledRadioButton("One-way");
        roundTripBtn = createStyledRadioButton("Round-trip");
        multiCityBtn = createStyledRadioButton("Multi-city");
        ButtonGroup tripGroup = new ButtonGroup();
        tripGroup.add(oneWayBtn);
        tripGroup.add(roundTripBtn);
        tripGroup.add(multiCityBtn);
        oneWayBtn.setSelected(true);
        
        tripTypePanel.add(oneWayBtn);
        tripTypePanel.add(roundTripBtn);
        tripTypePanel.add(multiCityBtn);
        
        header.add(tripTypePanel);
        
        header.add(Box.createVerticalStrut(20)); // 1 line space after trip type buttons
        
        return header;
    }
    
    private JPanel createSearchSection() {
        JPanel searchSection = new JPanel();
        searchSection.setLayout(new BoxLayout(searchSection, BoxLayout.Y_AXIS));
        searchSection.setOpaque(false);
        searchSection.setBorder(new EmptyBorder(0, 20, 20, 20)); // 1 line space (20px) after travel boxes
        searchSection.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel singleCityPanel = createSingleCityPanel();
        singleCityPanel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center align boxes

        multiCityPanel = createMultiCityPanel();
        multiCityPanel.setVisible(false);
        multiCityPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel contentPanel = new JPanel(new CardLayout());
        contentPanel.setOpaque(false);
        contentPanel.add(singleCityPanel, "SINGLE");
        contentPanel.add(multiCityPanel, "MULTI");

        ItemListener tripToggleListener = e -> {
            boolean isMultiCity = multiCityBtn.isSelected();
            CardLayout cl = (CardLayout) contentPanel.getLayout();
            if (isMultiCity) {
                cl.show(contentPanel, "MULTI");
            } else {
                cl.show(contentPanel, "SINGLE");
            }
        };
        oneWayBtn.addItemListener(tripToggleListener);
        roundTripBtn.addItemListener(tripToggleListener);
        multiCityBtn.addItemListener(tripToggleListener);
        
        searchSection.add(contentPanel);
        
        return searchSection;
    }
    
    private JPanel createDiscoverSection() {
        JPanel discoverSection = new JPanel();
        discoverSection.setLayout(new BoxLayout(discoverSection, BoxLayout.Y_AXIS));
        discoverSection.setOpaque(false);
        discoverSection.setBorder(new EmptyBorder(0, 20, 5, 20)); // No top padding - spacing handled by search section
        discoverSection.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel discoverLabel = new JLabel("Discover places with Voya");
        discoverLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        Font discoverFont = new Font("Segoe UI", Font.PLAIN, 24);
        discoverLabel.setFont(discoverFont);
        discoverLabel.setForeground(new Color(100, 150, 220));
        discoverLabel.setBorder(new EmptyBorder(0, 0, 0, 0)); // No label padding
        discoverSection.add(discoverLabel);
        
        return discoverSection;
    }

    private JPanel createSingleCityPanel() {

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0)); // Centered
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(8, 0, 0, 0)); // No bottom padding - no space before discover

        JPanel originPanel = createFieldBox("From", "Origin");
        originCombo = new JComboBox<>(new DefaultComboBoxModel<>(airportOptions.toArray(new String[0])));
        originCombo.setEditable(true);
        enableAutoComplete(originCombo, airportOptions);
        originCombo.setSelectedIndex(-1);
        ((JTextComponent) originCombo.getEditor().getEditorComponent()).setText("");
        styleComboBox(originCombo);
        originPanel.add(originCombo, BorderLayout.CENTER);
        panel.add(originPanel);

        JPanel destPanel = createFieldBox("To", "Destination");
        destinationCombo = new JComboBox<>(new DefaultComboBoxModel<>(airportOptions.toArray(new String[0])));
        destinationCombo.setEditable(true);
        enableAutoComplete(destinationCombo, airportOptions);
        destinationCombo.setSelectedIndex(-1);
        ((JTextComponent) destinationCombo.getEditor().getEditorComponent()).setText("");
        styleComboBox(destinationCombo);
        destPanel.add(destinationCombo, BorderLayout.CENTER);
        panel.add(destPanel);

        JPanel datePanel = createFieldBox("Departure", "Date");
        startDatePicker = new DatePickerField();
        styleDatePicker(startDatePicker);
        datePanel.add(startDatePicker, BorderLayout.CENTER);
        panel.add(datePanel);

        returnDatePicker = new DatePickerField();
        styleDatePicker(returnDatePicker);
        returnDatePicker.setEnabled(false);
        JPanel returnDatePanel = createFieldBox("Return", "Date");
        returnDatePanel.add(returnDatePicker, BorderLayout.CENTER);
        returnDatePanel.setVisible(false);
        panel.add(returnDatePanel);

        ItemListener dateToggleListener = e -> {
            boolean roundTrip = roundTripBtn.isSelected();
            returnDatePanel.setVisible(roundTrip);
            returnDatePicker.setEnabled(roundTrip);
            if (!roundTrip) {
                returnDatePicker.setSelectedDate(null);
            }
            panel.revalidate();
            panel.repaint();
        };
        roundTripBtn.addItemListener(dateToggleListener);
        oneWayBtn.addItemListener(dateToggleListener);

        JPanel travellersPanel = createFieldBox("Travellers", "Passengers");
        travellersDropdown = new TravellersDropdown();
        travellersPanel.add(travellersDropdown, BorderLayout.CENTER);
        panel.add(travellersPanel);

        JButton searchButton = createSearchButton("Search Flights");
        searchButton.addActionListener(e -> handleFlightSearch());
        panel.add(searchButton);

        return panel;
    }
    
    private JPanel createFieldBox(String label, String placeholder) {
        JPanel box = new JPanel(new BorderLayout(0, 4));
        box.setOpaque(true);
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(0, 0, 0, 0)
        ));

        box.setPreferredSize(new Dimension(155, 65));
        box.setMinimumSize(new Dimension(155, 65));
        box.setMaximumSize(new Dimension(155, 65));
        
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Segoe UI", Font.PLAIN, 12)); // Slightly bigger text
        labelComponent.setForeground(new Color(100, 100, 100));
        box.add(labelComponent, BorderLayout.NORTH);
        
        return box;
    }
    
    private void styleComboBox(JComboBox<String> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Slightly bigger text
        combo.setBorder(null);
        combo.setPreferredSize(new Dimension(131, 28));
        combo.setMaximumSize(new Dimension(131, 28));
    }
    
    private void styleDatePicker(DatePickerField picker) {
        picker.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Slightly bigger text
        picker.setPreferredSize(new Dimension(131, 28));
        picker.setMaximumSize(new Dimension(131, 28));
    }
    
    private JButton createSearchButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 15)); // Slightly bigger text
        button.setPreferredSize(new Dimension(155, 65));
        button.setMinimumSize(new Dimension(155, 65));
        button.setMaximumSize(new Dimension(155, 65));
        button.setBackground(new Color(100, 150, 220));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(80, 130, 200));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(100, 150, 220));
            }
        });
        
        return button;
    }
    
    private JRadioButton createStyledRadioButton(String text) {
        JRadioButton radio = new JRadioButton(text);
        radio.setFont(new Font("Segoe UI", Font.BOLD, 16));
        radio.setForeground(new Color(50, 60, 80));
        radio.setOpaque(false);
        radio.setCursor(new Cursor(Cursor.HAND_CURSOR));

        radio.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                radio.setForeground(new Color(100, 150, 220));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!radio.isSelected()) {
                    radio.setForeground(new Color(50, 60, 80));
                }
            }
        });

        radio.addItemListener(e -> {
            if (radio.isSelected()) {
                radio.setForeground(new Color(100, 150, 220));
            } else {
                radio.setForeground(new Color(50, 60, 80));
            }
        });
        
        return radio;
    }

    private JPanel createMultiCityPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(20, 0, 20, 0));

        destinationRows = new ArrayList<>();

        JPanel firstLine = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0)); // Reduced gap
        firstLine.setOpaque(false);

        JPanel originPanel = createFieldBox("From", "Origin");
        originComboMulti = new JComboBox<>(new DefaultComboBoxModel<>(airportOptions.toArray(new String[0])));
        originComboMulti.setEditable(true);
        enableAutoComplete(originComboMulti, airportOptions);
        originComboMulti.setSelectedIndex(-1);
        ((JTextComponent) originComboMulti.getEditor().getEditorComponent()).setText("");
        styleComboBox(originComboMulti);
        originPanel.add(originComboMulti, BorderLayout.CENTER);
        firstLine.add(originPanel);

        JPanel travellersPanel = createFieldBox("Travellers", "Passengers");
        multiCityTravellers = new TravellersDropdown();
        travellersPanel.add(multiCityTravellers, BorderLayout.CENTER);
        firstLine.add(travellersPanel);

        JButton searchButton = createSearchButton("Search Flights");
        searchButton.addActionListener(e -> handleMultiCitySearch());
        firstLine.add(searchButton);
        
        panel.add(firstLine);
        panel.add(Box.createVerticalStrut(20));

        destinationsContainer = new JPanel();
        destinationsContainer.setLayout(new BoxLayout(destinationsContainer, BoxLayout.Y_AXIS));
        destinationsContainer.setOpaque(false);
        destinationsContainer.setBorder(new EmptyBorder(10, 0, 10, 0));
        
        JScrollPane destinationsScroll = new JScrollPane(destinationsContainer);
        destinationsScroll.setOpaque(false);
        destinationsScroll.getViewport().setOpaque(false);
        destinationsScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 220), 1),
            "Destinations",
            0, 0,
            new Font("Segoe UI", Font.PLAIN, 14),
            new Color(80, 90, 110)
        ));
        destinationsScroll.setPreferredSize(new Dimension(1100, 300));
        panel.add(destinationsScroll);

        addDestinationRow();

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        buttonPanel.setOpaque(false);
        JButton addCityButton = new JButton("+ Add Destination");
        addCityButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        addCityButton.setForeground(new Color(100, 150, 220));
        addCityButton.setBorderPainted(false);
        addCityButton.setContentAreaFilled(false);
        addCityButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addCityButton.addActionListener(e -> addDestinationRow());
        buttonPanel.add(addCityButton);
        panel.add(buttonPanel);

        return panel;
    }

    private void addDestinationRow() {
        DestinationRow row = new DestinationRow();
        destinationRows.add(row);
        destinationsContainer.add(row.getPanel());
        destinationsContainer.revalidate();
        destinationsContainer.repaint();
    }

    private void removeDestinationRow(DestinationRow row) {
        if (destinationRows.size() <= 1) {
            JOptionPane.showMessageDialog(this, "At least one destination is required.", "Cannot Remove", JOptionPane.WARNING_MESSAGE);
            return;
        }
        destinationRows.remove(row);
        destinationsContainer.remove(row.getPanel());
        destinationsContainer.revalidate();
        destinationsContainer.repaint();
    }

    private class DestinationRow {
        private JPanel panel;
        private JComboBox<String> destinationCombo;
        private DatePickerField departureDatePicker;

        public DestinationRow() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0)); // Reduced gap
            panel.setOpaque(false);
            panel.setBorder(new EmptyBorder(8, 0, 8, 0));

            JPanel destPanel = createFieldBox("To", "Destination");
            destinationCombo = new JComboBox<>(new DefaultComboBoxModel<>(airportOptions.toArray(new String[0])));
            destinationCombo.setEditable(true);
            enableAutoComplete(destinationCombo, airportOptions);
            destinationCombo.setSelectedIndex(-1);
            ((JTextComponent) destinationCombo.getEditor().getEditorComponent()).setText("");
            styleComboBox(destinationCombo);
            destPanel.add(destinationCombo, BorderLayout.CENTER);
            panel.add(destPanel);

            JPanel datePanel = createFieldBox("Departure", "Date");
            departureDatePicker = new DatePickerField();
            styleDatePicker(departureDatePicker);
            datePanel.add(departureDatePicker, BorderLayout.CENTER);
            panel.add(datePanel);

            JButton removeButton = new JButton("Remove");
            removeButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            removeButton.setForeground(new Color(200, 80, 80));
            removeButton.setBorderPainted(false);
            removeButton.setContentAreaFilled(false);
            removeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            removeButton.setPreferredSize(new Dimension(100, 60));
            removeButton.addActionListener(e -> removeDestinationRow(this));
            panel.add(removeButton);
        }

        public JPanel getPanel() {
            return panel;
        }

        public String getDestinationCode() {
            String input = ((JTextComponent) destinationCombo.getEditor().getEditorComponent()).getText().trim();
            return extractIataCode(input);
        }

        public LocalDate getDepartureDate() {
            return departureDatePicker.getSelectedDate();
        }
    }

    private List<String> buildAirportOptions() {
        List<String> options = new ArrayList<>();
        for (Airport a : airportService.getAirports()) {
            options.add(a.getCity() + " (" + a.getIata() + ")");
        }
        return options;
    }

    private void enableAutoComplete(JComboBox<String> comboBox, List<String> masterOptions) {
        JTextComponent editor = (JTextComponent) comboBox.getEditor().getEditorComponent();

        DocumentListener listener = new DocumentListener() {
            private boolean adjusting;

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterOptions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterOptions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterOptions();
            }

            private void filterOptions() {
                if (adjusting) return;
                adjusting = true;
                SwingUtilities.invokeLater(() -> {
                    String typedText = editor.getText();
                    String lower = typedText.toLowerCase();

                    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                    for (String option : masterOptions) {
                        if (option.toLowerCase().contains(lower)) {
                            model.addElement(option);
                        }
                    }
                    comboBox.setModel(model);
                    comboBox.setSelectedItem(null);
                    editor.setText(typedText);
                    editor.setCaretPosition(typedText.length());

                    if (!typedText.isEmpty() && model.getSize() > 0) {
                        comboBox.showPopup();
                    } else {
                        comboBox.hidePopup();
                    }
                    adjusting = false;
                });
            }
        };

        editor.getDocument().addDocumentListener(listener);
        editor.setText("");
        comboBox.setSelectedItem(null);
    }

    private void handleFlightSearch() {

        if (multiCityBtn.isSelected()) {
            handleMultiCitySearch();
            return;
        }

        String originInput = ((JTextComponent) originCombo.getEditor().getEditorComponent()).getText().trim();
        String destInput = ((JTextComponent) destinationCombo.getEditor().getEditorComponent()).getText().trim();
        String originCode = extractIataCode(originInput);
        String destinationCode = extractIataCode(destInput);

        if (originCode == null || destinationCode == null) {
            JOptionPane.showMessageDialog(this, "Please pick valid origin and destination airports.", "Missing details", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (originCode.equalsIgnoreCase(destinationCode)) {
            JOptionPane.showMessageDialog(this, "Origin and destination cannot be the same.", "Invalid route", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate departureDate = startDatePicker.getSelectedDate();
        if (departureDate == null) {
            JOptionPane.showMessageDialog(this, "Choose a departure date to continue.", "Date required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean roundTrip = roundTripBtn.isSelected();
        LocalDate returnDate = returnDatePicker.getSelectedDate();
        if (roundTrip) {
            if (returnDate == null) {
                JOptionPane.showMessageDialog(this, "Add a return date for round-trip searches.", "Date required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!returnDate.isAfter(departureDate)) {
                JOptionPane.showMessageDialog(this, "Return date must be after the departure date.", "Invalid dates", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int adults = travellersDropdown.getAdults();
        int children = travellersDropdown.getChildren();
        int infants = travellersDropdown.getInfants();

        FlightSearchRequest request = new FlightSearchRequest(
                originCode,
                destinationCode,
                departureDate,
                roundTrip ? returnDate : null,
                roundTrip,
                adults,
                children,
                infants
        );

        new FlightPage(request, this);
    }

    private void handleMultiCitySearch() {

        String originInput = ((JTextComponent) originComboMulti.getEditor().getEditorComponent()).getText().trim();
        String originCode = extractIataCode(originInput);
        if (originCode == null) {
            JOptionPane.showMessageDialog(this, "Please select a valid origin airport.", "Missing Origin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (destinationRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add at least one destination.", "No Destinations", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<MultiCityTripRequest.CityLeg> cityLegs = new ArrayList<>();
        LocalDate previousDeparture = null;

        for (int i = 0; i < destinationRows.size(); i++) {
            DestinationRow row = destinationRows.get(i);
            String destCode = row.getDestinationCode();
            LocalDate departureDate = row.getDepartureDate(); // Departure date to this destination

            if (destCode == null) {
                JOptionPane.showMessageDialog(this, "Please select a valid destination for destination " + (i + 1) + ".", "Invalid Destination", JOptionPane.WARNING_MESSAGE);
                return;
            }


            if (departureDate == null) {
                JOptionPane.showMessageDialog(this, "Please select a departure date for destination " + (i + 1) + ".", "Date Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (i == 0) {

                if (departureDate.isBefore(LocalDate.now())) {
                    JOptionPane.showMessageDialog(this, "Departure date for first destination must be today or in the future.", "Invalid Date", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } else {

                if (!departureDate.isAfter(previousDeparture)) {
                    JOptionPane.showMessageDialog(this, "Departure date for destination " + (i + 1) + " must be after previous departure.", "Invalid Date Sequence", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }



            LocalDate arrivalDate = departureDate;
            LocalDate departureFromCity = departureDate.plusDays(1); // Default: stay 1 day
            
            cityLegs.add(new MultiCityTripRequest.CityLeg(destCode, arrivalDate, departureFromCity));
            previousDeparture = departureDate; // Next departure should be after this departure
        }

        boolean returnToOrigin = false;
        LocalDate returnDate = null;

        int adults = multiCityTravellers.getAdults();
        int children = multiCityTravellers.getChildren();
        int infants = multiCityTravellers.getInfants();

        MultiCityTripRequest request = new MultiCityTripRequest(
                originCode,
                cityLegs,
                returnToOrigin,
                returnDate,
                adults,
                children,
                infants
        );

        new MultiCityFlowPage(request, this);
    }

    private String extractIataCode(String displayValue) {
        if (displayValue == null || displayValue.isEmpty()) return null;
        int start = displayValue.lastIndexOf("(");
        int end = displayValue.lastIndexOf(")");
        if (start >= 0 && end > start) {
            return displayValue.substring(start + 1, end).trim().toUpperCase();
        }
        if (displayValue.length() == 3) {
            return displayValue.toUpperCase();
        }
        return null;
    }


    private JPanel createCityCardsPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 20, 15, 20)); // No top padding - no space after discover

        List<City> cities = cityService.getCities();
        int cityCount = cities.size();
        int cols = 5; // 5 columns for balanced layout
        int rows = (int) Math.ceil((double) cityCount / cols);

        
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(rows, cols, 15, 15)); // Balanced gaps for beautiful spacing
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        for (City city : cities) {
            JPanel card = createCityCard(city);
            panel.add(card);
        }
        
        wrapper.add(panel, BorderLayout.CENTER);

        JPanel borderPanel = new JPanel(new BorderLayout());
        borderPanel.setOpaque(false);
        borderPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(3, 0, 3, 0, new Color(200, 210, 230)),
            new EmptyBorder(20, 0, 20, 0)
        ));
        borderPanel.add(wrapper, BorderLayout.CENTER);

        JPanel leftDecor = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(220, 230, 240, 150));
                g2d.setStroke(new BasicStroke(2));
                int centerY = getHeight() / 2;
                g2d.drawLine(0, centerY - 30, 0, centerY + 30);
            }
        };
        leftDecor.setPreferredSize(new Dimension(5, 0));
        leftDecor.setOpaque(false);
        
        JPanel rightDecor = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(220, 230, 240, 150));
                g2d.setStroke(new BasicStroke(2));
                int centerY = getHeight() / 2;
                g2d.drawLine(getWidth() - 1, centerY - 30, getWidth() - 1, centerY + 30);
            }
        };
        rightDecor.setPreferredSize(new Dimension(5, 0));
        rightDecor.setOpaque(false);
        
        JPanel decoratedWrapper = new JPanel(new BorderLayout());
        decoratedWrapper.setOpaque(false);
        decoratedWrapper.add(leftDecor, BorderLayout.WEST);
        decoratedWrapper.add(borderPanel, BorderLayout.CENTER);
        decoratedWrapper.add(rightDecor, BorderLayout.EAST);
        
        return decoratedWrapper;
    }
    
    private JPanel createCityCard(City city) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(new Color(255, 255, 255, 250));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(8, 8, 8, 8)
        ));

        card.setPreferredSize(new Dimension(220, 200));

        JLabel nameLabel = new JLabel("<html><div style='text-align:center; line-height:1.4;'>" + city.getName() + "</div></html>", SwingConstants.CENTER);
        nameLabel.setFont(new Font("Georgia", Font.BOLD, 18)); // Bigger size, elegant font
        nameLabel.setForeground(new Color(50, 60, 80));
        nameLabel.setBorder(new EmptyBorder(8, 5, 8, 5)); // Better spacing around text

        ImageIcon icon = new ImageIcon("resources/images/" + city.getImagePath());

        JLabel imageLabel = new JLabel(new ImageIcon(icon.getImage().getScaledInstance(200, 150, Image.SCALE_SMOOTH)));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(imageLabel, BorderLayout.CENTER);
        card.add(nameLabel, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(100, 150, 220), 2),
                    new EmptyBorder(8, 8, 8, 8)
                ));
                card.setBackground(new Color(250, 250, 255));
                card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                    new EmptyBorder(8, 8, 8, 8)
                ));
                card.setBackground(new Color(255, 255, 255, 250));
                card.setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                new CityDetailPage(city);
            }
        });

        return card;
    }

    private class ThemedBackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int width = getWidth();
            int height = getHeight();

            Color color1 = new Color(240, 245, 255); // Calm - soft blue-white
            Color color2 = new Color(255, 240, 245); // Beauty - soft lavender-pink
            Color color3 = new Color(255, 250, 240); // Adventure - warm cream

            float progress = gradientProgress;
            Color startColor = interpolateColor(color1, color2, progress);
            Color endColor = interpolateColor(color2, color3, progress);
            
            GradientPaint gradient = new GradientPaint(
                0, 0, startColor,
                width, height, endColor
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, width, height);

            RadialGradientPaint radialOverlay = new RadialGradientPaint(
                width * 0.3f, height * 0.2f, width * 0.8f,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{
                    new Color(255, 255, 255, 30),
                    new Color(255, 255, 255, 10),
                    new Color(255, 255, 255, 0)
                }
            );
            g2d.setPaint(radialOverlay);
            g2d.fillRect(0, 0, width, height);

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
            g2d.setColor(new Color(100, 150, 220));
            for (int i = 0; i < 8; i++) {
                float x = width * (0.1f + i * 0.12f);
                float y = height * (0.3f + (i % 3) * 0.25f);
                int size = 150 + (i % 3) * 50;
                g2d.fillOval((int)(x - size/2), (int)(y - size/2), size, size);
            }
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
        
        private Color interpolateColor(Color c1, Color c2, float t) {
            int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * t);
            int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
            int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t);
            return new Color(r, g, b);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HomePage::new);
    }
}
