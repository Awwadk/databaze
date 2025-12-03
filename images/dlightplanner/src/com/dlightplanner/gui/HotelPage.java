package com.dlightplanner.gui;

import com.dlightplanner.models.Hotel;
import com.dlightplanner.services.AirportService;
import com.dlightplanner.services.HotelService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class HotelPage extends JFrame {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    
    private final HotelService hotelService;
    private final AirportService airportService;
    private final String destinationCode;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final int numberOfNights;
    
    private JPanel hotelListPanel;
    private JPanel breakdownPanel;
    private JPanel contentPanel;
    private JSplitPane splitPane;
    private List<Hotel> allHotels;
    private List<Hotel> filteredHotels;
    private Hotel selectedHotel;
    private Map<Integer, Integer> hotelRooms = new HashMap<>(); // hotel ID -> number of rooms
    private Map<Integer, Integer> hotelBeds = new HashMap<>(); // hotel ID -> number of beds
    private JButton confirmButton;
    private JSpinner nightsSpinner;
    private JFrame previousFrame;
    private boolean breakdownShown = false;

    private Integer starFilter = null;
    private Double minPrice = null;
    private Double maxPrice = null;
    private Double maxDistance = null;
    private Set<String> selectedAmenities = new HashSet<>();

    private static final Map<String, String> AMENITY_ICONS = new HashMap<>();
    static {
        AMENITY_ICONS.put("WiFi", "📶");
        AMENITY_ICONS.put("Pool", "🏊");
        AMENITY_ICONS.put("Spa", "💆");
        AMENITY_ICONS.put("Gym", "💪");
        AMENITY_ICONS.put("Restaurant", "🍽️");
        AMENITY_ICONS.put("Bar", "🍷");
        AMENITY_ICONS.put("Parking", "🅿️");
        AMENITY_ICONS.put("Room Service", "🛎️");
        AMENITY_ICONS.put("Beach Access", "🏖️");
        AMENITY_ICONS.put("Water Sports", "🏄");
        AMENITY_ICONS.put("Business Center", "💼");
        AMENITY_ICONS.put("Fine Dining", "🍴");
        AMENITY_ICONS.put("Mountain View", "⛰️");
        AMENITY_ICONS.put("Himalayan View", "🏔️");
        AMENITY_ICONS.put("Sea View", "🌊");
        AMENITY_ICONS.put("Lake View", "🏞️");
        AMENITY_ICONS.put("River View", "🌊");
        AMENITY_ICONS.put("Heritage Tours", "🏛️");
        AMENITY_ICONS.put("Trekking Tours", "🥾");
        AMENITY_ICONS.put("Yoga", "🧘");
        AMENITY_ICONS.put("Yoga Classes", "🧘");
        AMENITY_ICONS.put("Tea Tours", "🍵");
        AMENITY_ICONS.put("Scuba Diving", "🤿");
        AMENITY_ICONS.put("Snorkeling", "🤿");
        AMENITY_ICONS.put("Cultural Shows", "🎭");
        AMENITY_ICONS.put("Boat Rides", "⛵");
        AMENITY_ICONS.put("Boat Tours", "⛵");
        AMENITY_ICONS.put("Bonfire", "🔥");
        AMENITY_ICONS.put("Ayurveda Spa", "🧘");
        AMENITY_ICONS.put("Spiritual Tours", "🕉️");
        AMENITY_ICONS.put("Temple Tours", "🛕");
        AMENITY_ICONS.put("Taj View", "🕌");
    }

    private LocalDate currentCheckOutDate;
    
    public HotelPage(String destinationCode, LocalDate checkInDate, LocalDate checkOutDate) {
        this(destinationCode, checkInDate, checkOutDate, null);
    }
    
    public HotelPage(String destinationCode, LocalDate checkInDate, LocalDate checkOutDate, JFrame previousFrame) {
        this.destinationCode = destinationCode;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate != null ? checkOutDate : checkInDate.plusDays(1);
        this.currentCheckOutDate = this.checkOutDate;
        this.numberOfNights = (int) ChronoUnit.DAYS.between(checkInDate, this.currentCheckOutDate);
        this.previousFrame = previousFrame;

        if (previousFrame != null) {
            previousFrame.setVisible(false);
        }
        
        hotelService = new HotelService();
        hotelService.loadHotelsFromJson("resources/hotels.json");
        
        airportService = new AirportService();
        airportService.loadAirportsFromJson("resources/airports.json");

        hotelService.setAirportService(airportService);
        
        String cityName = getCityNameFromCode(destinationCode);
        allHotels = hotelService.getHotelsByDestination(destinationCode, checkInDate, checkOutDate);
        filteredHotels = new ArrayList<>(allHotels);
        
        setTitle("Voya | Select Hotel");
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
        
        renderHotels(filteredHotels);
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
        
        JLabel title = new JLabel("Select Your Hotel");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Serif", Font.BOLD, 26));
        
        JButton filterButton = new JButton("🔍 Filters");
        filterButton.setBackground(new Color(255, 255, 255));
        filterButton.setForeground(new Color(10, 32, 66));
        filterButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        filterButton.addActionListener(e -> showFilterDialog());
        
        topRow.add(title, BorderLayout.WEST);
        topRow.add(filterButton, BorderLayout.EAST);
        
        JPanel subPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        subPanel.setOpaque(false);
        
        JLabel sub = new JLabel(String.format("%s • Check-in: %s • Check-out: %s",
                cityName, checkInDate.format(DATE_FORMAT), currentCheckOutDate.format(DATE_FORMAT)));
        sub.setForeground(new Color(210, 225, 240));
        sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JLabel nightsLabel = new JLabel("Nights:");
        nightsLabel.setForeground(new Color(210, 225, 240));
        nightsLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        SpinnerNumberModel nightsModel = new SpinnerNumberModel(numberOfNights, 1, 30, 1);
        nightsSpinner = new JSpinner(nightsModel);
        nightsSpinner.setPreferredSize(new Dimension(60, 25));
        nightsSpinner.addChangeListener(e -> {
            int newNights = (Integer) nightsSpinner.getValue();
            currentCheckOutDate = checkInDate.plusDays(newNights);
            sub.setText(String.format("%s • Check-in: %s • Check-out: %s",
                    cityName, checkInDate.format(DATE_FORMAT), currentCheckOutDate.format(DATE_FORMAT)));
            renderHotels(filteredHotels);

            if (breakdownShown && selectedHotel != null) {
                showBreakdownPanel();
            }
        });
        
        subPanel.add(sub);
        subPanel.add(Box.createHorizontalStrut(15));
        subPanel.add(nightsLabel);
        subPanel.add(nightsSpinner);
        
        header.add(topRow, BorderLayout.NORTH);
        header.add(subPanel, BorderLayout.SOUTH);
        return header;
    }
    
    private void showFilterDialog() {
        JDialog filterDialog = new JDialog(this, "Filter Hotels", true);
        filterDialog.setSize(400, 500);
        filterDialog.setLocationRelativeTo(this);
        filterDialog.setLayout(new BorderLayout());
        
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        content.add(new JLabel("Star Rating:"));
        JComboBox<String> starCombo = new JComboBox<>(new String[]{"All", "3 Stars", "4 Stars", "5 Stars"});
        if (starFilter != null) {
            starCombo.setSelectedIndex(starFilter - 2);
        }
        content.add(starCombo);
        content.add(Box.createVerticalStrut(15));

        content.add(new JLabel("Price Range (per night):"));
        JPanel pricePanel = new JPanel(new FlowLayout());
        JTextField minPriceField = new JTextField(8);
        JTextField maxPriceField = new JTextField(8);
        if (minPrice != null) minPriceField.setText(String.valueOf(minPrice.intValue()));
        if (maxPrice != null) maxPriceField.setText(String.valueOf(maxPrice.intValue()));
        pricePanel.add(new JLabel("Min: ₹"));
        pricePanel.add(minPriceField);
        pricePanel.add(new JLabel("Max: ₹"));
        pricePanel.add(maxPriceField);
        content.add(pricePanel);
        content.add(Box.createVerticalStrut(15));

        content.add(new JLabel("Max Distance from City Center (km):"));
        JTextField distanceField = new JTextField(10);
        if (maxDistance != null) distanceField.setText(String.valueOf(maxDistance));
        content.add(distanceField);
        content.add(Box.createVerticalStrut(15));

        content.add(new JLabel("Amenities:"));
        JPanel amenitiesPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        Set<String> allAmenities = allHotels.stream()
                .flatMap(h -> h.getAmenities().stream())
                .collect(Collectors.toSet());
        List<JCheckBox> amenityCheckboxes = new ArrayList<>();
        for (String amenity : allAmenities) {
            JCheckBox cb = new JCheckBox(amenity);
            if (selectedAmenities.contains(amenity)) cb.setSelected(true);
            amenityCheckboxes.add(cb);
            amenitiesPanel.add(cb);
        }
        JScrollPane amenitiesScroll = new JScrollPane(amenitiesPanel);
        amenitiesScroll.setPreferredSize(new Dimension(350, 150));
        content.add(amenitiesScroll);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton applyButton = new JButton("Apply Filters");
        JButton clearButton = new JButton("Clear All");
        
        applyButton.addActionListener(e -> {

            int starIndex = starCombo.getSelectedIndex();
            starFilter = starIndex == 0 ? null : starIndex + 2;

            try {
                minPrice = minPriceField.getText().isEmpty() ? null : Double.parseDouble(minPriceField.getText());
                maxPrice = maxPriceField.getText().isEmpty() ? null : Double.parseDouble(maxPriceField.getText());
            } catch (NumberFormatException ex) {
                minPrice = null;
                maxPrice = null;
            }

            try {
                maxDistance = distanceField.getText().isEmpty() ? null : Double.parseDouble(distanceField.getText());
            } catch (NumberFormatException ex) {
                maxDistance = null;
            }

            selectedAmenities.clear();
            for (JCheckBox cb : amenityCheckboxes) {
                if (cb.isSelected()) {
                    selectedAmenities.add(cb.getText());
                }
            }
            
            applyFilters();
            filterDialog.dispose();
        });
        
        clearButton.addActionListener(e -> {
            starFilter = null;
            minPrice = null;
            maxPrice = null;
            maxDistance = null;
            selectedAmenities.clear();
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
        filteredHotels = new ArrayList<>(allHotels);
        
        if (starFilter != null) {
            filteredHotels.removeIf(h -> h.getStarRating() != starFilter);
        }
        
        if (minPrice != null) {
            filteredHotels.removeIf(h -> h.getPricePerNight() < minPrice);
        }
        if (maxPrice != null) {
            filteredHotels.removeIf(h -> h.getPricePerNight() > maxPrice);
        }
        
        if (maxDistance != null) {
            filteredHotels.removeIf(h -> h.getDistanceFromCityCenter() > maxDistance);
        }
        
        if (!selectedAmenities.isEmpty()) {
            filteredHotels.removeIf(h -> {
                Set<String> hotelAmenities = new HashSet<>(h.getAmenities());
                return !hotelAmenities.containsAll(selectedAmenities);
            });
        }
        
        renderHotels(filteredHotels);
    }
    
    private JPanel buildContentPanel() {
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(15, 25, 10, 25));
        
        hotelListPanel = new JPanel();
        hotelListPanel.setLayout(new BoxLayout(hotelListPanel, BoxLayout.Y_AXIS));
        hotelListPanel.setOpaque(false);
        
        JScrollPane scrollPane = new JScrollPane(hotelListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        return contentPanel;
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
        
        confirmButton = new JButton("Confirm Hotel");
        confirmButton.addActionListener(e -> confirmHotelSelection());
        confirmButton.setEnabled(false);
        
        footer.add(backButton);
        footer.add(confirmButton);
        
        return footer;
    }
    
    private void renderHotels(List<Hotel> hotels) {
        hotelListPanel.removeAll();
        
        if (hotels.isEmpty()) {
            JLabel noHotels = new JLabel("No hotels found matching your criteria.");
            noHotels.setFont(new Font("SansSerif", Font.PLAIN, 14));
            noHotels.setBorder(new EmptyBorder(20, 0, 20, 0));
            hotelListPanel.add(noHotels);
        } else {
            for (Hotel hotel : hotels) {
                hotelListPanel.add(createHotelCard(hotel));
                hotelListPanel.add(Box.createVerticalStrut(15));
            }
        }
        
        hotelListPanel.revalidate();
        hotelListPanel.repaint();
    }
    
    private JPanel createHotelCard(Hotel hotel) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(15, 15, 15, 15)));

        JLabel imageLabel = new JLabel();
        ImageIcon icon = loadHotelImage(hotel);
        if (icon != null && icon.getIconWidth() > 0) {
            Image scaled = icon.getImage().getScaledInstance(250, 180, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaled));
        } else {
            imageLabel.setText("No Image");
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        }
        imageLabel.setPreferredSize(new Dimension(250, 180));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(hotel.getName());
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < hotel.getStarRating(); i++) {
            stars.append("★");
        }
        JLabel starLabel = new JLabel(stars.toString());
        starLabel.setForeground(new Color(255, 215, 0));
        starLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JLabel locationLabel = new JLabel("📍 " + hotel.getLocation());
        locationLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        locationLabel.setForeground(new Color(100, 100, 100));

        JLabel distanceLabel = new JLabel(String.format("📍 %.1f km from city center", hotel.getDistanceFromCityCenter()));
        distanceLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        distanceLabel.setForeground(new Color(120, 120, 120));

        String description = generateDescription(hotel);
        JLabel descLabel = new JLabel("<html><p style='width:450px'>" + description + "</p></html>");
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JPanel amenitiesPanel = createAmenitiesPanel(hotel.getAmenities());
        
        detailsPanel.add(nameLabel);
        detailsPanel.add(Box.createVerticalStrut(5));
        detailsPanel.add(starLabel);
        detailsPanel.add(Box.createVerticalStrut(5));
        detailsPanel.add(locationLabel);
        detailsPanel.add(distanceLabel);
        detailsPanel.add(Box.createVerticalStrut(8));
        detailsPanel.add(descLabel);
        detailsPanel.add(Box.createVerticalStrut(8));
        detailsPanel.add(amenitiesPanel);
        detailsPanel.add(Box.createVerticalGlue());

        JPanel pricePanel = new JPanel();
        pricePanel.setLayout(new BoxLayout(pricePanel, BoxLayout.Y_AXIS));
        pricePanel.setOpaque(false);
        pricePanel.setPreferredSize(new Dimension(200, 0));

        int hotelRoomsCount = hotelRooms.getOrDefault(hotel.getId(), 1);
        int hotelBedsCount = hotelBeds.getOrDefault(hotel.getId(), 2);
        
        double pricePerNight = hotel.getPriceForBeds(hotelBedsCount);
        int currentNights = (Integer) (nightsSpinner != null ? nightsSpinner.getValue() : numberOfNights);
        double totalPrice = pricePerNight * currentNights * hotelRoomsCount;
        
        JLabel perNightLabel = new JLabel(String.format("₹%,.0f/night", pricePerNight));
        perNightLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        perNightLabel.setForeground(new Color(100, 100, 100));
        
        JLabel totalLabel = new JLabel(String.format("₹%,.0f total", totalPrice));
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        totalLabel.setForeground(new Color(34, 139, 34));
        
        JLabel bedsLabel = new JLabel(String.format("(%d beds)", hotelBedsCount));
        bedsLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        bedsLabel.setForeground(new Color(120, 120, 120));

        JPanel roomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        roomPanel.setOpaque(false);
        roomPanel.add(new JLabel("Rooms:"));
        SpinnerNumberModel roomModel = new SpinnerNumberModel(hotelRoomsCount, 1, 10, 1);
        JSpinner roomSpinner = new JSpinner(roomModel);
        roomSpinner.setPreferredSize(new Dimension(60, 25));
        roomSpinner.addChangeListener(e -> {
            hotelRooms.put(hotel.getId(), (Integer) roomSpinner.getValue());

            updateHotelCardPrice(card, hotel, hotelRooms.get(hotel.getId()), hotelBeds.getOrDefault(hotel.getId(), 2));
        });
        roomPanel.add(roomSpinner);

        JPanel bedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bedPanel.setOpaque(false);
        bedPanel.add(new JLabel("Beds:"));
        JComboBox<Integer> bedCombo = new JComboBox<>(new Integer[]{2, 3, 4});
        bedCombo.setSelectedItem(hotelBedsCount);
        bedCombo.addActionListener(e -> {
            hotelBeds.put(hotel.getId(), (Integer) bedCombo.getSelectedItem());

            updateHotelCardPrice(card, hotel, hotelRooms.getOrDefault(hotel.getId(), 1), hotelBeds.get(hotel.getId()));
        });
        bedPanel.add(bedCombo);
        
        boolean isSelected = selectedHotel != null && selectedHotel.getId() == hotel.getId();
        JButton selectButton = new JButton(isSelected ? "Selected" : "Select Hotel");
        selectButton.setEnabled(!isSelected);
        selectButton.addActionListener(e -> selectHotel(hotel));
        
        JPanel buttonPanel = new JPanel(new GridLayout(1, isSelected ? 2 : 1, 5, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(selectButton);
        
        if (isSelected) {
            JButton cancelButton = new JButton("Cancel");
            cancelButton.setFont(new Font("SansSerif", Font.PLAIN, 11));
            cancelButton.addActionListener(e -> cancelHotelSelection());
            buttonPanel.add(cancelButton);
        }
        
        pricePanel.add(Box.createVerticalGlue());
        pricePanel.add(perNightLabel);
        pricePanel.add(totalLabel);
        pricePanel.add(bedsLabel);
        pricePanel.add(Box.createVerticalStrut(5));
        pricePanel.add(roomPanel);
        pricePanel.add(bedPanel);
        pricePanel.add(Box.createVerticalStrut(10));
        pricePanel.add(buttonPanel);
        pricePanel.add(Box.createVerticalGlue());

        card.putClientProperty("roomSpinner", roomSpinner);
        card.putClientProperty("bedCombo", bedCombo);
        card.putClientProperty("totalLabel", totalLabel);
        card.putClientProperty("perNightLabel", perNightLabel);
        card.putClientProperty("bedsLabel", bedsLabel);
        card.putClientProperty("buttonPanel", buttonPanel);
        card.putClientProperty("hotelId", hotel.getId());
        
        card.add(imageLabel, BorderLayout.WEST);
        card.add(detailsPanel, BorderLayout.CENTER);
        card.add(pricePanel, BorderLayout.EAST);
        
        return card;
    }
    
    private JPanel createAmenitiesPanel(List<String> amenities) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 5));
        panel.setOpaque(false);
        
        int count = 0;
        for (String amenity : amenities) {
            if (count >= 6) break; // Show max 6 amenities (3 per column)
            String icon = AMENITY_ICONS.getOrDefault(amenity, "✓");
            JLabel amenityLabel = new JLabel(icon + " " + amenity);
            amenityLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
            amenityLabel.setForeground(new Color(80, 80, 80));
            panel.add(amenityLabel);
            count++;
        }
        
        return panel;
    }
    
    private String generateDescription(Hotel hotel) {
        String cityName = getCityNameFromCode(destinationCode);
        StringBuilder desc = new StringBuilder();
        
        desc.append("Experience ");
        if (hotel.getStarRating() == 5) {
            desc.append("luxurious ");
        } else if (hotel.getStarRating() == 4) {
            desc.append("comfortable ");
        } else {
            desc.append("affordable ");
        }
        
        desc.append("accommodation in ").append(cityName);
        
        if (hotel.getDistanceFromCityCenter() <= 2.0) {
            desc.append(", perfectly located in the heart of the city");
        } else if (hotel.getDistanceFromCityCenter() <= 5.0) {
            desc.append(", conveniently close to city center");
        } else {
            desc.append(", offering a peaceful retreat");
        }
        
        desc.append(". This ").append(hotel.getStarRating()).append("-star property");
        
        if (hotel.getAmenities().contains("Pool")) {
            desc.append(" features a swimming pool");
        }
        if (hotel.getAmenities().contains("Spa")) {
            desc.append(" and spa facilities");
        }
        if (hotel.getAmenities().contains("Beach Access")) {
            desc.append(" with direct beach access");
        }
        if (hotel.getAmenities().contains("Mountain View") || hotel.getAmenities().contains("Himalayan View")) {
            desc.append(" with stunning mountain views");
        }
        
        desc.append(". Ideal for both leisure and business travelers.");
        
        return desc.toString();
    }
    
    private void selectHotel(Hotel hotel) {
        selectedHotel = hotel;

        if (!hotelRooms.containsKey(hotel.getId())) {
            hotelRooms.put(hotel.getId(), 1);
        }
        if (!hotelBeds.containsKey(hotel.getId())) {
            hotelBeds.put(hotel.getId(), 2);
        }
        renderHotels(filteredHotels);
        if (confirmButton != null) {
            confirmButton.setEnabled(true);
        }
    }
    
    private void cancelHotelSelection() {
        selectedHotel = null;
        renderHotels(filteredHotels);
        if (confirmButton != null) {
            confirmButton.setEnabled(false);
        }
    }
    
    private void updateHotelCardPrice(JPanel card, Hotel hotel, int rooms, int beds) {
        double pricePerNight = hotel.getPriceForBeds(beds);
        int currentNights = (Integer) (nightsSpinner != null ? nightsSpinner.getValue() : numberOfNights);
        double totalPrice = pricePerNight * currentNights * rooms;
        
        JLabel totalLabel = (JLabel) card.getClientProperty("totalLabel");
        JLabel perNightLabel = (JLabel) card.getClientProperty("perNightLabel");
        JLabel bedsLabel = (JLabel) card.getClientProperty("bedsLabel");
        
        if (totalLabel != null) {
            totalLabel.setText(String.format("₹%,.0f total", totalPrice));
        }
        if (perNightLabel != null) {
            perNightLabel.setText(String.format("₹%,.0f/night", pricePerNight));
        }
        if (bedsLabel != null) {
            bedsLabel.setText(String.format("(%d beds)", beds));
        }
        
        card.revalidate();
        card.repaint();
    }
    
    private void confirmHotelSelection() {
        if (selectedHotel == null) {
            JOptionPane.showMessageDialog(this, "Please select a hotel to continue.", "No Hotel Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (!breakdownShown) {
            showBreakdownPanel();
        } else {
            proceedAfterConfirmation();
        }
    }
    
    private void showBreakdownPanel() {
        int finalNights = (Integer) (nightsSpinner != null ? nightsSpinner.getValue() : numberOfNights);
        int finalRooms = hotelRooms.getOrDefault(selectedHotel.getId(), 1);
        int finalBeds = hotelBeds.getOrDefault(selectedHotel.getId(), 2);
        LocalDate finalCheckOut = currentCheckOutDate != null ? currentCheckOutDate : checkInDate.plusDays(finalNights);
        
        double pricePerNight = selectedHotel.getPriceForBeds(finalBeds);
        double totalPrice = pricePerNight * finalNights * finalRooms;

        JPanel breakdownContent = buildBreakdownPanel(selectedHotel, finalNights, finalRooms, finalBeds, finalCheckOut, pricePerNight, totalPrice);
        breakdownPanel = breakdownContent;

        JScrollPane breakdownScrollPane = new JScrollPane(breakdownPanel);
        breakdownScrollPane.setBorder(BorderFactory.createEmptyBorder());
        breakdownScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        if (!breakdownShown) {

            JScrollPane hotelScrollPane = new JScrollPane(hotelListPanel);
            hotelScrollPane.setBorder(BorderFactory.createEmptyBorder());
            
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, hotelScrollPane, breakdownScrollPane);
            splitPane.setDividerLocation(0.65); // 65% for hotels, 35% for breakdown
            splitPane.setDividerSize(5);
            splitPane.setResizeWeight(0.65);
            splitPane.setOneTouchExpandable(true);

            contentPanel.removeAll();
            contentPanel.add(splitPane, BorderLayout.CENTER);
            contentPanel.revalidate();
            contentPanel.repaint();
            
            breakdownShown = true;

            updateFooterButtons();
        } else {

            if (splitPane != null) {
                splitPane.setRightComponent(breakdownScrollPane);
                splitPane.revalidate();
                splitPane.repaint();
            }
        }
    }
    
    private JPanel buildBreakdownPanel(Hotel hotel, int nights, int rooms, int beds, LocalDate checkOut, double pricePerNight, double totalPrice) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(245, 245, 250));

        JLabel titleLabel = new JLabel("Hotel Booking Breakdown");
        titleLabel.setFont(new Font("Serif", Font.BOLD, 20));
        titleLabel.setForeground(new Color(10, 32, 66));
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        panel.add(titleLabel);

        JPanel badgePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        badgePanel.setOpaque(false);
        JLabel badge = new JLabel("✓ Confirmed");
        badge.setFont(new Font("SansSerif", Font.BOLD, 14));
        badge.setForeground(new Color(34, 139, 34));
        badge.setBorder(new EmptyBorder(0, 0, 10, 0));
        badgePanel.add(badge);
        panel.add(badgePanel);

        JPanel detailsSection = createBreakdownSection("Hotel Details");
        
        JLabel hotelName = new JLabel("<html><b>" + hotel.getName() + "</b></html>");
        hotelName.setFont(new Font("SansSerif", Font.BOLD, 16));
        detailsSection.add(hotelName);
        
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < hotel.getStarRating(); i++) {
            stars.append("★");
        }
        JLabel starLabel = new JLabel(stars.toString());
        starLabel.setForeground(new Color(255, 215, 0));
        starLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        detailsSection.add(starLabel);
        
        JLabel locationLabel = new JLabel("📍 " + hotel.getLocation());
        locationLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        locationLabel.setForeground(new Color(100, 100, 100));
        detailsSection.add(locationLabel);
        
        JLabel distanceLabel = new JLabel("📍 " + String.format("%.1f km from city center", hotel.getDistanceFromCityCenter()));
        distanceLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        distanceLabel.setForeground(new Color(120, 120, 120));
        detailsSection.add(distanceLabel);
        
        panel.add(detailsSection);
        panel.add(Box.createVerticalStrut(10));

        JPanel bookingSection = createBreakdownSection("Booking Details");
        
        bookingSection.add(createDetailRow("Check-in:", checkInDate.format(DATE_FORMAT)));
        bookingSection.add(createDetailRow("Check-out:", checkOut.format(DATE_FORMAT)));
        bookingSection.add(createDetailRow("Nights:", String.valueOf(nights)));
        bookingSection.add(createDetailRow("Rooms:", String.valueOf(rooms)));
        bookingSection.add(createDetailRow("Beds per room:", String.valueOf(beds)));
        
        panel.add(bookingSection);
        panel.add(Box.createVerticalStrut(10));

        JPanel priceSection = createBreakdownSection("Price Breakdown");
        
        priceSection.add(createDetailRow("Price per night:", String.format("₹%,.0f", pricePerNight)));
        priceSection.add(createDetailRow("Number of nights:", String.valueOf(nights)));
        priceSection.add(createDetailRow("Number of rooms:", String.valueOf(rooms)));

        JSeparator separator = new JSeparator();
        separator.setBorder(new EmptyBorder(10, 0, 10, 0));
        priceSection.add(separator);

        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setBorder(new EmptyBorder(5, 0, 5, 0));
        JLabel totalLabel = new JLabel("Total Amount:");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel totalValue = new JLabel(String.format("₹%,.0f", totalPrice));
        totalValue.setFont(new Font("SansSerif", Font.BOLD, 18));
        totalValue.setForeground(new Color(34, 139, 34));
        totalRow.add(totalLabel, BorderLayout.WEST);
        totalRow.add(totalValue, BorderLayout.EAST);
        priceSection.add(totalRow);
        
        panel.add(priceSection);
        panel.add(Box.createVerticalGlue());
        
        return panel;
    }
    
    private JPanel createBreakdownSection(String title) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(15, 15, 15, 15)));
        
        JLabel sectionTitle = new JLabel(title);
        sectionTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        sectionTitle.setForeground(new Color(10, 32, 66));
        sectionTitle.setBorder(new EmptyBorder(0, 0, 10, 0));
        section.add(sectionTitle);
        
        return section;
    }
    
    private JPanel createDetailRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(5, 0, 5, 0));
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("SansSerif", Font.PLAIN, 12));
        labelComp.setForeground(new Color(100, 100, 100));
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("SansSerif", Font.PLAIN, 12));
        valueComp.setForeground(Color.BLACK);
        
        row.add(labelComp, BorderLayout.WEST);
        row.add(valueComp, BorderLayout.EAST);
        
        return row;
    }
    
    private void updateFooterButtons() {
        if (confirmButton != null) {
            confirmButton.setText("Continue");
        }
    }
    
    private void proceedAfterConfirmation() {
        if (selectedHotel == null) {
            return;
        }
        
        int finalNights = (Integer) (nightsSpinner != null ? nightsSpinner.getValue() : numberOfNights);
        LocalDate finalCheckOut = currentCheckOutDate != null ? currentCheckOutDate : checkInDate.plusDays(finalNights);

        if (previousFrame instanceof MultiCityFlowPage) {
            MultiCityFlowPage multiCityPage = (MultiCityFlowPage) previousFrame;
            multiCityPage.onHotelSelected(destinationCode, selectedHotel);
            dispose();
            return;
        }

        try {
            new TouristSpotsPage(destinationCode, checkInDate, finalCheckOut, selectedHotel, this);
            dispose();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error opening Tourist Spots page: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public JFrame getPreviousFrame() {
        return previousFrame;
    }
    
    /**
     * Load hotel image with fallback options for different file naming conventions
     */
    private ImageIcon loadHotelImage(Hotel hotel) {
        String imagePath = hotel.getImagePath();
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        
        String[] extensions = {".jpg", ".JPG", ".jpeg", ".JPEG", ".png", ".PNG"};

        String fullPath = "resources/images/" + imagePath;
        ImageIcon icon = new ImageIcon(fullPath);
        if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
            return icon;
        }

        try {
            String basePath = fullPath.substring(0, fullPath.lastIndexOf('.'));
            for (String ext : extensions) {
                icon = new ImageIcon(basePath + ext);
                if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                    return icon;
                }
            }
        } catch (Exception e) {

        }

        if (imagePath.contains("/")) {
            try {
                String[] parts = imagePath.split("/");
                if (parts.length == 2) {
                    String folder = parts[0];
                    String filename = parts[1];

                    String number = filename.replaceAll("[^0-9]", "");
                    if (!number.isEmpty()) {

                        for (String ext : extensions) {
                            String altPath = "resources/images/" + folder + "/" + number + ext;
                            icon = new ImageIcon(altPath);
                            if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                                return icon;
                            }
                        }
                    }

                    int hotelId = hotel.getId();
                    for (String ext : extensions) {
                        String altPath = "resources/images/" + folder + "/" + hotelId + ext;
                        icon = new ImageIcon(altPath);
                        if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                            return icon;
                        }
                    }
                }
            } catch (Exception e) {

            }
        }
        
        return null;
    }
}
