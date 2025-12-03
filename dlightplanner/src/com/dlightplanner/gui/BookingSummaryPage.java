package com.dlightplanner.gui;

import com.dlightplanner.models.*;
import com.dlightplanner.models.MultiCityTripRequest.CityLeg;
import com.dlightplanner.models.MultiCityTripRequest.FlightLeg;
import com.dlightplanner.services.AirportService;
import com.dlightplanner.services.LocalCostService;
import com.dlightplanner.utils.PDFGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class BookingSummaryPage extends JFrame {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private FlightSearchRequest singleCityRequest;
    private Flight selectedOutboundFlight;
    private Flight selectedReturnFlight;
    private Hotel selectedHotel;
    private List<TouristSpot> selectedSpots;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String destinationCode;

    private MultiCityTripRequest multiCityRequest;
    private Map<FlightLeg, Flight> selectedFlights;
    private Map<String, Hotel> selectedHotels;
    private Map<String, List<TouristSpot>> selectedTouristSpots;
    
    private AirportService airportService;
    private LocalCostService localCostService;
    private JFrame previousFrame;
    private boolean isMultiCity;

    public BookingSummaryPage(FlightSearchRequest request, Flight outbound, Flight returnFlight,
                             Hotel hotel, List<TouristSpot> spots, LocalDate checkIn, LocalDate checkOut,
                             String destCode, JFrame previousFrame) {
        this.singleCityRequest = request;
        this.selectedOutboundFlight = outbound;
        this.selectedReturnFlight = returnFlight;
        this.selectedHotel = hotel;
        this.selectedSpots = spots;
        this.checkInDate = checkIn;
        this.checkOutDate = checkOut;
        this.destinationCode = destCode;
        this.previousFrame = previousFrame;
        this.isMultiCity = false;
        
        initialize();
    }

    public BookingSummaryPage(MultiCityTripRequest request, Map<FlightLeg, Flight> flights,
                             Map<String, Hotel> hotels, Map<String, List<TouristSpot>> spots,
                             JFrame previousFrame) {
        this.multiCityRequest = request;
        this.selectedFlights = flights;
        this.selectedHotels = hotels;
        this.selectedTouristSpots = spots;
        this.previousFrame = previousFrame;
        this.isMultiCity = true;
        
        initialize();
    }
    
    private void initialize() {
        if (previousFrame != null) {
            previousFrame.setVisible(false);
        }
        
        airportService = new AirportService();
        airportService.loadAirportsFromJson("resources/airports.json");
        
        localCostService = new LocalCostService();
        localCostService.loadLocalCostsFromJson("resources/local_costs.json");
        
        setTitle("Voya | Booking Summary");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true); // Allow resizing/minimizing
        setSize(1200, 800); // Fallback size
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Default to maximized
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildContentPanel(), BorderLayout.CENTER);
        add(buildFooterPanel(), BorderLayout.SOUTH);
        
        setVisible(true);
    }
    
    private String getCityName(String code) {
        return airportService.getAirports().stream()
                .filter(airport -> airport.getIata().equalsIgnoreCase(code))
                .map(Airport::getCity)
                .findFirst()
                .orElse(code);
    }
    
    /**
     * Find a flight for the given leg by matching leg attributes (fromCode, toCode, date)
     * instead of object reference, since FlightLeg doesn't override equals/hashCode
     */
    private Flight findFlightForLeg(FlightLeg leg) {
        for (Map.Entry<FlightLeg, Flight> entry : selectedFlights.entrySet()) {
            FlightLeg key = entry.getKey();
            if (key.getFromCode().equals(leg.getFromCode()) &&
                key.getToCode().equals(leg.getToCode()) &&
                key.getDate().equals(leg.getDate())) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(10, 32, 66));
        header.setBorder(new EmptyBorder(20, 25, 20, 25));
        
        JLabel title = new JLabel("Booking Summary");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Serif", Font.BOLD, 26));
        
        String subtitle = isMultiCity ? 
            "Multi-City Trip Booking" : 
            "Trip Booking Confirmation";
        JLabel sub = new JLabel(subtitle);
        sub.setForeground(new Color(210, 225, 240));
        sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(sub, BorderLayout.SOUTH);
        
        header.add(titlePanel, BorderLayout.WEST);
        return header;
    }
    
    private JPanel buildContentPanel() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(new EmptyBorder(15, 25, 10, 25));
        
        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setOpaque(false);
        
        if (isMultiCity) {
            buildMultiCitySummary(summaryPanel);
        } else {
            buildSingleCitySummary(summaryPanel);
        }
        
        JScrollPane scrollPane = new JScrollPane(summaryPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        center.add(scrollPane, BorderLayout.CENTER);
        return center;
    }
    
    private void buildSingleCitySummary(JPanel panel) {
        double totalCost = 0;

        JPanel flightSection = createSectionPanel("FLIGHT COSTS");
        if (selectedOutboundFlight != null && singleCityRequest != null) {
            double flightTotal = addFlightCosts(flightSection, selectedOutboundFlight, selectedReturnFlight, singleCityRequest);
            totalCost += flightTotal;
        } else {
            addCostRow(flightSection, "Flight information not available. Please select flights first.", 0);
            addCostRow(flightSection, "Total Flight Cost", 0, true);
        }
        panel.add(flightSection);
        panel.add(Box.createVerticalStrut(15));

        JPanel hotelSection = createSectionPanel("HOTEL COSTS");
        double hotelTotal = addHotelCosts(hotelSection, selectedHotel, checkInDate, checkOutDate);
        totalCost += hotelTotal;
        panel.add(hotelSection);
        panel.add(Box.createVerticalStrut(15));

        JPanel spotsSection = createSectionPanel("TOURIST SPOTS COSTS");
        int totalTravelersForSpots = (singleCityRequest != null) ? 
            (singleCityRequest.getAdults() + singleCityRequest.getChildren()) : 1;
        double spotsTotal = addTouristSpotCosts(spotsSection, selectedSpots, totalTravelersForSpots);
        totalCost += spotsTotal;
        panel.add(spotsSection);
        panel.add(Box.createVerticalStrut(15));

        JPanel foodSection = createSectionPanel("LOCAL FOOD COSTS");
        int numberOfDays = (int) ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (numberOfDays < 1) numberOfDays = 1;
        double foodCostPerPerson = localCostService.getLocalFoodCost(destinationCode);
        int totalTravelers = (singleCityRequest != null) ? 
            (singleCityRequest.getAdults() + singleCityRequest.getChildren()) : 1;
        double foodTotal = foodCostPerPerson * totalTravelers * numberOfDays;
        addCostRow(foodSection, "Food cost per person per day", foodCostPerPerson);
        addCostRow(foodSection, "Number of travelers", totalTravelers);
        addCostRow(foodSection, "Number of days", numberOfDays);
        addCostRow(foodSection, "Total Food Cost", foodTotal, true);
        totalCost += foodTotal;
        panel.add(foodSection);
        panel.add(Box.createVerticalStrut(15));

        JPanel travelSection = createSectionPanel("LOCAL TRAVEL COSTS");
        double travelCostPerPerson = localCostService.getLocalTravelCost(destinationCode);
        double travelTotal = travelCostPerPerson * totalTravelers * numberOfDays;
        addCostRow(travelSection, "Travel cost per person per day", travelCostPerPerson);
        addCostRow(travelSection, "Number of travelers", totalTravelers);
        addCostRow(travelSection, "Number of days", numberOfDays);
        addCostRow(travelSection, "Total Travel Cost", travelTotal, true);
        totalCost += travelTotal;
        panel.add(travelSection);
        panel.add(Box.createVerticalStrut(15));

        JPanel totalSection = createTotalPanel(totalCost);
        panel.add(totalSection);
    }
    
    private void buildMultiCitySummary(JPanel panel) {
        double totalCost = 0;

        JPanel flightSection = createSectionPanel("FLIGHT COSTS");
        double flightTotal = addMultiCityFlightCosts(flightSection);
        totalCost += flightTotal;
        panel.add(flightSection);
        panel.add(Box.createVerticalStrut(15));

        JPanel hotelSection = createSectionPanel("HOTEL COSTS");
        double hotelTotal = addMultiCityHotelCosts(hotelSection);
        totalCost += hotelTotal;
        panel.add(hotelSection);
        panel.add(Box.createVerticalStrut(15));

        JPanel spotsSection = createSectionPanel("TOURIST SPOTS COSTS");
        double spotsTotal = addMultiCityTouristSpotCosts(spotsSection);
        totalCost += spotsTotal;
        panel.add(spotsSection);
        panel.add(Box.createVerticalStrut(15));

        JPanel foodSection = createSectionPanel("LOCAL FOOD COSTS");
        double foodTotal = addMultiCityFoodCosts(foodSection);
        totalCost += foodTotal;
        panel.add(foodSection);
        panel.add(Box.createVerticalStrut(15));

        JPanel travelSection = createSectionPanel("LOCAL TRAVEL COSTS");
        double travelTotal = addMultiCityTravelCosts(travelSection);
        totalCost += travelTotal;
        panel.add(travelSection);
        panel.add(Box.createVerticalStrut(15));

        JPanel totalSection = createTotalPanel(totalCost);
        panel.add(totalSection);
    }
    
    private double addFlightCosts(JPanel panel, Flight outbound, Flight returnFlight, FlightSearchRequest request) {
        double total = 0;
        
        if (outbound != null) {
            addCostRow(panel, "Departure: " + outbound.getAirline() + " " + outbound.getFlightNumber(), 0);
            double adultFare = outbound.getFare();
            double childFare = adultFare * 0.75;
            double infantFare = adultFare * 0.10;
            
            int adults = request.getAdults();
            int children = request.getChildren();
            int infants = request.getInfants();
            
            if (adults > 0) {
                double cost = adultFare * adults;
                addCostRow(panel, "  Adults (" + adults + " × ₹" + String.format("%,.0f", adultFare) + ")", cost);
                total += cost;
            }
            if (children > 0) {
                double cost = childFare * children;
                addCostRow(panel, "  Children (" + children + " × ₹" + String.format("%,.0f", childFare) + ")", cost);
                total += cost;
            }
            if (infants > 0) {
                double cost = infantFare * infants;
                addCostRow(panel, "  Infants (" + infants + " × ₹" + String.format("%,.0f", infantFare) + ")", cost);
                total += cost;
            }
        }
        
        if (returnFlight != null) {
            addCostRow(panel, "Return: " + returnFlight.getAirline() + " " + returnFlight.getFlightNumber(), 0);
            double adultFare = returnFlight.getFare();
            double childFare = adultFare * 0.75;
            double infantFare = adultFare * 0.10;
            
            int adults = request.getAdults();
            int children = request.getChildren();
            int infants = request.getInfants();
            
            if (adults > 0) {
                double cost = adultFare * adults;
                addCostRow(panel, "  Adults (" + adults + " × ₹" + String.format("%,.0f", adultFare) + ")", cost);
                total += cost;
            }
            if (children > 0) {
                double cost = childFare * children;
                addCostRow(panel, "  Children (" + children + " × ₹" + String.format("%,.0f", childFare) + ")", cost);
                total += cost;
            }
            if (infants > 0) {
                double cost = infantFare * infants;
                addCostRow(panel, "  Infants (" + infants + " × ₹" + String.format("%,.0f", infantFare) + ")", cost);
                total += cost;
            }
        }
        
        addCostRow(panel, "Total Flight Cost", total, true);
        return total;
    }
    
    private double addHotelCosts(JPanel panel, Hotel hotel, LocalDate checkIn, LocalDate checkOut) {
        if (hotel == null) {
            addCostRow(panel, "No hotel selected", 0, true);
            return 0;
        }
        
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < 1) nights = 1;
        
        double pricePerNight = hotel.getPricePerNight();
        double total = pricePerNight * nights;
        
        addCostRow(panel, "Hotel: " + hotel.getName(), 0);
        addCostRow(panel, "  Price per night", pricePerNight);
        addCostRow(panel, "  Number of nights", nights);
        addCostRow(panel, "Total Hotel Cost", total, true);
        
        return total;
    }
    
    private double addTouristSpotCosts(JPanel panel, List<TouristSpot> spots, int travelers) {
        if (spots == null || spots.isEmpty()) {
            addCostRow(panel, "No tourist spots selected", 0, true);
            return 0;
        }
        
        double total = 0;
        for (TouristSpot spot : spots) {
            double spotCost = spot.getPrice() * travelers;
            addCostRow(panel, spot.getName() + " (₹" + String.format("%,.0f", spot.getPrice()) + " × " + travelers + ")", spotCost);
            total += spotCost;
        }
        
        addCostRow(panel, "Total Tourist Spots Cost", total, true);
        return total;
    }
    
    private double addMultiCityFlightCosts(JPanel panel) {
        double total = 0;
        List<FlightLeg> legs = multiCityRequest.getFlightLegs();
        
        for (int i = 0; i < legs.size(); i++) {
            FlightLeg leg = legs.get(i);
            Flight flight = findFlightForLeg(leg);
            
            if (flight != null) {
                String route = getCityName(leg.getFromCode()) + " → " + getCityName(leg.getToCode());
                addCostRow(panel, "Flight " + (i + 1) + ": " + route, 0);
                addCostRow(panel, "  " + flight.getAirline() + " " + flight.getFlightNumber(), 0);
                
                double adultFare = flight.getFare();
                double childFare = adultFare * 0.75;
                double infantFare = adultFare * 0.10;
                
                int adults = multiCityRequest.getAdults();
                int children = multiCityRequest.getChildren();
                int infants = multiCityRequest.getInfants();
                
                double legTotal = 0;
                if (adults > 0) {
                    double cost = adultFare * adults;
                    addCostRow(panel, "    Adults (" + adults + " × ₹" + String.format("%,.0f", adultFare) + ")", cost);
                    legTotal += cost;
                }
                if (children > 0) {
                    double cost = childFare * children;
                    addCostRow(panel, "    Children (" + children + " × ₹" + String.format("%,.0f", childFare) + ")", cost);
                    legTotal += cost;
                }
                if (infants > 0) {
                    double cost = infantFare * infants;
                    addCostRow(panel, "    Infants (" + infants + " × ₹" + String.format("%,.0f", infantFare) + ")", cost);
                    legTotal += cost;
                }
                addCostRow(panel, "  Leg Total", legTotal);
                total += legTotal;
            }
        }
        
        addCostRow(panel, "Total Flight Cost", total, true);
        return total;
    }
    
    private double addMultiCityHotelCosts(JPanel panel) {
        double total = 0;
        List<CityLeg> cities = multiCityRequest.getCityLegs();
        
        for (int i = 0; i < cities.size(); i++) {
            CityLeg city = cities.get(i);
            String destCode = city.getDestinationCode();
            Hotel hotel = selectedHotels.get(destCode);
            
            if (hotel != null) {
                long nights = ChronoUnit.DAYS.between(city.getArrivalDate(), city.getDepartureDate());
                if (nights < 1) nights = 1;
                
                double pricePerNight = hotel.getPricePerNight();
                double hotelTotal = pricePerNight * nights;
                
                addCostRow(panel, "City " + (i + 1) + ": " + getCityName(destCode), 0);
                addCostRow(panel, "  Hotel: " + hotel.getName(), 0);
                addCostRow(panel, "  Nights: " + nights, 0);
                addCostRow(panel, "  Total: ₹" + String.format("%,.0f", hotelTotal), hotelTotal);
                total += hotelTotal;
            }
        }
        
        addCostRow(panel, "Total Hotel Cost", total, true);
        return total;
    }
    
    private double addMultiCityTouristSpotCosts(JPanel panel) {
        double total = 0;
        int travelers = multiCityRequest.getAdults() + multiCityRequest.getChildren();
        List<CityLeg> cities = multiCityRequest.getCityLegs();
        
        for (CityLeg city : cities) {
            String destCode = city.getDestinationCode();
            List<TouristSpot> spots = selectedTouristSpots.getOrDefault(destCode, null);
            
            if (spots != null && !spots.isEmpty()) {
                addCostRow(panel, getCityName(destCode) + " Tourist Spots", 0);
                double cityTotal = 0;
                for (TouristSpot spot : spots) {
                    double spotCost = spot.getPrice() * travelers;
                    addCostRow(panel, "  " + spot.getName() + " (₹" + String.format("%,.0f", spot.getPrice()) + " × " + travelers + ")", spotCost);
                    cityTotal += spotCost;
                }
                addCostRow(panel, "  City Total", cityTotal);
                total += cityTotal;
            }
        }
        
        addCostRow(panel, "Total Tourist Spots Cost", total, true);
        return total;
    }
    
    private double addMultiCityFoodCosts(JPanel panel) {
        double total = 0;
        int travelers = multiCityRequest.getAdults() + multiCityRequest.getChildren();
        List<CityLeg> cities = multiCityRequest.getCityLegs();
        String originCode = multiCityRequest.getOriginCode();
        
        for (int i = 0; i < cities.size(); i++) {
            CityLeg city = cities.get(i);
            String destCode = city.getDestinationCode();
            boolean isLastCity = (i == cities.size() - 1);

            if (isLastCity && destCode.equalsIgnoreCase(originCode)) {
                continue;
            }
            
            long days = ChronoUnit.DAYS.between(city.getArrivalDate(), city.getDepartureDate());
            if (days < 1) days = 1;
            
            double foodCostPerPerson = localCostService.getLocalFoodCost(destCode);
            double cityFoodTotal = foodCostPerPerson * travelers * days;
            
            addCostRow(panel, getCityName(destCode), 0);
            addCostRow(panel, "  Food cost per person per day: ₹" + String.format("%,.0f", foodCostPerPerson), 0);
            addCostRow(panel, "  Travelers: " + travelers + ", Days: " + days, 0);
            addCostRow(panel, "  City Total: ₹" + String.format("%,.0f", cityFoodTotal), cityFoodTotal);
            total += cityFoodTotal;
        }
        
        addCostRow(panel, "Total Food Cost", total, true);
        return total;
    }
    
    private double addMultiCityTravelCosts(JPanel panel) {
        double total = 0;
        int travelers = multiCityRequest.getAdults() + multiCityRequest.getChildren();
        List<CityLeg> cities = multiCityRequest.getCityLegs();
        String originCode = multiCityRequest.getOriginCode();
        
        for (int i = 0; i < cities.size(); i++) {
            CityLeg city = cities.get(i);
            String destCode = city.getDestinationCode();
            boolean isLastCity = (i == cities.size() - 1);

            if (isLastCity && destCode.equalsIgnoreCase(originCode)) {
                continue;
            }
            
            int days = (int) ChronoUnit.DAYS.between(city.getArrivalDate(), city.getDepartureDate());
            if (days < 1) days = 1;
            double travelCostPerPerson = localCostService.getLocalTravelCost(destCode);
            double cityTravelTotal = travelCostPerPerson * travelers * days;
            
            addCostRow(panel, getCityName(destCode), 0);
            addCostRow(panel, "  Travel cost per person per day: ₹" + String.format("%,.0f", travelCostPerPerson), 0);
            addCostRow(panel, "  Travelers: " + travelers + ", Days: " + days, 0);
            addCostRow(panel, "  City Total: ₹" + String.format("%,.0f", cityTravelTotal), cityTravelTotal);
            total += cityTravelTotal;
        }
        
        addCostRow(panel, "Total Travel Cost", total, true);
        return total;
    }
    
    private JPanel createSectionPanel(String title) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(10, 32, 66), 2),
                new EmptyBorder(15, 20, 15, 20)));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(new Color(10, 32, 66));
        section.add(titleLabel);
        section.add(Box.createVerticalStrut(10));
        
        return section;
    }
    
    private void addCostRow(JPanel panel, String label, double cost) {
        addCostRow(panel, label, cost, false);
    }
    
    private void addCostRow(JPanel panel, String label, double cost, boolean isTotal) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(3, 0, 3, 0));
        
        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("SansSerif", isTotal ? Font.BOLD : Font.PLAIN, isTotal ? 14 : 12));
        labelComponent.setForeground(isTotal ? new Color(10, 32, 66) : new Color(50, 50, 50));
        
        if (cost > 0 || isTotal) {
            JLabel costLabel = new JLabel("₹" + String.format("%,.0f", cost));
            costLabel.setFont(new Font("SansSerif", isTotal ? Font.BOLD : Font.PLAIN, isTotal ? 14 : 12));
            costLabel.setForeground(isTotal ? new Color(10, 32, 66) : new Color(50, 50, 50));
            costLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            
            row.add(labelComponent, BorderLayout.WEST);
            row.add(costLabel, BorderLayout.EAST);
        } else {
            row.add(labelComponent, BorderLayout.WEST);
        }
        
        panel.add(row);
    }
    
    private JPanel createTotalPanel(double total) {
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setBackground(new Color(10, 32, 66));
        totalPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        
        JLabel totalLabel = new JLabel("GRAND TOTAL");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        totalLabel.setForeground(Color.WHITE);
        
        JLabel totalAmount = new JLabel("₹" + String.format("%,.0f", total));
        totalAmount.setFont(new Font("SansSerif", Font.BOLD, 24));
        totalAmount.setForeground(Color.WHITE);
        totalAmount.setHorizontalAlignment(SwingConstants.RIGHT);
        
        totalPanel.add(totalLabel, BorderLayout.WEST);
        totalPanel.add(totalAmount, BorderLayout.EAST);
        
        return totalPanel;
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
            }
            dispose();
        });
        
        JButton savePdfButton = new JButton("💾 Save as PDF");
        savePdfButton.setBackground(new Color(10, 32, 66));
        savePdfButton.setForeground(Color.WHITE);
        savePdfButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        savePdfButton.addActionListener(e -> exportToPDF());
        
        JButton confirmButton = new JButton("Confirm Booking");
        confirmButton.setBackground(new Color(34, 139, 34));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        confirmButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, 
                "Booking confirmed! Thank you for choosing Voya.\n\nYour booking details have been saved.", 
                "Booking Confirmed", 
                JOptionPane.INFORMATION_MESSAGE);
            if (previousFrame != null) {
                previousFrame.dispose();
            }
            dispose();
            new HomePage();
        });
        
        footer.add(backButton);
        footer.add(savePdfButton);
        footer.add(confirmButton);
        
        return footer;
    }
    
    private void exportToPDF() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Booking Summary as PDF");

        String defaultFileName = "Voya_Booking_Summary_" + 
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";
        fileChooser.setSelectedFile(new File(defaultFileName));

        FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf");
        fileChooser.setFileFilter(pdfFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        
        int userSelection = fileChooser.showSaveDialog(this);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();

            final String finalFilePath;
            if (!filePath.toLowerCase().endsWith(".pdf")) {
                finalFilePath = filePath + ".pdf";
            } else {
                finalFilePath = filePath;
            }

            JDialog progressDialog = new JDialog(this, "Exporting PDF", true);
            progressDialog.setSize(300, 100);
            progressDialog.setLocationRelativeTo(this);
            JLabel progressLabel = new JLabel("Generating PDF... Please wait.");
            progressLabel.setHorizontalAlignment(SwingConstants.CENTER);
            progressDialog.add(progressLabel);

            Thread exportThread = new Thread(() -> {
                SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));
                final boolean[] success = {false};
                
                if (isMultiCity) {

                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(this,
                            "Multi-city PDF export is not yet implemented.",
                            "Feature Not Available",
                            JOptionPane.INFORMATION_MESSAGE);
                    });
                    return;
                } else {
                    success[0] = PDFGenerator.generatePDF(finalFilePath, singleCityRequest,
                        selectedOutboundFlight, selectedReturnFlight,
                        selectedHotel, selectedSpots, checkInDate, checkOutDate,
                        destinationCode, airportService, localCostService);
                }
                
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    if (success[0]) {
                        JOptionPane.showMessageDialog(this,
                            "Booking summary has been saved successfully!\n\n" +
                            "File: " + finalFilePath,
                            "Export Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        String textFilePath = finalFilePath.replace(".pdf", ".txt");
                        if (new File(textFilePath).exists()) {
                            JOptionPane.showMessageDialog(this,
                                "PDF library not found. A text file has been created instead.\n\n" +
                                "File: " + textFilePath + "\n\n" +
                                "To enable PDF export, please add pdfbox-2.x.x.jar to your classpath.",
                                "Text File Created",
                                JOptionPane.WARNING_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this,
                                "Failed to export booking summary.\n\n" +
                                "Please ensure you have write permissions to the selected location.",
                                "Export Failed",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
            });
            exportThread.start();
        }
    }
}

