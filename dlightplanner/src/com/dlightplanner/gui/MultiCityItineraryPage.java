package com.dlightplanner.gui;

import com.dlightplanner.models.*;
import com.dlightplanner.models.MultiCityTripRequest.CityLeg;
import com.dlightplanner.models.MultiCityTripRequest.FlightLeg;
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
import java.util.stream.Collectors;

public class MultiCityItineraryPage extends JFrame {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final int MAX_HOURS_PER_DAY = 10; // Maximum hours per day (9 AM to 7 PM)
    
    private final MultiCityTripRequest request;
    private final Map<FlightLeg, Flight> selectedFlights;
    private final Map<String, Hotel> selectedHotels;
    private final Map<String, List<TouristSpot>> selectedTouristSpots;
    
    private final AirportService airportService;
    private final ItineraryGenerator itineraryGenerator;
    private final WeatherService weatherService;
    private final TouristSpotService touristSpotService;
    private JPanel itineraryPanel;
    private JFrame previousFrame;
    
    public MultiCityItineraryPage(MultiCityTripRequest request,
                                 Map<FlightLeg, Flight> selectedFlights,
                                 Map<String, Hotel> selectedHotels,
                                 Map<String, List<TouristSpot>> selectedTouristSpots,
                                 JFrame previousFrame) {
        this.request = request;
        this.selectedFlights = selectedFlights;
        this.selectedHotels = selectedHotels;
        this.selectedTouristSpots = selectedTouristSpots;
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
        
        setTitle("Voya | Multi-City Smart Itinerary");
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
    
    private String getCityName(String code) {
        return airportService.getAirports().stream()
                .filter(airport -> airport.getIata().equalsIgnoreCase(code))
                .map(Airport::getCity)
                .findFirst()
                .orElse(code);
    }
    
    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(10, 32, 66));
        header.setBorder(new EmptyBorder(20, 25, 20, 25));
        
        JLabel title = new JLabel("Multi-City Smart Itinerary");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Serif", Font.BOLD, 26));
        
        StringBuilder route = new StringBuilder();
        route.append(getCityName(request.getOriginCode()));
        for (CityLeg city : request.getCityLegs()) {
            route.append(" → ").append(getCityName(city.getDestinationCode()));
        }
        
        JLabel subTitle = new JLabel(route.toString());
        subTitle.setForeground(new Color(210, 225, 240));
        subTitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(subTitle, BorderLayout.SOUTH);
        
        header.add(titlePanel, BorderLayout.WEST);
        return header;
    }
    
    private JPanel buildContentPanel() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(new EmptyBorder(15, 25, 10, 25));
        
        itineraryPanel = new JPanel();
        itineraryPanel.setLayout(new BoxLayout(itineraryPanel, BoxLayout.Y_AXIS));
        itineraryPanel.setOpaque(false);
        itineraryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JScrollPane scrollPane = new JScrollPane(itineraryPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(new Color(245, 245, 250));
        scrollPane.getViewport().setPreferredSize(new Dimension(0, 0));
        
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
        
        JButton summaryButton = new JButton("View Booking Summary");
        summaryButton.setBackground(new Color(0, 123, 255));
        summaryButton.setForeground(Color.WHITE);
        summaryButton.addActionListener(e -> {
            new BookingSummaryPage(request, selectedFlights, selectedHotels, selectedTouristSpots, this);
        });
        
        footer.add(backButton);
        footer.add(summaryButton);
        
        return footer;
    }
    
    private void generateItinerary() {
        itineraryPanel.removeAll();
        
        List<CityLeg> cities = request.getCityLegs();
        
        for (int cityIndex = 0; cityIndex < cities.size(); cityIndex++) {
            CityLeg city = cities.get(cityIndex);
            String destCode = city.getDestinationCode();
            String originCode = request.getOriginCode();
            boolean isLastCity = (cityIndex == cities.size() - 1);

            if (isLastCity && destCode.equalsIgnoreCase(originCode)) {
                continue;
            }
            
            Hotel hotel = selectedHotels.get(destCode);
            List<TouristSpot> spots = selectedTouristSpots.getOrDefault(destCode, new ArrayList<>());

            JPanel cityHeader = createCityHeader(cityIndex + 1, city, hotel);
            itineraryPanel.add(cityHeader);
            itineraryPanel.add(Box.createVerticalStrut(15));

            int numberOfDays = (int) ChronoUnit.DAYS.between(city.getArrivalDate(), city.getDepartureDate());
            if (numberOfDays < 1) numberOfDays = 1;
            
            if (spots.isEmpty()) {
                JLabel noSpotsLabel = new JLabel("<html><center><h3>No Tourist Spots Selected for " + 
                    getCityName(destCode) + "</h3>" +
                    "<p>You can still enjoy your stay at " + (hotel != null ? hotel.getName() : "your hotel") + "</p>" +
                    "<p>Explore the city at your own pace!</p></center></html>");
                noSpotsLabel.setHorizontalAlignment(SwingConstants.CENTER);
                noSpotsLabel.setBorder(new EmptyBorder(50, 0, 50, 0));
                itineraryPanel.add(noSpotsLabel);
            } else {

                Map<LocalDate, List<TouristSpot>> dayWiseSpots = itineraryGenerator.groupSpotsByProximity(
                        spots, hotel, city.getArrivalDate(), numberOfDays);

                Set<Integer> selectedSpotIds = spots.stream()
                        .map(TouristSpot::getId)
                        .collect(Collectors.toSet());

                for (TouristSpot spot : spots) {

                    boolean found = false;
                    for (List<TouristSpot> daySpots : dayWiseSpots.values()) {
                        for (TouristSpot daySpot : daySpots) {
                            if (daySpot.getId() == spot.getId()) {
                                found = true;
                                break;
                            }
                        }
                        if (found) break;
                    }
                    
                    if (!found) {

                        LocalDate bestDay = city.getArrivalDate();
                        int minCount = Integer.MAX_VALUE;
                        for (int d = 0; d < numberOfDays; d++) {
                            LocalDate dDate = city.getArrivalDate().plusDays(d);
                            List<TouristSpot> existingDaySpots = dayWiseSpots.getOrDefault(dDate, new ArrayList<>());
                            int count = existingDaySpots.size();
                            if (count < minCount) {
                                minCount = count;
                                bestDay = dDate;
                            }
                        }
                        if (!dayWiseSpots.containsKey(bestDay)) {
                            dayWiseSpots.put(bestDay, new ArrayList<>());
                        }
                        dayWiseSpots.get(bestDay).add(spot);
                    }
                }


                int totalDistributedSpots = 0;
                for (List<TouristSpot> daySpots : dayWiseSpots.values()) {
                    totalDistributedSpots += daySpots.size();
                }

                boolean needsRedistribution = false;
                if (totalDistributedSpots > numberOfDays) {
                    for (int d = 0; d < numberOfDays; d++) {
                        LocalDate dDate = city.getArrivalDate().plusDays(d);
                        List<TouristSpot> daySpots = dayWiseSpots.getOrDefault(dDate, new ArrayList<>());
                        if (daySpots.size() > (totalDistributedSpots * 0.8)) {
                            needsRedistribution = true;
                            break;
                        }
                    }
                }

                if (needsRedistribution && totalDistributedSpots > 0) {

                    List<TouristSpot> allSpotsList = new ArrayList<>();
                    for (int d = 0; d < numberOfDays; d++) {
                        LocalDate dDate = city.getArrivalDate().plusDays(d);
                        List<TouristSpot> daySpots = dayWiseSpots.getOrDefault(dDate, new ArrayList<>());
                        allSpotsList.addAll(daySpots);
                    }

                    for (int d = 0; d < numberOfDays; d++) {
                        LocalDate dDate = city.getArrivalDate().plusDays(d);
                        dayWiseSpots.put(dDate, new ArrayList<>());
                    }

                    int spotIndex = 0;
                    for (TouristSpot spot : allSpotsList) {
                        int dayIndex = spotIndex % numberOfDays;
                        LocalDate dayDate = city.getArrivalDate().plusDays(dayIndex);
                        dayWiseSpots.get(dayDate).add(spot);
                        spotIndex++;
                    }
                }

                for (int d = 0; d < numberOfDays; d++) {
                    LocalDate dDate = city.getArrivalDate().plusDays(d);
                    if (!dayWiseSpots.containsKey(dDate)) {
                        dayWiseSpots.put(dDate, new ArrayList<>());
                    }
                }

                for (int day = 0; day < numberOfDays; day++) {
                    LocalDate currentDate = city.getArrivalDate().plusDays(day);


                    List<TouristSpot> daySpots = new ArrayList<>(dayWiseSpots.get(currentDate));

                    if (daySpots.size() < 4 && daySpots.size() > 0) {
                        int dayDuration = calculateDayDuration(daySpots);
                        int availableHours = MAX_HOURS_PER_DAY - dayDuration;
                        if (availableHours > 0) {
                            List<TouristSpot> recommendedSpots = getRecommendedSpotsForExtraTime(
                                    destCode, selectedSpotIds, daySpots, availableHours);
                            int spotsNeeded = Math.min(4 - daySpots.size(), recommendedSpots.size());
                            if (spotsNeeded > 0 && !recommendedSpots.isEmpty()) {
                                daySpots.addAll(recommendedSpots.subList(0, spotsNeeded));
                            }
                        }
                    }

                    List<TouristSpot> optimizedRoute = itineraryGenerator.optimizeRoute(daySpots, hotel);

                    itineraryPanel.add(createDayCard(cityIndex + 1, day + 1, currentDate, optimizedRoute, hotel, destCode, numberOfDays));
                    itineraryPanel.add(Box.createVerticalStrut(20));
                }
            }

            if (cityIndex < cities.size() - 1) {
                JSeparator separator = new JSeparator();
                separator.setPreferredSize(new Dimension(Integer.MAX_VALUE, 2));
                itineraryPanel.add(separator);
                itineraryPanel.add(Box.createVerticalStrut(30));
            }
        }
        
        itineraryPanel.revalidate();
        itineraryPanel.repaint();
    }
    
    private JPanel createCityHeader(int cityNumber, CityLeg city, Hotel hotel) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(240, 248, 255));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(10, 32, 66), 2),
                new EmptyBorder(15, 20, 15, 20)));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel cityLabel = new JLabel(String.format("City %d: %s", cityNumber, getCityName(city.getDestinationCode())));
        cityLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        cityLabel.setForeground(new Color(10, 32, 66));
        
        JLabel dateLabel = new JLabel(String.format("%s to %s", 
            city.getArrivalDate().format(DATE_FORMAT),
            city.getDepartureDate().format(DATE_FORMAT)));
        dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        dateLabel.setForeground(new Color(100, 100, 100));
        
        if (hotel != null) {
            JLabel hotelLabel = new JLabel("🏨 " + hotel.getName() + " (" + hotel.getStarRating() + "★)");
            hotelLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            hotelLabel.setForeground(new Color(100, 100, 100));
            
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);
            infoPanel.add(dateLabel);
            infoPanel.add(Box.createVerticalStrut(5));
            infoPanel.add(hotelLabel);
            
            header.add(cityLabel, BorderLayout.WEST);
            header.add(infoPanel, BorderLayout.EAST);
        } else {
            header.add(cityLabel, BorderLayout.WEST);
            header.add(dateLabel, BorderLayout.EAST);
        }
        
        return header;
    }
    
    private JPanel createDayCard(int cityNumber, int dayNumber, LocalDate date, List<TouristSpot> spots, Hotel hotel, String destinationCode, int numberOfDays) {
        JPanel dayCard = new JPanel();
        dayCard.setLayout(new BoxLayout(dayCard, BoxLayout.Y_AXIS));
        dayCard.setBackground(Color.WHITE);
        dayCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(10, 32, 66), 2),
                new EmptyBorder(20, 20, 20, 20)));
        dayCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        dayCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        String dayName = date.getDayOfWeek().toString().substring(0, 1) + 
                        date.getDayOfWeek().toString().substring(1).toLowerCase();
        JLabel dayLabel = new JLabel(dayName + ", " + date.format(DATE_FORMAT));
        dayLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        dayLabel.setForeground(new Color(10, 32, 66));
        
        headerPanel.add(dayLabel, BorderLayout.WEST);
        
        if (spots.isEmpty()) {

            dayCard.add(headerPanel);
            dayCard.add(Box.createVerticalStrut(15));

            WeatherForecast dayWeather = weatherService.getWeatherForecast(destinationCode, date);

            JPanel weatherPanel = createWeatherPanel(dayWeather);
            dayCard.add(weatherPanel);
            dayCard.add(Box.createVerticalStrut(10));
            
            JLabel freeDayLabel = new JLabel("Free day - Explore at your own pace or relax at the hotel");
            freeDayLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
            freeDayLabel.setForeground(new Color(150, 150, 150));
            freeDayLabel.setBorder(new EmptyBorder(10, 0, 10, 0));
            dayCard.add(freeDayLabel);

            Map<TouristSpot, TimeSlot> emptySchedule = new HashMap<>();
            JPanel recommendationsPanel = createTimeBasedRecommendations(date, emptySchedule, spots, dayNumber, destinationCode, numberOfDays);
            dayCard.add(Box.createVerticalStrut(10));
            dayCard.add(recommendationsPanel);
            
            return dayCard;
        }

        Map<TouristSpot, TimeSlot> schedule = itineraryGenerator.scheduleTimeSlots(
                spots, hotel, numberOfDays, spots.size());

        double totalDuration = 0;
        for (TouristSpot spot : spots) {
            TimeSlot slot = schedule.get(spot);
            if (slot != null) {
                long minutes = ChronoUnit.MINUTES.between(slot.getStartTime(), slot.getEndTime());
                totalDuration += minutes / 60.0;
            }
        }
        JLabel durationLabel = new JLabel(String.format("Total: %.1f hours (%d spots)", totalDuration, spots.size()));
        durationLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        durationLabel.setForeground(new Color(100, 100, 100));
        headerPanel.add(durationLabel, BorderLayout.EAST);

        dayCard.add(headerPanel);
        dayCard.add(Box.createVerticalStrut(15));

        WeatherForecast dayWeather = weatherService.getWeatherForecast(destinationCode, date);

        JPanel weatherPanel = createWeatherPanel(dayWeather);
        dayCard.add(weatherPanel);
        dayCard.add(Box.createVerticalStrut(10));

        List<TouristSpot> sortedSpots = new ArrayList<>(spots);
        sortedSpots.sort(Comparator.comparing(spot -> {
            TimeSlot slot = schedule.get(spot);
            return slot != null ? slot.getStartTime() : LocalTime.of(23, 59);
        }));

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

        JPanel recommendationsPanel = createTimeBasedRecommendations(date, schedule, spots, dayNumber, destinationCode, numberOfDays);
        dayCard.add(recommendationsPanel);
        
        return dayCard;
    }
    
    private JPanel createWeatherPanel(WeatherForecast weather) {
        JPanel weatherPanel = new JPanel(new BorderLayout());
        weatherPanel.setBackground(new Color(230, 245, 255));
        weatherPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 150, 220), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        weatherPanel.setOpaque(true);
        weatherPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        weatherPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));
        weatherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        weatherPanel.setMinimumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        if (weather != null) {
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
            weatherLabel.setHorizontalAlignment(SwingConstants.LEFT);
            weatherPanel.add(weatherLabel, BorderLayout.WEST);
        } else {
            JLabel weatherLabel = new JLabel("Weather information not available");
            weatherLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            weatherLabel.setForeground(new Color(100, 100, 100));
            weatherLabel.setHorizontalAlignment(SwingConstants.LEFT);
            weatherPanel.add(weatherLabel, BorderLayout.WEST);
        }
        
        return weatherPanel;
    }
    
    private JPanel createSpotTimeSlot(TouristSpot spot, TimeSlot slot, LocalDate date, TravelInfo travelInfo) {
        JPanel slotPanel = new JPanel(new BorderLayout(15, 0));
        slotPanel.setBackground(new Color(245, 250, 255));
        slotPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 220, 240), 1),
                new EmptyBorder(12, 12, 12, 12)));

        String timeSlotStr = formatTimeSlot(slot.getStartTime(), slot.getEndTime());
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

        long totalMinutes = ChronoUnit.MINUTES.between(slot.getStartTime(), slot.getEndTime());
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
    
    private String formatTimeSlot(LocalTime startTime, LocalTime endTime) {
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
                                                  int dayNumber,
                                                  String destinationCode,
                                                  int numberOfDays) {
        JPanel recommendationsPanel = new JPanel();
        recommendationsPanel.setLayout(new BoxLayout(recommendationsPanel, BoxLayout.Y_AXIS));
        recommendationsPanel.setBackground(new Color(255, 250, 240));
        recommendationsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 200, 100), 1),
                new EmptyBorder(15, 15, 15, 15)));
        
        String cityName = getCityName(destinationCode);
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
    
    private List<TouristSpot> getRecommendedSpotsForExtraTime(String destinationCode, 
                                                              Set<Integer> alreadySelectedIds,
                                                              List<TouristSpot> existingDaySpots,
                                                              int availableHours) {
        List<TouristSpot> recommended = new ArrayList<>();
        
        List<TouristSpot> allSpots = touristSpotService.getTouristSpotsByDestination(destinationCode);
        
        List<TouristSpot> availableSpots = allSpots.stream()
                .filter(spot -> !alreadySelectedIds.contains(spot.getId()))
                .filter(spot -> !existingDaySpots.contains(spot))
                .collect(Collectors.toList());
        
        availableSpots.sort(Comparator.comparingDouble(TouristSpot::getDistanceFromCityCenter));
        
        int usedHours = 0;
        for (TouristSpot spot : availableSpots) {
            if (usedHours >= availableHours - 1) {
                break;
            }
            
            double spotDuration = spot.getEstimatedDuration();
            int travelTime = existingDaySpots.isEmpty() ? 0 : 1;
            
            if (usedHours + (int) Math.ceil(spotDuration) + travelTime <= availableHours) {
                recommended.add(spot);
                usedHours += (int) Math.ceil(spotDuration) + travelTime;
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

