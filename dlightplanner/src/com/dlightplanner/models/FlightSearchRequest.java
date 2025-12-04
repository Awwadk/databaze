package com.dlightplanner.models;

import java.time.LocalDate;

public class FlightSearchRequest {

    private final String originCode;
    private final String destinationCode;
    private final LocalDate departureDate;
    private final LocalDate returnDate; // can be null for one-way
    private final boolean roundTrip;
    private final int adults;
    private final int children;
    private final int infants;

    public FlightSearchRequest(String originCode,
                               String destinationCode,
                               LocalDate departureDate,
                               LocalDate returnDate,
                               boolean roundTrip,
                               int adults,
                               int children,
                               int infants) {
        this.originCode = originCode;
        this.destinationCode = destinationCode;
        this.departureDate = departureDate;
        this.returnDate = returnDate;
        this.roundTrip = roundTrip;
        this.adults = adults;
        this.children = children;
        this.infants = infants;
    }

    public String getOriginCode() {
        return originCode;
    }

    public String getDestinationCode() {
        return destinationCode;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public boolean isRoundTrip() {
        return roundTrip;
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
}


