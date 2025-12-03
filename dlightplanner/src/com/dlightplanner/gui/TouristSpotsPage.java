package com.dlightplanner.gui;

import com.dlightplanner.models.Hotel;
import com.dlightplanner.models.TouristSpot;
import com.dlightplanner.services.AirportService;
import com.dlightplanner.services.TouristSpotService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TouristSpotsPage extends JFrame {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    
    private final TouristSpotService touristSpotService;
    private final AirportService airportService;
    private final String destinationCode;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final Hotel selectedHotel;
    
    private JPanel spotsListPanel;
    private List<TouristSpot> allSpots;
    private Set<Integer> selectedSpotIds = new HashSet<>();
    private JButton continueButton;
    private JFrame previousFrame;

    private String categoryFilter = "All";

    private static final Map<String, String> CATEGORY_ICONS = new HashMap<>();
    static {
        CATEGORY_ICONS.put("Historical", "🏛️");
        CATEGORY_ICONS.put("Nature", "🌳");
        CATEGORY_ICONS.put("Adventure", "⛰️");
        CATEGORY_ICONS.put("Cultural", "🎭");
        CATEGORY_ICONS.put("Religious", "🕉️");
        CATEGORY_ICONS.put("Shopping", "🛍️");
        CATEGORY_ICONS.put("Entertainment", "🎪");
        CATEGORY_ICONS.put("Food", "🍽️");
    }

    public TouristSpotsPage(String destinationCode, LocalDate checkInDate, LocalDate checkOutDate, Hotel selectedHotel) {
        this(destinationCode, checkInDate, checkOutDate, selectedHotel, null);
    }
    
    public TouristSpotsPage(String destinationCode, LocalDate checkInDate, LocalDate checkOutDate, Hotel selectedHotel, JFrame previousFrame) {
        this.destinationCode = destinationCode;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.selectedHotel = selectedHotel;
        this.previousFrame = previousFrame;

        if (previousFrame != null) {
            previousFrame.setVisible(false);
        }
        
        touristSpotService = new TouristSpotService();
        touristSpotService.loadTouristSpotsFromJson("resources/tourist_spots.json");
        
        airportService = new AirportService();
        airportService.loadAirportsFromJson("resources/airports.json");
        
        String cityName = getCityNameFromCode(destinationCode);
        allSpots = touristSpotService.getTouristSpotsByDestination(destinationCode);
        
        setTitle("Voya | Select Tourist Spots");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true); // Allow resizing/minimizing
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Default to maximized
        setLayout(new BorderLayout());

        addWindowStateListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowStateChanged(java.awt.event.WindowEvent e) {
                if ((e.getNewState() & JFrame.ICONIFIED) != 0) {
                    setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            }
        });
        
        add(buildHeaderPanel(cityName), BorderLayout.NORTH);
        add(buildContentPanel(), BorderLayout.CENTER);
        add(buildFooterPanel(), BorderLayout.SOUTH);
        
        renderSpots(allSpots);
        setVisible(true);
    }
    
    private String getCityNameFromCode(String code) {
        return airportService.getAirports().stream()
                .filter(airport -> airport.getIata().equalsIgnoreCase(code))
                .map(airport -> airport.getCity())
                .findFirst()
                .orElse(code);
    }
    
    private JPanel buildHeaderPanel(String cityName) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(10, 32, 66));
        header.setBorder(new EmptyBorder(20, 25, 20, 25));
        
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        
        JLabel title = new JLabel("Select Tourist Spots (Optional)");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Serif", Font.BOLD, 26));
        
        JButton filterButton = new JButton("🔍 Filter by Category");
        filterButton.setBackground(new Color(255, 255, 255));
        filterButton.setForeground(new Color(10, 32, 66));
        filterButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        filterButton.addActionListener(e -> showFilterDialog());
        
        topRow.add(title, BorderLayout.WEST);
        topRow.add(filterButton, BorderLayout.EAST);
        
        JLabel sub = new JLabel(String.format("%s • %d spot%s available • Select spots you'd like to visit",
                cityName, allSpots.size(), allSpots.size() == 1 ? "" : "s"));
        sub.setForeground(new Color(210, 225, 240));
        sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        header.add(topRow, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        return header;
    }
    
    private void showFilterDialog() {
        JDialog filterDialog = new JDialog(this, "Filter by Category", true);
        filterDialog.setSize(300, 400);
        filterDialog.setLocationRelativeTo(this);
        filterDialog.setLayout(new BorderLayout());
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        content.add(new JLabel("Select Category:"));
        content.add(Box.createVerticalStrut(15));
        
        Set<String> categories = allSpots.stream()
                .map(TouristSpot::getCategory)
                .collect(Collectors.toSet());
        
        List<String> categoryList = new ArrayList<>(categories);
        categoryList.sort(String::compareTo);
        categoryList.add(0, "All");
        
        JComboBox<String> categoryCombo = new JComboBox<>(categoryList.toArray(new String[0]));
        categoryCombo.setSelectedItem(categoryFilter);
        content.add(categoryCombo);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton applyButton = new JButton("Apply");
        JButton clearButton = new JButton("Clear");
        
        applyButton.addActionListener(e -> {
            categoryFilter = (String) categoryCombo.getSelectedItem();
            applyFilters();
            filterDialog.dispose();
        });
        
        clearButton.addActionListener(e -> {
            categoryFilter = "All";
            applyFilters();
            filterDialog.dispose();
        });
        
        buttonPanel.add(applyButton);
        buttonPanel.add(clearButton);
        
        filterDialog.add(content, BorderLayout.CENTER);
        filterDialog.add(buttonPanel, BorderLayout.SOUTH);
        filterDialog.setVisible(true);
    }
    
    private void applyFilters() {
        List<TouristSpot> filtered = new ArrayList<>(allSpots);
        
        if (!categoryFilter.equals("All")) {
            filtered = filtered.stream()
                    .filter(spot -> spot.getCategory().equals(categoryFilter))
                    .collect(Collectors.toList());
        }
        
        renderSpots(filtered);
    }
    
    private JPanel buildContentPanel() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(new EmptyBorder(15, 25, 10, 25));
        
        spotsListPanel = new JPanel();
        spotsListPanel.setLayout(new BoxLayout(spotsListPanel, BoxLayout.Y_AXIS));
        spotsListPanel.setOpaque(false);
        
        JScrollPane scrollPane = new JScrollPane(spotsListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        center.add(scrollPane, BorderLayout.CENTER);
        return center;
    }
    
    private JPanel buildFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBorder(new EmptyBorder(10, 25, 15, 25));
        
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> {
            if (previousFrame != null && previousFrame.isDisplayable()) {

                previousFrame.setExtendedState(JFrame.NORMAL);
                previousFrame.setVisible(true);
                previousFrame.toFront();
                previousFrame.requestFocus();
            }
            dispose();
        });
        
        JButton skipButton = new JButton("Skip");
        skipButton.addActionListener(e -> proceedToItinerary());
        
        continueButton = new JButton("Continue to Itinerary");
        continueButton.addActionListener(e -> proceedToItinerary());
        continueButton.setBackground(new Color(34, 139, 34));
        continueButton.setForeground(Color.WHITE);
        continueButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        
        footer.add(backButton);
        footer.add(skipButton);
        footer.add(continueButton);
        
        return footer;
    }
    
    private void renderSpots(List<TouristSpot> spots) {
        spotsListPanel.removeAll();
        
        if (spots.isEmpty()) {
            JLabel noSpots = new JLabel("No tourist spots found matching your criteria.");
            noSpots.setFont(new Font("SansSerif", Font.PLAIN, 14));
            noSpots.setBorder(new EmptyBorder(20, 0, 20, 0));
            spotsListPanel.add(noSpots);
        } else {
            for (TouristSpot spot : spots) {
                spotsListPanel.add(createSpotCard(spot));
                spotsListPanel.add(Box.createVerticalStrut(15));
            }
        }
        
        spotsListPanel.revalidate();
        spotsListPanel.repaint();
    }
    
    private JPanel createSpotCard(TouristSpot spot) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(15, 15, 15, 15)));

        JLabel imageLabel = new JLabel();
        try {
            ImageIcon icon = new ImageIcon("resources/images/" + spot.getImagePath());
            if (icon.getIconWidth() > 0) {
                Image scaled = icon.getImage().getScaledInstance(200, 150, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaled));
            } else {
                imageLabel.setText("No Image");
                imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            }
        } catch (Exception e) {
            imageLabel.setText("No Image");
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        }
        imageLabel.setPreferredSize(new Dimension(200, 150));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(spot.getName());
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        
        String icon = CATEGORY_ICONS.getOrDefault(spot.getCategory(), "📍");
        JLabel categoryLabel = new JLabel(icon + " " + spot.getCategory());
        categoryLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        categoryLabel.setForeground(new Color(100, 100, 100));
        
        JLabel locationLabel = new JLabel("📍 " + spot.getLocation());
        locationLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        locationLabel.setForeground(new Color(120, 120, 120));
        
        JLabel distanceLabel = new JLabel(String.format("📍 %.1f km from city center", spot.getDistanceFromCityCenter()));
        distanceLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        distanceLabel.setForeground(new Color(120, 120, 120));
        
        JLabel descLabel = new JLabel("<html><p style='width:500px'>" + spot.getDescription() + "</p></html>");
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        
        String durationText = spot.getEstimatedDuration() % 1 == 0 
            ? String.format("%.0f", spot.getEstimatedDuration())
            : String.format("%.1f", spot.getEstimatedDuration());
        JLabel durationLabel = new JLabel("⏱️ Estimated Duration: " + durationText + " hours");
        durationLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        durationLabel.setForeground(new Color(100, 100, 100));
        
        detailsPanel.add(nameLabel);
        detailsPanel.add(Box.createVerticalStrut(5));
        detailsPanel.add(categoryLabel);
        detailsPanel.add(locationLabel);
        detailsPanel.add(distanceLabel);
        detailsPanel.add(Box.createVerticalStrut(8));
        detailsPanel.add(descLabel);
        detailsPanel.add(Box.createVerticalStrut(5));
        detailsPanel.add(durationLabel);
        detailsPanel.add(Box.createVerticalGlue());

        JPanel pricePanel = new JPanel();
        pricePanel.setLayout(new BoxLayout(pricePanel, BoxLayout.Y_AXIS));
        pricePanel.setOpaque(false);
        pricePanel.setPreferredSize(new Dimension(180, 0));
        
        String priceText = spot.getPrice() == 0 ? "Free Entry" : String.format("₹%,.0f per person", spot.getPrice());
        JLabel priceLabel = new JLabel(priceText);
        priceLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        priceLabel.setForeground(new Color(34, 139, 34));
        
        JCheckBox selectCheckbox = new JCheckBox("Select");
        selectCheckbox.setSelected(selectedSpotIds.contains(spot.getId()));
        selectCheckbox.addActionListener(e -> {
            if (selectCheckbox.isSelected()) {
                selectedSpotIds.add(spot.getId());
            } else {
                selectedSpotIds.remove(spot.getId());
            }
            updateContinueButton();
        });
        
        pricePanel.add(Box.createVerticalGlue());
        pricePanel.add(priceLabel);
        pricePanel.add(Box.createVerticalStrut(10));
        pricePanel.add(selectCheckbox);
        pricePanel.add(Box.createVerticalGlue());
        
        card.add(imageLabel, BorderLayout.WEST);
        card.add(detailsPanel, BorderLayout.CENTER);
        card.add(pricePanel, BorderLayout.EAST);
        
        return card;
    }
    
    private void updateContinueButton() {
        if (continueButton != null) {
            continueButton.setEnabled(true);
        }
    }
    
    private void proceedToItinerary() {
        List<TouristSpot> selectedSpots = allSpots.stream()
                .filter(spot -> selectedSpotIds.contains(spot.getId()))
                .collect(Collectors.toList());
        
        dispose();

        if (previousFrame instanceof MultiCityFlowPage) {
            MultiCityFlowPage multiCityPage = (MultiCityFlowPage) previousFrame;
            multiCityPage.onTouristSpotsSelected(destinationCode, selectedSpots);
            return;
        }

        new SmartItineraryPage(destinationCode, checkInDate, checkOutDate, selectedHotel, selectedSpots, this);
    }

    public JFrame getPreviousFrame() {
        return previousFrame;
    }
}

