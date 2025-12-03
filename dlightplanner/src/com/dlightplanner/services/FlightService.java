package com.dlightplanner.services;

import com.dlightplanner.models.Flight;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class FlightService {

    private static final List<String> AIRLINES = List.of(
            "Air India",
            "IndiGo",
            "Akasa Air",
            "SpiceJet",
            "Air India Express"
    );

    private static final Map<String, double[]> COORDINATES = new HashMap<>();

    static {
        COORDINATES.put("DEL", new double[]{28.5562, 77.1000});
        COORDINATES.put("BOM", new double[]{19.0896, 72.8656});
        COORDINATES.put("BLR", new double[]{13.1986, 77.7066});
        COORDINATES.put("MAA", new double[]{12.9900, 80.1693});
        COORDINATES.put("HYD", new double[]{17.2403, 78.4294});
        COORDINATES.put("CCU", new double[]{22.6570, 88.4467});
        COORDINATES.put("GOI", new double[]{15.3800, 73.8310});
        COORDINATES.put("GOX", new double[]{15.5450, 73.8700});
        COORDINATES.put("COK", new double[]{10.1520, 76.4019});
        COORDINATES.put("TRV", new double[]{8.4821, 76.9200});
        COORDINATES.put("PNQ", new double[]{18.5822, 73.9197});
        COORDINATES.put("AMD", new double[]{23.0739, 72.6346});
        COORDINATES.put("JAI", new double[]{26.8242, 75.8122});
        COORDINATES.put("LKO", new double[]{26.7606, 80.8893});
        COORDINATES.put("GAU", new double[]{26.1061, 91.5859});
        COORDINATES.put("BBI", new double[]{20.2443, 85.8178});
        COORDINATES.put("IDR", new double[]{22.7218, 75.8011});
        COORDINATES.put("IXC", new double[]{30.6735, 76.7885});
        COORDINATES.put("SXR", new double[]{33.9871, 74.7740});
        COORDINATES.put("IXZ", new double[]{11.6410, 92.7297});
        COORDINATES.put("VTZ", new double[]{17.7212, 83.2240});
        COORDINATES.put("IXM", new double[]{9.8346, 78.0934});
        COORDINATES.put("TRZ", new double[]{10.7654, 78.7080});
        COORDINATES.put("IXE", new double[]{12.9613, 74.8901});
        COORDINATES.put("UDR", new double[]{24.6177, 73.8963});
        COORDINATES.put("IXJ", new double[]{32.6891, 74.8373});
        COORDINATES.put("IXL", new double[]{34.1359, 77.5465});
        COORDINATES.put("IXR", new double[]{23.3143, 85.3216});
        COORDINATES.put("IXU", new double[]{19.8627, 75.3981});
        COORDINATES.put("NAG", new double[]{21.0910, 79.0561});
        COORDINATES.put("VNS", new double[]{25.4524, 82.8590});
        COORDINATES.put("ATQ", new double[]{31.7096, 74.7973});
        COORDINATES.put("IMF", new double[]{24.7645, 93.8967});
    }

    public List<Flight> searchFlights(String originCode,
                                      String destinationCode,
                                      LocalDate date,
                                      int count) {
        double distanceKm = estimateDistance(originCode, destinationCode);
        double baseFare = distanceKm * 5.2; // ₹ per km baseline
        if (date != null && date.getDayOfWeek().getValue() >= 5) {
            baseFare *= 1.08; // weekend demand
        }

        List<Flight> flights = new ArrayList<>();
        LocalTime firstDeparture = LocalTime.of(5, 45);
        if (count < 1) {
            count = 1;
        }

        for (int i = 0; i < count; i++) {
            String airline = AIRLINES.get(i % AIRLINES.size());
            String flightNumber = generateFlightNumber(airline, i);

            long offsetMinutes = ThreadLocalRandom.current().nextLong(-15, 20);
            LocalTime departure = firstDeparture.plusMinutes(i * 95L + offsetMinutes);
            double durationHours = clamp(distanceKm / 700.0 + ThreadLocalRandom.current().nextDouble(-0.2, 0.35), 1.0, 5.5);
            Duration duration = Duration.ofMinutes((long) (durationHours * 60));
            LocalTime arrival = departure.plus(duration);

            double dynamicFactor = ThreadLocalRandom.current().nextDouble(0.92, 1.18);
            double fare = Math.max(2500, baseFare * dynamicFactor);
            double discount = ThreadLocalRandom.current().nextDouble(0.75, 0.88);
            double saleFare = fare * discount;
            double percentOff = Math.max(0, Math.round((1 - (saleFare / fare)) * 100));
            boolean highlightDeal = percentOff > 12 && ThreadLocalRandom.current().nextDouble() < 0.4;

            flights.add(new Flight(
                    airline,
                    flightNumber,
                    originCode,
                    destinationCode,
                    departure,
                    arrival,
                    duration,
                    Math.round(saleFare / 10.0) * 10.0,
                    Math.round(fare / 10.0) * 10.0,
                    highlightDeal ? percentOff : 0
            ));
        }

        return flights;
    }

    private double estimateDistance(String origin, String destination) {
        double[] originCoord = COORDINATES.get(origin);
        double[] destCoord = COORDINATES.get(destination);

        if (originCoord == null || destCoord == null) {
            return 1200; // fallback average distance
        }

        double lat1 = Math.toRadians(originCoord[0]);
        double lon1 = Math.toRadians(originCoord[1]);
        double lat2 = Math.toRadians(destCoord[0]);
        double lon2 = Math.toRadians(destCoord[1]);

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));
        double earthRadiusKm = 6371;
        return earthRadiusKm * c;
    }

    private String generateFlightNumber(String airline, int index) {
        String prefix = switch (airline) {
            case "Air India" -> "AI";
            case "IndiGo" -> "6E";
            case "Akasa Air" -> "QP";
            case "SpiceJet" -> "SG";
            case "Air India Express" -> "IX";
            default -> "XY";
        };
        return prefix + (200 + ThreadLocalRandom.current().nextInt(700) + index);
    }

    private double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}

