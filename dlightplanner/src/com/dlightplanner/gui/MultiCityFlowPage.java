package com.dlightplanner.gui;

import com.dlightplanner.models.*;
import com.dlightplanner.models.MultiCityTripRequest.CityLeg;
import com.dlightplanner.models.MultiCityTripRequest.FlightLeg;
import com.dlightplanner.services.AirportService;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiCityFlowPage extends JFrame {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
    private static final DateTimeFormatter DATE_FORMAT_SIMPLE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    
    private final MultiCityTripRequest request;
    private final AirportService airportService = new AirportService();
    
    private Map<FlightLeg, Flight> selectedFlights = new HashMap<>();
    private Map<String, Hotel> selectedHotels = new HashMap<>();
    private Map<String, List<TouristSpot>> selectedTouristSpots = new HashMap<>();
    
    private int currentFlightLegIndex = 0;
    private int currentCityIndex = 0;
    private FlightLeg currentFlightLeg;
    
    private JFrame previousFrame;
    
    public FlightLeg getCurrentFlightLeg() {
        return currentFlightLeg;
    }
    
    public int getCurrentFlightLegIndex() {
        return currentFlightLegIndex;
    }
    
    public Map<FlightLeg, Flight> getSelectedFlights() {
        return selectedFlights;
    }
    
    public MultiCityTripRequest getRequest() {
        return request;
    }
    
    public MultiCityFlowPage(MultiCityTripRequest request, JFrame previousFrame) {
        this.request = request;
        this.previousFrame = previousFrame;
        
        setTitle("Voya | Multi-City Trip Planning");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(1400, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        if (previousFrame != null) {
            previousFrame.setVisible(false);
        }
        
        airportService.loadAirportsFromJson("resources/airports.json");
        
        showNextFlightSelection();
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
    
    private void showNextFlightSelection() {
        List<FlightLeg> legs = request.getFlightLegs();
        
        if (currentFlightLegIndex >= legs.size()) {
            currentCityIndex = 0;
            showNextHotelSelection();
            return;
        }
        
        FlightLeg leg = legs.get(currentFlightLegIndex);
        
        FlightSearchRequest flightRequest = new FlightSearchRequest(
            leg.getFromCode(),
            leg.getToCode(),
            leg.getDate(),
            null,
            false,
            request.getAdults(),
            request.getChildren(),
            request.getInfants()
        );
        
        setVisible(false);
        currentFlightLeg = leg;
        new FlightPage(flightRequest, this);
    }
    
    public void onFlightSelected(FlightLeg leg, Flight selectedFlight) {
        selectedFlights.put(leg, selectedFlight);
        currentFlightLegIndex++;
        
        List<FlightLeg> allLegs = request.getFlightLegs();
        if (currentFlightLegIndex >= allLegs.size()) {
            showFlightBreakdown();
        } else {
            showNextFlightSelection();
        }
    }
    
    private void showFlightBreakdown() {
        StringBuilder breakdown = new StringBuilder();
        breakdown.append("=== FLIGHT BREAKDOWN ===\n\n");
        
        double totalFlightCost = 0;
        List<FlightLeg> legs = request.getFlightLegs();
        
        for (int i = 0; i < legs.size(); i++) {
            FlightLeg leg = legs.get(i);
            Flight flight = findFlightForLeg(leg);
            
            if (flight != null) {
                breakdown.append(String.format("Flight %d: %s → %s\n", i + 1, 
                    getCityName(leg.getFromCode()), getCityName(leg.getToCode())));
                breakdown.append(String.format("  %s %s\n", flight.getAirline(), flight.getFlightNumber()));
                breakdown.append(String.format("  %s → %s\n", flight.getDepartureTime(), flight.getArrivalTime()));
                breakdown.append(String.format("  Date: %s\n", leg.getDate().format(DATE_FORMAT)));
                
                double adultFare = flight.getFare();
                double childFare = adultFare * 0.75;
                double infantFare = adultFare * 0.10;
                
                int adults = request.getAdults();
                int children = request.getChildren();
                int infants = request.getInfants();
                
                double legTotal = 0;
                if (adults > 0) {
                    legTotal += adultFare * adults;
                    breakdown.append(String.format("  Adults (%d × ₹%,.0f): ₹%,.0f\n", adults, adultFare, adultFare * adults));
                }
                if (children > 0) {
                    legTotal += childFare * children;
                    breakdown.append(String.format("  Children (%d × ₹%,.0f): ₹%,.0f\n", children, childFare, childFare * children));
                }
                if (infants > 0) {
                    legTotal += infantFare * infants;
                    breakdown.append(String.format("  Infants (%d × ₹%,.0f): ₹%,.0f\n", infants, infantFare, infantFare * infants));
                }
                breakdown.append(String.format("  Leg Total: ₹%,.0f\n\n", legTotal));
                totalFlightCost += legTotal;
            }
        }
        
        breakdown.append("═══════════════════════════\n");
        breakdown.append(String.format("TOTAL FLIGHT COST: ₹%,.0f", totalFlightCost));
        
        JOptionPane.showMessageDialog(null, breakdown.toString(), "Flight Breakdown", JOptionPane.INFORMATION_MESSAGE);
        
        currentCityIndex = 0;
        showNextHotelSelection();
    }
    
    private void showNextHotelSelection() {
        List<CityLeg> cities = request.getCityLegs();
        
        if (currentCityIndex >= cities.size()) {
            currentCityIndex = 0;
            showNextTouristSpotSelection();
            return;
        }
        
        CityLeg city = cities.get(currentCityIndex);
        String destCode = city.getDestinationCode();
        String originCode = request.getOriginCode();
        
        boolean isLastCity = (currentCityIndex == cities.size() - 1);
        if (isLastCity && destCode.equalsIgnoreCase(originCode)) {
            currentCityIndex++;
            showNextHotelSelection();
            return;
        }
        
        setVisible(false);
        new HotelPage(city.getDestinationCode(), city.getArrivalDate(), city.getDepartureDate(), this);
    }
    
    public void onHotelSelected(String destinationCode, Hotel hotel) {
        selectedHotels.put(destinationCode, hotel);
        currentCityIndex++;
        
        List<CityLeg> cities = request.getCityLegs();
        boolean allHotelsSelected = true;
        for (CityLeg city : cities) {
            String destCode = city.getDestinationCode();
            String originCode = request.getOriginCode();
            boolean isLastCity = (cities.indexOf(city) == cities.size() - 1);
            
            if (!(isLastCity && destCode.equalsIgnoreCase(originCode))) {
                if (!selectedHotels.containsKey(destCode)) {
                    allHotelsSelected = false;
                    break;
                }
            }
        }
        
        if (allHotelsSelected) {
            showHotelBreakdown();
        } else {
            showNextHotelSelection();
        }
    }
    
    private void showHotelBreakdown() {
        StringBuilder breakdown = new StringBuilder();
        breakdown.append("=== HOTEL BREAKDOWN ===\n\n");
        
        double totalHotelCost = 0;
        List<CityLeg> cities = request.getCityLegs();
        
        for (int i = 0; i < cities.size(); i++) {
            CityLeg city = cities.get(i);
            String destCode = city.getDestinationCode();
            String originCode = request.getOriginCode();
            boolean isLastCity = (i == cities.size() - 1);
            
            if (isLastCity && destCode.equalsIgnoreCase(originCode)) {
                continue;
            }
            
            Hotel hotel = selectedHotels.get(destCode);
            if (hotel != null) {
                long nights = java.time.temporal.ChronoUnit.DAYS.between(city.getArrivalDate(), city.getDepartureDate());
                if (nights < 1) nights = 1;
                
                double pricePerNight = hotel.getPricePerNight();
                double hotelTotal = pricePerNight * nights;
                totalHotelCost += hotelTotal;
                
                breakdown.append(String.format("Hotel %d: %s\n", i + 1, getCityName(destCode)));
                breakdown.append(String.format("  Hotel: %s\n", hotel.getName()));
                breakdown.append(String.format("  Check-in: %s\n", city.getArrivalDate().format(DATE_FORMAT_SIMPLE)));
                breakdown.append(String.format("  Check-out: %s\n", city.getDepartureDate().format(DATE_FORMAT_SIMPLE)));
                breakdown.append(String.format("  Nights: %d\n", nights));
                breakdown.append(String.format("  Price per night: ₹%,.0f\n", pricePerNight));
                breakdown.append(String.format("  Hotel Total: ₹%,.0f\n\n", hotelTotal));
            }
        }
        
        breakdown.append("═══════════════════════════\n");
        breakdown.append(String.format("TOTAL HOTEL COST: ₹%,.0f", totalHotelCost));
        
        JOptionPane.showMessageDialog(null, breakdown.toString(), "Hotel Breakdown", JOptionPane.INFORMATION_MESSAGE);
        
        currentCityIndex = 0;
        showNextTouristSpotSelection();
    }
    
    private void showNextTouristSpotSelection() {
        List<CityLeg> cities = request.getCityLegs();
        
        if (currentCityIndex >= cities.size()) {
            showFinalItinerary();
            return;
        }
        
        CityLeg city = cities.get(currentCityIndex);
        String destCode = city.getDestinationCode();
        String originCode = request.getOriginCode();
        
        boolean isLastCity = (currentCityIndex == cities.size() - 1);
        if (isLastCity && destCode.equalsIgnoreCase(originCode)) {
            currentCityIndex++;
            showNextTouristSpotSelection();
            return;
        }
        
        Hotel hotel = selectedHotels.get(destCode);
        
        setVisible(false);
        new TouristSpotsPage(city.getDestinationCode(), city.getArrivalDate(), city.getDepartureDate(), hotel, this);
    }
    
    public void onTouristSpotsSelected(String destinationCode, List<TouristSpot> spots) {
        selectedTouristSpots.put(destinationCode, spots);
        currentCityIndex++;
        
        List<CityLeg> cities = request.getCityLegs();
        boolean allTouristSpotsSelected = true;
        for (CityLeg city : cities) {
            String destCode = city.getDestinationCode();
            String originCode = request.getOriginCode();
            boolean isLastCity = (cities.indexOf(city) == cities.size() - 1);
            
            if (!(isLastCity && destCode.equalsIgnoreCase(originCode))) {
                if (!selectedTouristSpots.containsKey(destCode)) {
                    allTouristSpotsSelected = false;
                    break;
                }
            }
        }
        
        if (allTouristSpotsSelected) {
            showFinalItinerary();
        } else {
            showNextTouristSpotSelection();
        }
    }
    
    private void showFinalItinerary() {
        dispose();
        new MultiCityItineraryPage(request, selectedFlights, selectedHotels, selectedTouristSpots, previousFrame);
    }
}
