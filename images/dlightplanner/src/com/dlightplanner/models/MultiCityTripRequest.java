package com.dlightplanner.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MultiCityTripRequest {
    private final String originCode;
    private final List<CityLeg> cityLegs; // List of destinations with dates
    private final boolean returnToOrigin;
    private final LocalDate returnDate; // Only used if returnToOrigin is true
    private final int adults;
    private final int children;
    private final int infants;

    public static class CityLeg {
        private final String destinationCode;
        private final LocalDate arrivalDate;
        private final LocalDate departureDate; // When leaving this city

        public CityLeg(String destinationCode, LocalDate arrivalDate, LocalDate departureDate) {
            this.destinationCode = destinationCode;
            this.arrivalDate = arrivalDate;
            this.departureDate = departureDate;
        }

        public String getDestinationCode() {
            return destinationCode;
        }

        public LocalDate getArrivalDate() {
            return arrivalDate;
        }

        public LocalDate getDepartureDate() {
            return departureDate;
        }
    }

    public MultiCityTripRequest(String originCode,
                               List<CityLeg> cityLegs,
                               boolean returnToOrigin,
                               LocalDate returnDate,
                               int adults,
                               int children,
                               int infants) {
        this.originCode = originCode;
        this.cityLegs = cityLegs != null ? new ArrayList<>(cityLegs) : new ArrayList<>();
        this.returnToOrigin = returnToOrigin;
        this.returnDate = returnDate;
        this.adults = adults;
        this.children = children;
        this.infants = infants;
    }

    public String getOriginCode() {
        return originCode;
    }

    public List<CityLeg> getCityLegs() {
        return cityLegs;
    }

    public boolean isReturnToOrigin() {
        return returnToOrigin;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public int getAdults() {
        return adults;
    }

    public int getChildren() {
        return children;
    }

    public int getInfants() {
        return infants;
    }

    /**
     * Get all flight legs for this multi-city trip
     * Returns list of FlightLeg objects representing each flight segment
     */
    public List<FlightLeg> getFlightLegs() {
        List<FlightLeg> legs = new ArrayList<>();
        
        if (cityLegs.isEmpty()) {
            return legs;
        }

        legs.add(new FlightLeg(originCode, cityLegs.get(0).getDestinationCode(), 
                              cityLegs.get(0).getArrivalDate()));

        for (int i = 0; i < cityLegs.size() - 1; i++) {
            CityLeg current = cityLegs.get(i);
            CityLeg next = cityLegs.get(i + 1);
            legs.add(new FlightLeg(current.getDestinationCode(), 
                                  next.getDestinationCode(), 
                                  next.getArrivalDate()));
        }

        if (returnToOrigin && returnDate != null) {
            CityLeg lastCity = cityLegs.get(cityLegs.size() - 1);
            legs.add(new FlightLeg(lastCity.getDestinationCode(), 
                                  originCode, 
                                  returnDate));
        }

        return legs;
    }

    public static class FlightLeg {
        private final String fromCode;
        private final String toCode;
        private final LocalDate date;

        public FlightLeg(String fromCode, String toCode, LocalDate date) {
            this.fromCode = fromCode;
            this.toCode = toCode;
            this.date = date;
        }

        public String getFromCode() {
            return fromCode;
        }

        public String getToCode() {
            return toCode;
        }

        public LocalDate getDate() {
            return date;
        }
    }
}



