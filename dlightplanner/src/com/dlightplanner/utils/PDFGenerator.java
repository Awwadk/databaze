package com.dlightplanner.utils;

import com.dlightplanner.models.*;
import com.dlightplanner.services.AirportService;
import com.dlightplanner.services.LocalCostService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Utility class for generating PDF documents from booking summaries
 * Uses Apache PDFBox library for PDF generation
 */
public class PDFGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    
    /**
     * Generate PDF for single city booking
     * Returns true if successful, false otherwise
     */
    public static boolean generatePDF(String filePath, FlightSearchRequest request, 
                                     Flight outbound, Flight returnFlight,
                                     Hotel hotel, List<TouristSpot> spots, 
                                     LocalDate checkIn, LocalDate checkOut,
                                     String destCode, AirportService airportService,
                                     LocalCostService localCostService) {
        try {

            return generatePDFWithPDFBox(filePath, request, outbound, returnFlight,
                hotel, spots, checkIn, checkOut, destCode, airportService, localCostService);
        } catch (Exception e) {

            System.err.println("PDFBox not available, generating text file: " + e.getMessage());
            return generateTextFile(filePath + ".txt", request, outbound, returnFlight,
                hotel, spots, checkIn, checkOut, destCode, airportService, localCostService);
        }
    }
    
    /**
     * Generate PDF using Apache PDFBox library
     */
    private static boolean generatePDFWithPDFBox(String filePath, FlightSearchRequest request,
                                                Flight outbound, Flight returnFlight,
                                                Hotel hotel, List<TouristSpot> spots,
                                                LocalDate checkIn, LocalDate checkOut,
                                                String destCode, AirportService airportService,
                                                LocalCostService localCostService) throws Exception {

        Class<?> documentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
        Class<?> pageClass = Class.forName("org.apache.pdfbox.pdmodel.PDPage");
        Class<?> contentStreamClass = Class.forName("org.apache.pdfbox.pdmodel.PDPageContentStream");
        Class<?> fontClass = Class.forName("org.apache.pdfbox.pdmodel.font.PDType1Font");
        Class<?> pageSizeClass = Class.forName("org.apache.pdfbox.pdmodel.common.PDRectangle");

        Object document = documentClass.getDeclaredConstructor().newInstance();
        Object pageSize = pageSizeClass.getField("A4").get(null);
        Object page = pageClass.getDeclaredConstructor(pageSizeClass).newInstance(pageSize);
        documentClass.getMethod("addPage", pageClass).invoke(document, page);
        
        Object contentStream = contentStreamClass.getDeclaredConstructor(
            pageClass, documentClass
        ).newInstance(page, document);
        
        float y = 750;
        float leftMargin = 50;
        float lineHeight = 15f;
        
        Object helveticaBold = fontClass.getField("HELVETICA_BOLD").get(null);
        Object helvetica = fontClass.getField("HELVETICA").get(null);

        contentStreamClass.getMethod("beginText").invoke(contentStream);
        contentStreamClass.getMethod("setFont", fontClass, float.class).invoke(contentStream, helveticaBold, 20f);
        contentStreamClass.getMethod("newLineAtOffset", float.class, float.class).invoke(contentStream, leftMargin, y);
        contentStreamClass.getMethod("showText", String.class).invoke(contentStream, "VOYA - Booking Summary");
        contentStreamClass.getMethod("endText").invoke(contentStream);
        y -= 30;

        StringBuilder content = buildPDFContent(request, outbound, returnFlight, hotel, spots,
            checkIn, checkOut, destCode, airportService, localCostService);
        
        String[] lines = content.toString().split("\n");
        
        contentStreamClass.getMethod("beginText").invoke(contentStream);
        contentStreamClass.getMethod("setFont", fontClass, float.class).invoke(contentStream, helvetica, 11f);
        
        for (String line : lines) {
            if (y < 50) {

                contentStreamClass.getMethod("endText").invoke(contentStream);
                contentStreamClass.getMethod("close").invoke(contentStream);
                
                page = pageClass.getDeclaredConstructor(pageSizeClass).newInstance(pageSize);
                documentClass.getMethod("addPage", pageClass).invoke(document, page);
                contentStream = contentStreamClass.getDeclaredConstructor(pageClass, documentClass)
                    .newInstance(page, document);
                
                contentStreamClass.getMethod("beginText").invoke(contentStream);
                contentStreamClass.getMethod("setFont", fontClass, float.class).invoke(contentStream, helvetica, 11f);
                y = 750;
            }

            if (line.length() > 80) {
                String[] words = line.split(" ");
                StringBuilder currentLine = new StringBuilder();
                for (String word : words) {
                    if ((currentLine + " " + word).length() > 80) {
                        contentStreamClass.getMethod("newLineAtOffset", float.class, float.class)
                            .invoke(contentStream, leftMargin, y);
                        contentStreamClass.getMethod("showText", String.class)
                            .invoke(contentStream, currentLine.toString());
                        y -= lineHeight;
                        currentLine = new StringBuilder(word);
                    } else {
                        if (currentLine.length() > 0) currentLine.append(" ");
                        currentLine.append(word);
                    }
                }
                if (currentLine.length() > 0) {
                    contentStreamClass.getMethod("newLineAtOffset", float.class, float.class)
                        .invoke(contentStream, leftMargin, y);
                    contentStreamClass.getMethod("showText", String.class)
                        .invoke(contentStream, currentLine.toString());
                    y -= lineHeight;
                }
            } else {
                contentStreamClass.getMethod("newLineAtOffset", float.class, float.class)
                    .invoke(contentStream, leftMargin, y);
                contentStreamClass.getMethod("showText", String.class).invoke(contentStream, line);
                y -= lineHeight;
            }
        }
        
        contentStreamClass.getMethod("endText").invoke(contentStream);
        contentStreamClass.getMethod("close").invoke(contentStream);
        
        documentClass.getMethod("save", String.class).invoke(document, filePath);
        documentClass.getMethod("close").invoke(document);
        
        return true;
    }
    
    /**
     * Generate a text file as fallback
     */
    private static boolean generateTextFile(String filePath, FlightSearchRequest request,
                                           Flight outbound, Flight returnFlight,
                                           Hotel hotel, List<TouristSpot> spots,
                                           LocalDate checkIn, LocalDate checkOut,
                                           String destCode, AirportService airportService,
                                           LocalCostService localCostService) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            StringBuilder content = buildPDFContent(request, outbound, returnFlight, hotel, spots,
                checkIn, checkOut, destCode, airportService, localCostService);
            fos.write(content.toString().getBytes());
            return true;
        } catch (IOException e) {
            System.err.println("Error writing text file: " + e.getMessage());
            return false;
        }
    }
    
    private static StringBuilder buildPDFContent(FlightSearchRequest request, Flight outbound,
                                                Flight returnFlight, Hotel hotel,
                                                List<TouristSpot> spots, LocalDate checkIn,
                                                LocalDate checkOut, String destCode,
                                                AirportService airportService,
                                                LocalCostService localCostService) {
        StringBuilder content = new StringBuilder();
        double totalCost = 0;
        
        content.append("\n");
        content.append("TRIP BOOKING CONFIRMATION\n");
        content.append("==================================================\n\n");
        
        String cityName = airportService.getAirports().stream()
            .filter(a -> a.getIata().equalsIgnoreCase(destCode))
            .map(Airport::getCity)
            .findFirst()
            .orElse(destCode);
        
        content.append("Destination: ").append(cityName).append("\n");
        content.append("Check-in: ").append(checkIn.format(DATE_FORMAT)).append("\n");
        content.append("Check-out: ").append(checkOut.format(DATE_FORMAT)).append("\n\n");

        content.append("FLIGHT COSTS\n");
        content.append("------------------------------\n");
        if (outbound != null && request != null) {
            double adultFare = outbound.getFare();
            double childFare = adultFare * 0.75;
            double infantFare = adultFare * 0.10;
            
            int adults = request.getAdults();
            int children = request.getChildren();
            int infants = request.getInfants();
            
            double flightTotal = 0;
            
            content.append("Departure: ").append(outbound.getAirline())
                .append(" ").append(outbound.getFlightNumber()).append("\n");
            
            if (adults > 0) {
                double cost = adultFare * adults;
                content.append(String.format("  Adults (%d × ₹%,.0f): ₹%,.0f\n", adults, adultFare, cost));
                flightTotal += cost;
            }
            if (children > 0) {
                double cost = childFare * children;
                content.append(String.format("  Children (%d × ₹%,.0f): ₹%,.0f\n", children, childFare, cost));
                flightTotal += cost;
            }
            if (infants > 0) {
                double cost = infantFare * infants;
                content.append(String.format("  Infants (%d × ₹%,.0f): ₹%,.0f\n", infants, infantFare, cost));
                flightTotal += cost;
            }
            
            if (returnFlight != null) {
                adultFare = returnFlight.getFare();
                childFare = adultFare * 0.75;
                infantFare = adultFare * 0.10;
                
                content.append("Return: ").append(returnFlight.getAirline())
                    .append(" ").append(returnFlight.getFlightNumber()).append("\n");
                
                if (adults > 0) {
                    double cost = adultFare * adults;
                    content.append(String.format("  Adults (%d × ₹%,.0f): ₹%,.0f\n", adults, adultFare, cost));
                    flightTotal += cost;
                }
                if (children > 0) {
                    double cost = childFare * children;
                    content.append(String.format("  Children (%d × ₹%,.0f): ₹%,.0f\n", children, childFare, cost));
                    flightTotal += cost;
                }
                if (infants > 0) {
                    double cost = infantFare * infants;
                    content.append(String.format("  Infants (%d × ₹%,.0f): ₹%,.0f\n", infants, infantFare, cost));
                    flightTotal += cost;
                }
            }
            
            content.append(String.format("Total Flight Cost: ₹%,.0f\n\n", flightTotal));
            totalCost += flightTotal;
        } else {
            content.append("Flight information not available\n");
            content.append("Total Flight Cost: ₹0\n\n");
        }

        content.append("HOTEL COSTS\n");
        content.append("------------------------------\n");
        if (hotel != null) {
            long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
            if (nights < 1) nights = 1;
            
            double pricePerNight = hotel.getPricePerNight();
            double hotelTotal = pricePerNight * nights;
            
            content.append("Hotel: ").append(hotel.getName()).append("\n");
            content.append(String.format("Price per night: ₹%,.0f\n", pricePerNight));
            content.append("Number of nights: ").append(nights).append("\n");
            content.append(String.format("Total Hotel Cost: ₹%,.0f\n\n", hotelTotal));
            totalCost += hotelTotal;
        } else {
            content.append("No hotel selected\n");
            content.append("Total Hotel Cost: ₹0\n\n");
        }

        content.append("TOURIST SPOTS COSTS\n");
        content.append("------------------------------\n");
        if (spots != null && !spots.isEmpty()) {
            int travelers = request != null ? request.getAdults() + request.getChildren() : 1;
            double spotsTotal = 0;
            
            for (TouristSpot spot : spots) {
                double spotCost = spot.getPrice() * travelers;
                content.append(String.format("%s (₹%,.0f × %d): ₹%,.0f\n",
                    spot.getName(), spot.getPrice(), travelers, spotCost));
                spotsTotal += spotCost;
            }
            
            content.append(String.format("Total Tourist Spots Cost: ₹%,.0f\n\n", spotsTotal));
            totalCost += spotsTotal;
        } else {
            content.append("No tourist spots selected\n");
            content.append("Total Tourist Spots Cost: ₹0\n\n");
        }

        if (request != null) {
            content.append("LOCAL FOOD COSTS\n");
            content.append("------------------------------\n");
            int numberOfDays = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
            if (numberOfDays < 1) numberOfDays = 1;
            double foodCostPerPerson = localCostService.getLocalFoodCost(destCode);
            int totalTravelers = request.getAdults() + request.getChildren();
            double foodTotal = foodCostPerPerson * totalTravelers * numberOfDays;
            
            content.append(String.format("Food cost per person per day: ₹%,.0f\n", foodCostPerPerson));
            content.append("Number of travelers: ").append(totalTravelers).append("\n");
            content.append("Number of days: ").append(numberOfDays).append("\n");
            content.append(String.format("Total Food Cost: ₹%,.0f\n\n", foodTotal));
            totalCost += foodTotal;

            content.append("LOCAL TRAVEL COSTS\n");
            content.append("------------------------------\n");
            double travelCostPerPerson = localCostService.getLocalTravelCost(destCode);
            double travelTotal = travelCostPerPerson * totalTravelers * numberOfDays;
            content.append(String.format("Travel cost per person per day: ₹%,.0f\n", travelCostPerPerson));
            content.append("Number of travelers: ").append(totalTravelers).append("\n");
            content.append("Number of days: ").append(numberOfDays).append("\n");
            content.append(String.format("Total Travel Cost: ₹%,.0f\n\n", travelTotal));
            totalCost += travelTotal;
        }

        content.append("\n");
        content.append("==================================================\n");
        content.append(String.format("GRAND TOTAL: ₹%,.0f\n", totalCost));
        content.append("==================================================\n");
        
        content.append("\n\nThank you for choosing Voya!\n");
        content.append("Generated on: ").append(LocalDate.now().format(DATE_FORMAT)).append("\n");
        
        return content;
    }
}