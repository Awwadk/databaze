package com.dlightplanner.gui;

import com.dlightplanner.models.Hotel;
import com.dlightplanner.models.TouristSpot;
import com.dlightplanner.services.AirportService;
import com.dlightplanner.services.ItineraryGenerator;
import com.dlightplanner.services.ItineraryGenerator.TimeSlot;
import com.dlightplanner.services.ItineraryGenerator.TravelInfo;
import com.dlightplanner.services.TouristSpotService;
import com.dlightplanner.services.WeatherService;
import com.dlightplanner.services.WeatherService.WeatherForecast;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

public class SmartItineraryPage extends JFrame {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final int MAX_HOURS_PER_DAY = 10; // Maximum hours per day (9 AM to 7 PM)
    
    private final String destinationCode;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final Hotel selectedHotel;
    private final List<TouristSpot> selectedSpots;
    private int numberOfDays; // Will be calculated from spots
    
    private final AirportService airportService;
    private final ItineraryGenerator itineraryGenerator;
    private final WeatherService weatherService;
    private final TouristSpotService touristSpotService;
    private JPanel itineraryPanel;
    private JLabel smartMessageLabel;
    private JFrame previousFrame;
    
    public SmartItineraryPage(String destinationCode, LocalDate checkInDate, LocalDate checkOutDate, 
                             Hotel selectedHotel, List<TouristSpot> selectedSpots) {
        this(destinationCode, checkInDate, checkOutDate, selectedHotel, selectedSpots, null);
    }
    
    public SmartItineraryPage(String destinationCode, LocalDate checkInDate, LocalDate checkOutDate, 
                             Hotel selectedHotel, List<TouristSpot> selectedSpots, JFrame previousFrame) {
        this.destinationCode = destinationCode;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.selectedHotel = selectedHotel;
        this.selectedSpots = selectedSpots != null ? selectedSpots : new ArrayList<>();
        this.previousFrame = previousFrame;

        if (previousFrame != null) {
            previousFrame.setVisible(false);
        }
        
        airportService = new AirportService();
        airportService.loadAirportsFromJson("resources/airports.json");
        
        itineraryGenerator = new ItineraryGenerator();
        weatherService = new WeatherService();
        
        touristSpotService = new TouristSpotService();
        touristSpotService.loadTouristSpotsFromJson("resources/tourist_spots.json");

        this.numberOfDays = (int) ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (this.numberOfDays < 1) {
            this.numberOfDays = 1; // At least 1 day
        }
        
        setTitle("Voya | Smart Itinerary");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true); // Allow resizing/minimizing
        setSize(1400, 800); // Fallback size
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Default to maximized
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildContentPanel(), BorderLayout.CENTER);
        add(buildFooterPanel(), BorderLayout.SOUTH);
        
        generateItinerary();
        setVisible(true);
    }
    
    private String getCityNameFromCode(String code) {
        return airportService.getAirports().stream()
                .filter(airport -> airport.getIata().equalsIgnoreCase(code))
                .map(airport -> airport.getCity())
                .findFirst()
                .orElse(code);
    }
    
    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(10, 32, 66));
        header.setBorder(new EmptyBorder(20, 25, 20, 25));
        
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        
        JLabel title = new JLabel("Your Smart Itinerary");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Serif", Font.BOLD, 26));
        
        topRow.add(title, BorderLayout.WEST);
        
        String cityName = getCityNameFromCode(destinationCode);
        String daysInfo = String.format("%d day%s (Check-in: %s to Check-out: %s)", 
            numberOfDays, numberOfDays == 1 ? "" : "s",
            checkInDate.format(DATE_FORMAT),
            checkOutDate.format(DATE_FORMAT));
        
        JLabel sub = new JLabel(String.format("%s • %s to %s • %s • Hotel: %s",
                cityName, 
                checkInDate.format(DATE_FORMAT), 
                checkOutDate.format(DATE_FORMAT),
                daysInfo,
                selectedHotel.getName()));
        sub.setForeground(new Color(210, 225, 240));
        sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        header.add(topRow, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        
        return header;
    }
    
    private JPanel buildContentPanel() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(new EmptyBorder(15, 25, 10, 25));

        smartMessageLabel = new JLabel();
        smartMessageLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        smartMessageLabel.setForeground(new Color(50, 50, 50));
        smartMessageLabel.setBorder(new EmptyBorder(10, 15, 10, 15));
        smartMessageLabel.setBackground(new Color(255, 248, 220));
        smartMessageLabel.setOpaque(true);
        updateSmartMessages();
        
        itineraryPanel = new JPanel();
        itineraryPanel.setLayout(new BoxLayout(itineraryPanel, BoxLayout.Y_AXIS));
        itineraryPanel.setOpaque(false);
        
        JScrollPane scrollPane = new JScrollPane(itineraryPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        center.add(smartMessageLabel, BorderLayout.NORTH);
        center.add(scrollPane, BorderLayout.CENTER);
        
        return center;
    }
    
    private JPanel buildFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBorder(new EmptyBorder(10, 25, 15, 25));
        
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> {
            if (previousFrame != null) {
                previousFrame.setVisible(true);
            }
            dispose();
        });
        
        JButton summaryButton = new JButton("View Booking Summary");
        summaryButton.setBackground(new Color(0, 123, 255));
        summaryButton.setForeground(Color.WHITE);
        summaryButton.addActionListener(e -> {

            com.dlightplanner.models.FlightSearchRequest flightRequest = null;
            com.dlightplanner.models.Flight outboundFlight = null;
            com.dlightplanner.models.Flight returnFlight = null;

            JFrame currentFrame = previousFrame;
            while (currentFrame != null) {
                if (currentFrame instanceof FlightPage) {
                    FlightPage flightPage = (FlightPage) currentFrame;
                    flightRequest = flightPage.getRequest();
                    outboundFlight = flightPage.getSelectedOutboundFlight();
                    returnFlight = flightPage.getSelectedReturnFlight();
                    break;
                }

                if (currentFrame instanceof HotelPage) {
                    currentFrame = ((HotelPage) currentFrame).getPreviousFrame();
                } else if (currentFrame instanceof TouristSpotsPage) {
                    currentFrame = ((TouristSpotsPage) currentFrame).getPreviousFrame();
                } else {
                    break;
                }
            }
            
            new BookingSummaryPage(flightRequest, outboundFlight, returnFlight, selectedHotel, selectedSpots, 
                                 checkInDate, checkOutDate, destinationCode, this);
        });
        
        footer.add(backButton);
        footer.add(summaryButton);
        
        return footer;
    }
    
    private void updateSmartMessages() {
        StringBuilder messages = new StringBuilder();
        messages.append("<html>💡 <b>Smart Tips:</b> ");
        
        if (selectedSpots.isEmpty()) {
            messages.append("No spots selected. Add tourist spots to generate a detailed itinerary.");
        } else {

            WeatherForecast todayWeather = weatherService.getWeatherForecast(destinationCode, checkInDate);
            messages.append(todayWeather.getConditionIcon()).append(" ");
            messages.append(weatherService.getWeatherRecommendation(todayWeather)).append(" | ");

            messages.append("Best visiting times shown for each spot. ");
            messages.append("Travel times and crowd levels displayed. ");

            if (checkInDate.getDayOfWeek().getValue() >= 5) {
                messages.append("Weekend - Higher crowds expected at popular spots.");
            }
        }
        
        messages.append("</html>");
        smartMessageLabel.setText(messages.toString());
    }
    
    private void generateItinerary() {
        itineraryPanel.removeAll();
        
        if (selectedSpots.isEmpty()) {
            JLabel noSpotsLabel = new JLabel("<html><center><h3>No Tourist Spots Selected</h3>" +
                    "<p>You can still enjoy your stay at " + selectedHotel.getName() + "</p>" +
                    "<p>Explore the city at your own pace!</p></center></html>");
            noSpotsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            noSpotsLabel.setBorder(new EmptyBorder(50, 0, 50, 0));
            itineraryPanel.add(noSpotsLabel);
        } else {


            Map<LocalDate, List<TouristSpot>> dayWiseSpots = itineraryGenerator.groupSpotsByProximity(
                    selectedSpots, selectedHotel, checkInDate, numberOfDays);

            Set<Integer> selectedSpotIds = new HashSet<>();
            Map<Integer, TouristSpot> selectedSpotsMap = new HashMap<>();
            for (TouristSpot spot : selectedSpots) {
                selectedSpotIds.add(spot.getId());
                selectedSpotsMap.put(spot.getId(), spot);
            }

            Set<Integer> distributedSpotIds = new HashSet<>();
            Map<Integer, TouristSpot> distributedSpotsMap = new HashMap<>();
            for (List<TouristSpot> daySpots : dayWiseSpots.values()) {
                for (TouristSpot spot : daySpots) {
                    distributedSpotIds.add(spot.getId());
                    distributedSpotsMap.put(spot.getId(), spot);
                }
            }

            Set<Integer> missingSpotIds = new HashSet<>(selectedSpotIds);
            missingSpotIds.removeAll(distributedSpotIds);

            if (!missingSpotIds.isEmpty()) {
                for (Integer missingId : missingSpotIds) {
                    TouristSpot missingSpot = selectedSpotsMap.get(missingId);
                    if (missingSpot != null) {

                        LocalDate bestDay = null;
                        int minSpots = Integer.MAX_VALUE;
                        for (int d = 0; d < numberOfDays; d++) {
                            LocalDate dayDate = checkInDate.plusDays(d);
                            List<TouristSpot> daySpots = dayWiseSpots.computeIfAbsent(dayDate, k -> new ArrayList<>());
                            int count = daySpots.size();
                            if (count < minSpots) {
                                minSpots = count;
                                bestDay = dayDate;
                            }
                        }
                        if (bestDay != null) {
                            dayWiseSpots.get(bestDay).add(missingSpot);
                            distributedSpotIds.add(missingId);
                        }
                    }
                }
            }

            Set<Integer> finalCheck = new HashSet<>(selectedSpotIds);
            finalCheck.retainAll(distributedSpotIds);

            if (finalCheck.size() < selectedSpotIds.size()) {
                for (TouristSpot selectedSpot : selectedSpots) {
                    boolean found = false;
                    for (List<TouristSpot> daySpots : dayWiseSpots.values()) {
                        for (TouristSpot daySpot : daySpots) {
                            if (daySpot.getId() == selectedSpot.getId()) {
                                found = true;
                                break;
                            }
                        }
                        if (found) break;
                    }
                    if (!found) {

                        LocalDate dayDate = checkInDate.plusDays(0);
                        List<TouristSpot> daySpots = dayWiseSpots.computeIfAbsent(dayDate, k -> new ArrayList<>());
                        daySpots.add(selectedSpot);
                    }
                }
            }

            for (int day = 0; day < numberOfDays; day++) {
                LocalDate currentDate = checkInDate.plusDays(day);
                List<TouristSpot> daySpots = dayWiseSpots.getOrDefault(currentDate, new ArrayList<>());

                int dayDuration = calculateDayDuration(daySpots);
                int availableHours = MAX_HOURS_PER_DAY - dayDuration;

                if (daySpots.size() < 4) {
                    List<TouristSpot> recommendedSpots = getRecommendedSpotsForExtraTime(
                            destinationCode, selectedSpotIds, daySpots, availableHours);

                    int spotsNeeded = Math.min(4 - daySpots.size(), recommendedSpots.size());
                    if (spotsNeeded > 0) {
                        daySpots.addAll(recommendedSpots.subList(0, spotsNeeded));
                    }
                }

                List<TouristSpot> optimizedRoute = itineraryGenerator.optimizeRoute(daySpots, selectedHotel);
                
                itineraryPanel.add(createDayCard(day + 1, currentDate, optimizedRoute));
                itineraryPanel.add(Box.createVerticalStrut(20));
            }
        }
        
        itineraryPanel.revalidate();
        itineraryPanel.repaint();
    }
    
    
    private JPanel createDayCard(int dayNumber, LocalDate date, List<TouristSpot> spots) {
        JPanel dayCard = new JPanel();
        dayCard.setLayout(new BoxLayout(dayCard, BoxLayout.Y_AXIS));
        dayCard.setBackground(Color.WHITE);
        dayCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(10, 32, 66), 2),
                new EmptyBorder(20, 20, 20, 20)));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        String dayName = date.getDayOfWeek().toString().substring(0, 1) + 
                        date.getDayOfWeek().toString().substring(1).toLowerCase();
        JLabel dayLabel = new JLabel(dayName + ", " + date.format(DATE_FORMAT));
        dayLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        dayLabel.setForeground(new Color(10, 32, 66));
        
        headerPanel.add(dayLabel, BorderLayout.WEST);
        
        dayCard.add(headerPanel);
        dayCard.add(Box.createVerticalStrut(15));
        
        if (spots.isEmpty()) {
            JLabel freeDayLabel = new JLabel("Free day - Explore at your own pace or relax at the hotel");
            freeDayLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
            freeDayLabel.setForeground(new Color(150, 150, 150));
            freeDayLabel.setBorder(new EmptyBorder(10, 0, 10, 0));
            dayCard.add(freeDayLabel);
        } else {

            Map<TouristSpot, TimeSlot> schedule = itineraryGenerator.scheduleTimeSlots(
                    spots, selectedHotel, numberOfDays, selectedSpots.size());

            double totalDuration = 0;
            for (TouristSpot spot : spots) {
                TimeSlot slot = schedule.get(spot);
                if (slot != null) {
                    long minutes = java.time.temporal.ChronoUnit.MINUTES.between(
                            slot.getStartTime(), slot.getEndTime());
                    totalDuration += minutes / 60.0;
                }
            }
            JLabel durationLabel = new JLabel(String.format("Total: %.1f hours (%d spots)", totalDuration, spots.size()));
            durationLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            durationLabel.setForeground(new Color(100, 100, 100));
            headerPanel.add(durationLabel, BorderLayout.EAST);

            List<TouristSpot> sortedSpots = new ArrayList<>(spots);
            sortedSpots.sort(Comparator.comparing(spot -> {
                TimeSlot slot = schedule.get(spot);
                return slot != null ? slot.getStartTime() : LocalTime.of(23, 59);
            }));

            WeatherForecast dayWeather = weatherService.getWeatherForecast(destinationCode, date);

            JPanel weatherPanel = createWeatherPanel(dayWeather);
            dayCard.add(weatherPanel);
            dayCard.add(Box.createVerticalStrut(10));

            for (int i = 0; i < sortedSpots.size(); i++) {
                TouristSpot spot = sortedSpots.get(i);
                TimeSlot timeSlot = schedule.get(spot);

                if (timeSlot == null) {

                    LocalTime startTime = LocalTime.of(9, 0);
                    if (i > 0) {
                        TouristSpot prevSpot = sortedSpots.get(i - 1);
                        TimeSlot prevSlot = schedule.get(prevSpot);
                        if (prevSlot != null) {
                            startTime = prevSlot.getEndTime().plusMinutes(30); // 30 min travel buffer
                            startTime = roundTimeTo15Minutes(startTime); // Round to 15-minute interval
                        }
                    }

                    if (startTime.isAfter(LocalTime.of(20, 0))) {
                        startTime = LocalTime.of(9, 0).plusHours(i);
                        startTime = roundTimeTo15Minutes(startTime); // Round to 15-minute interval
                    }
                    LocalTime endTime = startTime.plusHours(1).plusMinutes(30); // 1.5 hours default
                    endTime = roundTimeTo15Minutes(endTime); // Round to 15-minute interval
                    timeSlot = new TimeSlot(startTime, endTime);
                    schedule.put(spot, timeSlot); // Add to schedule
                }

                TravelInfo travelInfo = null;
                if (i > 0) {
                    TouristSpot previousSpot = sortedSpots.get(i - 1);
                    travelInfo = itineraryGenerator.getTravelInfo(previousSpot, spot);
                }
                
                JPanel spotTimeSlot = createSpotTimeSlot(spot, timeSlot, date, travelInfo);
                dayCard.add(spotTimeSlot);
                dayCard.add(Box.createVerticalStrut(10));
            }

            JPanel recommendationsPanel = createTimeBasedRecommendations(date, schedule, spots, dayNumber);
            dayCard.add(recommendationsPanel);
        }
        
        return dayCard;
    }
    
    private JPanel createWeatherPanel(WeatherForecast weather) {
        JPanel weatherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        weatherPanel.setBackground(new Color(230, 245, 255));
        weatherPanel.setBorder(new EmptyBorder(8, 12, 8, 12));
        
        JLabel weatherLabel = new JLabel(String.format(
            "%s %s | %d°C | Humidity: %d%% | Rain: %.0f%%",
            weather.getConditionIcon(),
            weather.getCondition(),
            weather.getTemperature(),
            weather.getHumidity(),
            weather.getPrecipitation()
        ));
        weatherLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        weatherLabel.setForeground(new Color(10, 32, 66));
        
        weatherPanel.add(weatherLabel);
        return weatherPanel;
    }
    
    private JPanel createSpotTimeSlot(TouristSpot spot, TimeSlot timeSlot, LocalDate date, TravelInfo travelInfo) {
        JPanel slotPanel = new JPanel(new BorderLayout(15, 0));
        slotPanel.setBackground(new Color(245, 250, 255));
        slotPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 220, 240), 1),
                new EmptyBorder(12, 12, 12, 12)));

        String timeSlotStr = formatTimeSlot(timeSlot.getStartTime(), timeSlot.getEndTime());
        JLabel timeLabel = new JLabel(timeSlotStr);
        timeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        timeLabel.setForeground(new Color(10, 32, 66));
        timeLabel.setPreferredSize(new Dimension(150, 0));

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(spot.getName());
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        
        JLabel categoryLabel = new JLabel("📍 " + spot.getCategory() + " • " + spot.getLocation());
        categoryLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        categoryLabel.setForeground(new Color(100, 100, 100));

        long totalMinutes = java.time.temporal.ChronoUnit.MINUTES.between(timeSlot.getStartTime(), timeSlot.getEndTime());
        double scheduledHours = totalMinutes / 60.0;
        String durationText = String.format("⏱️ %.1f hours", scheduledHours);
        JLabel durationLabel = new JLabel(durationText);
        durationLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        durationLabel.setForeground(new Color(120, 120, 120));

        String bestTime = getBestTimeMessage(spot);
        if (!bestTime.isEmpty()) {
            JLabel bestTimeLabel = new JLabel("💡 " + bestTime);
            bestTimeLabel.setFont(new Font("SansSerif", Font.ITALIC, 10));
            bestTimeLabel.setForeground(new Color(34, 139, 34));
            detailsPanel.add(bestTimeLabel);
        }

        String crowdMessage = itineraryGenerator.getCrowdLevelMessage(spot, date);
        if (!crowdMessage.isEmpty()) {
            JLabel crowdLabel = new JLabel("👥 " + crowdMessage);
            crowdLabel.setFont(new Font("SansSerif", Font.ITALIC, 9));
            crowdLabel.setForeground(new Color(255, 140, 0));
            detailsPanel.add(crowdLabel);
        }

        if (travelInfo != null) {
            JLabel travelLabel = new JLabel("🚗 Travel: " + travelInfo.getBestOption());
            travelLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
            travelLabel.setForeground(new Color(70, 130, 180));
            detailsPanel.add(travelLabel);
        }
        
        detailsPanel.add(nameLabel);
        detailsPanel.add(Box.createVerticalStrut(3));
        detailsPanel.add(categoryLabel);
        detailsPanel.add(durationLabel);

        JLabel priceLabel = new JLabel(spot.getPrice() == 0 ? "Free" : "₹" + (int)spot.getPrice() + " per person");
        priceLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        priceLabel.setForeground(new Color(34, 139, 34));
        priceLabel.setPreferredSize(new Dimension(150, 0));
        
        slotPanel.add(timeLabel, BorderLayout.WEST);
        slotPanel.add(detailsPanel, BorderLayout.CENTER);
        slotPanel.add(priceLabel, BorderLayout.EAST);
        
        return slotPanel;
    }
    
    private String formatTimeSlot(java.time.LocalTime startTime, java.time.LocalTime endTime) {
        return startTime.format(DateTimeFormatter.ofPattern("h:mm a")) + " - " + 
               endTime.format(DateTimeFormatter.ofPattern("h:mm a"));
    }
    
    private String getBestTimeMessage(TouristSpot spot) {
        if (spot.getBestVisitingTime() != null && !spot.getBestVisitingTime().isEmpty()) {
            return "Best time: " + spot.getBestVisitingTime();
        }

        String category = spot.getCategory();
        if (category.equals("Nature") || category.equals("Historical")) {
            return "Best visited in early morning for cooler weather";
        } else if (category.equals("Shopping")) {
            return "Evening recommended for better shopping experience";
        } else if (category.equals("Religious")) {
            return "Morning hours are ideal for peaceful visit";
        }
        return "";
    }
    
    /**
     * Create time-based recommendations for the day
     * Different recommendations for each day based on day number, weather, and scheduled spots
     */
    private JPanel createTimeBasedRecommendations(LocalDate date, 
                                                  Map<TouristSpot, TimeSlot> schedule, 
                                                  List<TouristSpot> spots,
                                                  int dayNumber) {
        JPanel recommendationsPanel = new JPanel();
        recommendationsPanel.setLayout(new BoxLayout(recommendationsPanel, BoxLayout.Y_AXIS));
        recommendationsPanel.setBackground(new Color(255, 250, 240));
        recommendationsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 200, 100), 1),
                new EmptyBorder(15, 15, 15, 15)));
        
        String cityName = getCityNameFromCode(destinationCode);
        WeatherForecast weather = weatherService.getWeatherForecast(destinationCode, date);

        boolean hasMorning = false, hasAfternoon = false, hasEvening = false;
        for (TouristSpot spot : spots) {
            TimeSlot slot = schedule.get(spot);
            if (slot != null) {
                int hour = slot.getStartTime().getHour();
                if (hour >= 9 && hour < 12) hasMorning = true;
                else if (hour >= 12 && hour < 16) hasAfternoon = true;
                else if (hour >= 16) hasEvening = true;
            }
        }
        
        String dayName = date.format(DateTimeFormatter.ofPattern("EEEE"));
        JLabel titleLabel = new JLabel("💡 Recommended Activities for " + dayName + ":");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLabel.setForeground(new Color(139, 69, 19));
        recommendationsPanel.add(titleLabel);
        recommendationsPanel.add(Box.createVerticalStrut(10));

        List<String> recommendations = getUniqueDayRecommendations(
            dayNumber, numberOfDays, date, cityName, weather, 
            hasMorning, hasAfternoon, hasEvening, spots);

        int count = 0;
        for (String rec : recommendations) {
            if (count >= 4) break;
            JLabel recLabel = new JLabel("• " + rec);
            recLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
            recLabel.setForeground(new Color(100, 100, 100));
            recommendationsPanel.add(recLabel);
            recommendationsPanel.add(Box.createVerticalStrut(5));
            count++;
        }
        
        return recommendationsPanel;
    }
    
    /**
     * Get unique recommendations for each day based on day number and context
     */
    private List<String> getUniqueDayRecommendations(int dayNumber, int totalDays, LocalDate date,
                                                    String cityName, WeatherForecast weather,
                                                    boolean hasMorning, boolean hasAfternoon, 
                                                    boolean hasEvening, List<TouristSpot> spots) {
        List<String> recommendations = new ArrayList<>();
        boolean isFirstDay = (dayNumber == 1);
        boolean isLastDay = (dayNumber == totalDays);
        boolean isWeekend = date.getDayOfWeek().getValue() >= 5;

        if (isFirstDay) {
            recommendations.add("Get familiar with the area around your hotel");
            recommendations.add("Visit nearby local markets for essentials and souvenirs");
            if (!hasEvening) {
                recommendations.add("Enjoy a relaxed dinner at a local restaurant");
            }
            recommendations.add("Take a short walk to explore the neighborhood");
            recommendations.add("Rest and prepare for the days ahead");
        }

        else if (isLastDay) {
            recommendations.add("Complete any last-minute shopping for souvenirs");
            recommendations.add("Visit a favorite spot one more time for photos");
            if (!hasMorning) {
                recommendations.add("Enjoy a final breakfast at a local favorite spot");
            }
            recommendations.add("Pack and prepare for departure");
            recommendations.add("Try one last local specialty before leaving");
        }

        else {

            if (dayNumber == 2) {
                recommendations.add("Explore cultural and heritage sites in depth");
                recommendations.add("Visit local museums or art galleries");
                recommendations.add("Attend a cultural show or traditional performance");
                recommendations.add("Try authentic " + cityName + " cuisine at recommended restaurants");
            }

            else if (dayNumber == 3) {
                recommendations.add("Try adventure activities or outdoor experiences");
                recommendations.add("Visit nature spots, parks, or scenic viewpoints");
                recommendations.add("Experience local festivals or events if available");
                recommendations.add("Explore off-the-beaten-path locations");
            }

            else {
                recommendations.add("Take a more relaxed pace - enjoy local cafes");
                recommendations.add("Interact with locals and learn about daily life");
                recommendations.add("Visit local neighborhoods away from tourist areas");
                recommendations.add("Try street food and local delicacies");
            }
        }

        if (!hasMorning && !isLastDay) {
            recommendations.add("Early morning photography session (best light)");
        }
        if (!hasAfternoon) {
            if (weather.getTemperature() > 30) {
                recommendations.add("Relax at air-conditioned cafes or shopping malls");
            } else {
                recommendations.add("Explore local markets and shopping areas");
            }
        }
        if (!hasEvening && !isFirstDay) {
            recommendations.add("Experience " + cityName + " nightlife or evening entertainment");
        }

        if (isWeekend) {
            recommendations.add("Visit weekend markets or flea markets (if available)");
            recommendations.add("Attend local events or festivals happening this weekend");
        }

        if (weather.getCondition().equals("Rainy")) {
            recommendations.add("Visit indoor attractions: museums, galleries, or malls");
        } else if (weather.getCondition().equals("Clear") && weather.getTemperature() < 25) {
            recommendations.add("Perfect weather for outdoor activities and sightseeing");
        }

        while (recommendations.size() < 3) {
            recommendations.add("Explore local culture and traditions");
            recommendations.add("Try local cuisine and street food");
            recommendations.add("Take leisurely walks to discover hidden gems");
        }

        Collections.shuffle(recommendations);
        return recommendations.subList(0, Math.min(4, recommendations.size()));
    }
    
    /**
     * Calculate total duration for a day's spots including travel time
     */
    private int calculateDayDuration(List<TouristSpot> daySpots) {
        if (daySpots.isEmpty()) {
            return 0;
        }
        
        double totalDuration = daySpots.get(0).getEstimatedDuration();
        for (int i = 1; i < daySpots.size(); i++) {
            TouristSpot prev = daySpots.get(i - 1);
            TouristSpot curr = daySpots.get(i);
            int travelTime = itineraryGenerator.calculateTravelTimeMinutes(prev, curr);
            totalDuration += curr.getEstimatedDuration() + (travelTime / 60.0);
        }
        
        return (int) Math.ceil(totalDuration);
    }
    
    /**
     * Get recommended spots for extra time in a day
     * Only adds spots if there's available time, and keeps all selected spots
     */
    private List<TouristSpot> getRecommendedSpotsForExtraTime(String destinationCode, 
                                                              Set<Integer> alreadySelectedIds,
                                                              List<TouristSpot> existingDaySpots,
                                                              int availableHours) {
        List<TouristSpot> recommended = new ArrayList<>();

        List<TouristSpot> allSpots = touristSpotService.getTouristSpotsByDestination(destinationCode);

        List<TouristSpot> availableSpots = allSpots.stream()
                .filter(spot -> !alreadySelectedIds.contains(spot.getId()))
                .filter(spot -> !existingDaySpots.contains(spot)) // Not already in this day
                .collect(java.util.stream.Collectors.toList());

        availableSpots.sort(Comparator.comparingDouble(TouristSpot::getDistanceFromCityCenter));

        int usedHours = 0;
        Set<String> categories = new HashSet<>();
        for (TouristSpot spot : existingDaySpots) {
            categories.add(spot.getCategory());
        }
        
        for (TouristSpot spot : availableSpots) {
            if (usedHours >= availableHours - 1) {
                break; // Stop if we've used most of available time
            }
            
            double spotDuration = spot.getEstimatedDuration();
            int travelTime = existingDaySpots.isEmpty() ? 0 : 1; // Estimate 1 hour travel
            
            if (usedHours + (int) Math.ceil(spotDuration) + travelTime <= availableHours) {

                if (!categories.contains(spot.getCategory()) || recommended.size() < 2) {
                    recommended.add(spot);
                    usedHours += spotDuration + travelTime;
                    categories.add(spot.getCategory());
                    
                    if (recommended.size() >= 3) {
                        break; // Max 3 recommendations per day
                    }
                }
            }
        }
        
        return recommended;
    }
    
    /**
     * Get recommended spots for a day with fewer activities
     */
    private List<TouristSpot> getRecommendedSpotsForDay(String destinationCode, 
                                                        Set<Integer> alreadySelectedIds,
                                                        List<TouristSpot> existingDaySpots,
                                                        int dayNumber) {
        List<TouristSpot> recommended = new ArrayList<>();

        List<TouristSpot> allSpots = touristSpotService.getTouristSpotsByDestination(destinationCode);

        List<TouristSpot> availableSpots = allSpots.stream()
                .filter(spot -> !alreadySelectedIds.contains(spot.getId()))
                .collect(java.util.stream.Collectors.toList());

        if (existingDaySpots.isEmpty()) {

            availableSpots.sort(Comparator.comparingDouble(TouristSpot::getDistanceFromCityCenter));

            Set<String> categories = new HashSet<>();
            for (TouristSpot spot : availableSpots) {
                if (!categories.contains(spot.getCategory()) && recommended.size() < 3) {
                    recommended.add(spot);
                    categories.add(spot.getCategory());
                    alreadySelectedIds.add(spot.getId()); // Mark as used
                }
            }
        } else if (existingDaySpots.size() == 1) {

            TouristSpot existingSpot = existingDaySpots.get(0);
            String existingCategory = existingSpot.getCategory();

            for (TouristSpot spot : availableSpots) {
                if (!spot.getCategory().equals(existingCategory) && recommended.size() < 2) {
                    recommended.add(spot);
                    alreadySelectedIds.add(spot.getId());
                }
            }
        }
        
        return recommended;
    }
    
    /**
     * Round LocalTime to nearest 15-minute interval (00, 15, 30, 45)
     */
    private LocalTime roundTimeTo15Minutes(LocalTime time) {
        int minutes = time.getMinute();
        int roundedMinutes = ((minutes + 7) / 15) * 15; // Round to nearest 15
        
        if (roundedMinutes >= 60) {
            return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        } else {
            return time.withMinute(roundedMinutes).withSecond(0).withNano(0);
        }
    }
    
}