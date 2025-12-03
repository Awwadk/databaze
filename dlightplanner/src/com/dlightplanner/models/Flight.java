package com.dlightplanner.models;

import java.time.Duration;
import java.time.LocalTime;

public class Flight {

    private final String airline;
    private final String flightNumber;
    private final String originCode;
    private final String destinationCode;
    private final LocalTime departureTime;
    private final LocalTime arrivalTime;
    private final Duration duration;
    private final double fare;
    private final double originalFare;
    private final double discountPercent;

    public Flight(String airline,
                  String flightNumber,
                  String originCode,
                  String destinationCode,
                  LocalTime departureTime,
                  LocalTime arrivalTime,
                  Duration duration,
                  double fare,
                  double originalFare,
                  double discountPercent) {
        this.airline = airline;
        this.flightNumber = flightNumber;
        this.originCode = originCode;
        this.destinationCode = destinationCode;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.duration = duration;
        this.fare = fare;
        this.originalFare = originalFare;
        this.discountPercent = discountPercent;
    }

    public String getAirline() {
        return airline;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getOriginCode() {
        return originCode;
    }

    public String getDestinationCode() {
        return destinationCode;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public double getFare() {
        return fare;
    }

    public double getOriginalFare() {
        return originalFare;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }
}

