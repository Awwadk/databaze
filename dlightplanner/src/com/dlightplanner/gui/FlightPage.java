package com.dlightplanner.gui;

import com.dlightplanner.models.Flight;
import com.dlightplanner.models.FlightSearchRequest;
import com.dlightplanner.models.MultiCityTripRequest.FlightLeg;
import com.dlightplanner.services.FlightService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FlightPage extends JFrame {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM");

    private final FlightService flightService = new FlightService();
    private final FlightSearchRequest request;

    private final double travelerUnits;
    private final String passengerSummary;
    private boolean selectingReturn;

    private JPanel flightListPanel;
    private JLabel flightSummaryLabel;
    private JButton primaryButton;
    private JButton secondaryButton;
    private JLabel totalCostLabel; // For multi-city total cost display

    private List<Flight> outboundFlights = new ArrayList<>();
    private List<Flight> returnFlights = new ArrayList<>();
    private Flight selectedOutboundFlight;
    private Flight selectedReturnFlight;
    private LocalDate currentSelectedDate;
    private LocalDate baseDate; // Date selected in HomePage - center of the date range
    private JPanel dateNavigationPanel;
    private JFrame previousFrame;

    public FlightPage(FlightSearchRequest request) {
        this(request, null);
    }
    
    public FlightPage(FlightSearchRequest request, JFrame previousFrame) {
        this.request = request;
        this.previousFrame = previousFrame;
        this.travelerUnits = request.getAdults() + (request.getChildren() * 0.75) + (request.getInfants() * 0.1);
        this.passengerSummary = buildPassengerSummary(request);

        if (previousFrame != null) {
            previousFrame.setVisible(false);
        }

        setTitle("Voya | Flight Options");
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

        baseDate = request.getDepartureDate();
        currentSelectedDate = request.getDepartureDate();
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(buildDateNavigationPanel(), BorderLayout.NORTH);
        mainPanel.add(buildContentPanel(), BorderLayout.CENTER);
        
        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(buildFooterPanel(), BorderLayout.SOUTH);

        fetchOutboundFlights();
        renderFlightResults(outboundFlights, false);

        setVisible(true);
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(10, 32, 66));
        header.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("Flight Availability");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Serif", Font.BOLD, 26));

        JLabel sub = new JLabel(String.format("%s → %s • %s",
                request.getOriginCode(),
                request.getDestinationCode(),
                request.isRoundTrip() ? "Round trip" : "One-way"));
        sub.setForeground(new Color(210, 225, 240));
        sub.setFont(new Font("SansSerif", Font.PLAIN, 14));

        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildDateNavigationPanel() {
        dateNavigationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        dateNavigationPanel.setBackground(Color.WHITE);
        dateNavigationPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                new EmptyBorder(12, 25, 12, 25)));

        refreshDateNavigation();
        return dateNavigationPanel;
    }

    private void refreshDateNavigation() {
        dateNavigationPanel.removeAll();
        LocalDate today = LocalDate.now();

        LocalDate centerDate;
        if (selectingReturn && request.getReturnDate() != null) {
            centerDate = request.getReturnDate();
        } else {
            centerDate = baseDate != null ? baseDate : request.getDepartureDate();
        }


        for (int i = -6; i <= 7; i++) {
            LocalDate date = centerDate.plusDays(i);
            JPanel dateTab = createDateTab(date, date.equals(currentSelectedDate), today);
            dateNavigationPanel.add(dateTab);
        }
        dateNavigationPanel.revalidate();
        dateNavigationPanel.repaint();
    }

    private JPanel createDateTab(LocalDate date, boolean isSelected, LocalDate today) {
        boolean isPast = date.isBefore(today);
        
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isSelected ? new Color(0, 102, 204) : new Color(200, 200, 200), isSelected ? 2 : 1),
                new EmptyBorder(8, 12, 8, 12)));
        
        if (isPast) {
            tab.setBackground(new Color(240, 240, 240));
            tab.setEnabled(false);
        } else {
            tab.setBackground(isSelected ? new Color(230, 245, 255) : Color.WHITE);
            tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        String dayName = date.format(DateTimeFormatter.ofPattern("EEE"));
        String dayNumber = date.format(DateTimeFormatter.ofPattern("dd"));
        String month = date.format(DateTimeFormatter.ofPattern("MMM"));

        Color textColor = isPast ? new Color(180, 180, 180) : new Color(100, 100, 100);
        Color dateColor = isPast ? new Color(180, 180, 180) : (isSelected ? new Color(0, 102, 204) : Color.BLACK);

        JLabel dayLabel = new JLabel(dayName);
        dayLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        dayLabel.setForeground(textColor);
        dayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel dateLabel = new JLabel(dayNumber);
        dateLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        dateLabel.setForeground(dateColor);
        dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel monthLabel = new JLabel(month);
        monthLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        monthLabel.setForeground(textColor);
        monthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        double cheapestPrice = isPast ? 0 : getCheapestPriceForDate(date);
        JLabel priceLabel = new JLabel(isPast ? "—" : (cheapestPrice > 0 ? formatCurrency(cheapestPrice * travelerUnits) : "N/A"));
        priceLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        priceLabel.setForeground(isPast ? new Color(180, 180, 180) : new Color(34, 139, 34));
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        tab.add(dayLabel);
        tab.add(Box.createVerticalStrut(2));
        tab.add(dateLabel);
        tab.add(Box.createVerticalStrut(2));
        tab.add(monthLabel);
        tab.add(Box.createVerticalStrut(4));
        tab.add(priceLabel);

        if (!isPast) {
            tab.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    selectDate(date);
                }
            });
        }

        return tab;
    }

    private double getCheapestPriceForDate(LocalDate date) {
        List<Flight> flights = flightService.searchFlights(
                selectingReturn ? request.getDestinationCode() : request.getOriginCode(),
                selectingReturn ? request.getOriginCode() : request.getDestinationCode(),
                date,
                10
        );

        if (flights == null || flights.isEmpty()) {
            return 0;
        }

        return flights.stream()
                .mapToDouble(Flight::getFare)
                .min()
                .orElse(0);
    }

    private void selectDate(LocalDate date) {
        currentSelectedDate = date;
        refreshDateNavigation();

        if (selectingReturn) {
            returnFlights = flightService.searchFlights(
                    request.getDestinationCode(),
                    request.getOriginCode(),
                    date,
                    10
            );
            renderFlightResults(returnFlights, true);
        } else {
            outboundFlights = flightService.searchFlights(
                    request.getOriginCode(),
                    request.getDestinationCode(),
                    date,
                    10
            );
            renderFlightResults(outboundFlights, false);
        }
    }

    private JPanel buildContentPanel() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(new EmptyBorder(15, 25, 10, 25));

        flightSummaryLabel = new JLabel("Select a flight to continue.");
        flightSummaryLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        flightSummaryLabel.setBorder(new EmptyBorder(0, 0, 12, 0));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(flightSummaryLabel, BorderLayout.NORTH);
        
        if (previousFrame instanceof MultiCityFlowPage) {
            MultiCityFlowPage multiCityPage = (MultiCityFlowPage) previousFrame;
            List<com.dlightplanner.models.MultiCityTripRequest.FlightLeg> allLegs = multiCityPage.getRequest().getFlightLegs();
            int currentLegIndex = multiCityPage.getCurrentFlightLegIndex();

            if (currentLegIndex == allLegs.size() - 1) {

                double totalCost = calculateTotalFlightCost(multiCityPage);

                totalCostLabel = new JLabel();
                totalCostLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
                totalCostLabel.setForeground(new Color(34, 139, 34));
                totalCostLabel.setBorder(new EmptyBorder(8, 0, 8, 0));
                updateTotalCostLabel(totalCost);
                
                headerPanel.add(totalCostLabel, BorderLayout.SOUTH);
            }
        }
        
        flightListPanel = new JPanel();
        flightListPanel.setLayout(new BoxLayout(flightListPanel, BoxLayout.Y_AXIS));
        flightListPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(flightListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        center.add(headerPanel, BorderLayout.NORTH);
        center.add(scrollPane, BorderLayout.CENTER);
        return center;
    }
    
    private double calculateTotalFlightCost(MultiCityFlowPage multiCityPage) {
        double total = 0;
        Map<com.dlightplanner.models.MultiCityTripRequest.FlightLeg, Flight> selectedFlights = multiCityPage.getSelectedFlights();
        com.dlightplanner.models.MultiCityTripRequest request = multiCityPage.getRequest();
        
        int adults = request.getAdults();
        int children = request.getChildren();
        int infants = request.getInfants();
        
        for (Flight flight : selectedFlights.values()) {
            if (flight != null) {
                double adultFare = flight.getFare();
                double childFare = adultFare * 0.75;
                double infantFare = adultFare * 0.10;
                
                total += (adultFare * adults) + (childFare * children) + (infantFare * infants);
            }
        }
        
        return total;
    }
    
    private void updateTotalCostLabel(double totalCost) {
        if (totalCostLabel != null) {
            totalCostLabel.setText(String.format("Total Flight Cost So Far: ₹%,.0f", totalCost));
        }
    }

    private JPanel buildFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBorder(new EmptyBorder(10, 25, 15, 25));

        secondaryButton = new JButton("Close");
        secondaryButton.addActionListener(e -> handleSecondaryAction());

        primaryButton = new JButton(request.isRoundTrip() ? "Next: Choose Return" : "Confirm Booking");
        primaryButton.addActionListener(e -> handlePrimaryAction());
        primaryButton.setEnabled(false);

        footer.add(secondaryButton);
        footer.add(primaryButton);
        return footer;
    }

    private void fetchOutboundFlights() {
        outboundFlights = flightService.searchFlights(
                request.getOriginCode(),
                request.getDestinationCode(),
                currentSelectedDate,
                10
        );
    }

    private void fetchReturnFlights() {
        if (request.getReturnDate() == null) {
            returnFlights = new ArrayList<>();
            return;
        }
        returnFlights = flightService.searchFlights(
                request.getDestinationCode(),
                request.getOriginCode(),
                request.getReturnDate(),
                10
        );
    }

    private void renderFlightResults(List<Flight> flights, boolean isReturnSegment) {
        String route = isReturnSegment
                ? request.getDestinationCode() + " → " + request.getOriginCode()
                : request.getOriginCode() + " → " + request.getDestinationCode();
        String legLabel = isReturnSegment ? "Return" : "Departure";
        LocalDate flightDate = isReturnSegment ? currentSelectedDate : currentSelectedDate;
        String dateString = (flightDate != null ? flightDate : LocalDate.now())
                .format(DATE_FORMAT);

        flightSummaryLabel.setText(String.format("%s flights • %s • %s • %s",
                legLabel, route, dateString, passengerSummary));

        flightListPanel.removeAll();
        if (flights == null || flights.isEmpty()) {
            JLabel emptyState = new JLabel("No flights available for this leg right now.");
            emptyState.setBorder(new EmptyBorder(40, 0, 40, 0));
            emptyState.setHorizontalAlignment(SwingConstants.CENTER);
            flightListPanel.add(emptyState);
        } else {
            for (Flight flight : flights) {
                boolean isSelected = (!isReturnSegment && flight.equals(selectedOutboundFlight))
                        || (isReturnSegment && flight.equals(selectedReturnFlight));
                flightListPanel.add(buildFlightCard(flight, isSelected, () -> selectFlight(flight)));
                flightListPanel.add(Box.createVerticalStrut(12));
            }
        }

        flightListPanel.revalidate();
        flightListPanel.repaint();
        updateActionButtons();
    }

    private JPanel buildFlightCard(Flight flight, boolean isSelected, Runnable onSelect) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isSelected ? new Color(0, 120, 215) : new Color(225, 225, 225), isSelected ? 2 : 1),
                new EmptyBorder(12, 16, 12, 16)
        ));
        card.setBackground(Color.WHITE);

        JPanel carrierPanel = new JPanel();
        carrierPanel.setOpaque(false);
        carrierPanel.setLayout(new BoxLayout(carrierPanel, BoxLayout.Y_AXIS));
        JLabel airlineLabel = new JLabel(flight.getAirline());
        airlineLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        JLabel flightNumberLabel = new JLabel(flight.getFlightNumber());
        flightNumberLabel.setForeground(new Color(100, 100, 100));
        carrierPanel.add(airlineLabel);
        carrierPanel.add(flightNumberLabel);

        JPanel timingPanel = new JPanel();
        timingPanel.setOpaque(false);
        timingPanel.setLayout(new BoxLayout(timingPanel, BoxLayout.Y_AXIS));
        JLabel timeLabel = new JLabel(flight.getDepartureTime() + " → " + flight.getArrivalTime());
        timeLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        Duration duration = flight.getDuration();
        JLabel durationLabel = new JLabel(String.format("Duration: %dh %02dm",
                duration.toHours(),
                duration.toMinutesPart()));
        durationLabel.setForeground(new Color(90, 90, 90));
        timingPanel.add(timeLabel);
        timingPanel.add(durationLabel);

        JPanel pricePanel = new JPanel();
        pricePanel.setOpaque(false);
        pricePanel.setLayout(new BoxLayout(pricePanel, BoxLayout.Y_AXIS));
        JLabel currentFare = new JLabel(formatCurrency(flight.getFare()));
        currentFare.setFont(new Font("SansSerif", Font.BOLD, 18));
        JLabel strikethrough = new JLabel(String.format(
                "<html><span style='text-decoration:line-through;opacity:0.6;'>%s</span></html>",
                formatCurrency(flight.getOriginalFare())));
        strikethrough.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JLabel saveLabel = null;
        if (flight.getDiscountPercent() > 12) {
            saveLabel = new JLabel(String.format("<html><span style='color:#c62828;font-size:10px;'>%s%% OFF</span></html>",
                    Math.round(flight.getDiscountPercent())));
        }
        JButton selectButton = new JButton(isSelected ? "Selected" : (selectingReturn ? "Select Return" : "Select Flight"));
        selectButton.setEnabled(!isSelected);
        selectButton.addActionListener(e -> onSelect.run());
        pricePanel.add(currentFare);
        pricePanel.add(strikethrough);
        if (saveLabel != null) {
            pricePanel.add(saveLabel);
        }
        pricePanel.add(Box.createVerticalStrut(8));
        pricePanel.add(selectButton);

        JLabel logoSlot = createAirlineLogo(flight.getAirline());
        logoSlot.setHorizontalAlignment(SwingConstants.CENTER);
        logoSlot.setPreferredSize(new Dimension(100, 60));

        card.add(carrierPanel, BorderLayout.WEST);
        card.add(timingPanel, BorderLayout.CENTER);

        JPanel rightWrapper = new JPanel(new BorderLayout(10, 0));
        rightWrapper.setOpaque(false);
        rightWrapper.add(pricePanel, BorderLayout.CENTER);
        rightWrapper.add(logoSlot, BorderLayout.EAST);
        card.add(rightWrapper, BorderLayout.EAST);

        return card;
    }

    private void selectFlight(Flight flight) {
        if (selectingReturn) {
            selectedReturnFlight = flight;
        } else {
            selectedOutboundFlight = flight;
        }
        renderFlightResults(selectingReturn ? returnFlights : outboundFlights, selectingReturn);

        if (previousFrame instanceof MultiCityFlowPage) {
            MultiCityFlowPage multiCityPage = (MultiCityFlowPage) previousFrame;
            List<com.dlightplanner.models.MultiCityTripRequest.FlightLeg> allLegs = multiCityPage.getRequest().getFlightLegs();
            int currentLegIndex = multiCityPage.getCurrentFlightLegIndex();
            
            if (currentLegIndex == allLegs.size() - 1 && totalCostLabel != null) {

                com.dlightplanner.models.MultiCityTripRequest.FlightLeg currentLeg = multiCityPage.getCurrentFlightLeg();
                if (currentLeg != null) {

                    double totalCost = calculateTotalFlightCost(multiCityPage);

                    com.dlightplanner.models.MultiCityTripRequest request = multiCityPage.getRequest();
                    int adults = request.getAdults();
                    int children = request.getChildren();
                    int infants = request.getInfants();
                    
                    double adultFare = flight.getFare();
                    double childFare = adultFare * 0.75;
                    double infantFare = adultFare * 0.10;
                    totalCost += (adultFare * adults) + (childFare * children) + (infantFare * infants);
                    
                    updateTotalCostLabel(totalCost);
                }
            }
        }
    }

    private void updateActionButtons() {
        if (!request.isRoundTrip()) {
            primaryButton.setText("Confirm Booking");
            primaryButton.setEnabled(selectedOutboundFlight != null);
            secondaryButton.setText("Close");
        } else if (!selectingReturn) {
            primaryButton.setText("Next: Choose Return");
            primaryButton.setEnabled(selectedOutboundFlight != null);
            secondaryButton.setText("Close");
        } else {
            primaryButton.setText("Confirm Booking");
            primaryButton.setEnabled(selectedReturnFlight != null);
            secondaryButton.setText("Back to Outbound");
        }
    }

    private void handlePrimaryAction() {
        if (!request.isRoundTrip()) {
            if (selectedOutboundFlight == null) {
                JOptionPane.showMessageDialog(this, "Select a flight to continue.", "Pick a flight", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            confirmBooking(selectedOutboundFlight, null);
            return;
        }

        if (!selectingReturn) {
            if (selectedOutboundFlight == null) {
                JOptionPane.showMessageDialog(this, "Pick your departure flight first.", "Pick a flight", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            selectingReturn = true;

            if (request.getReturnDate() != null) {
                baseDate = request.getReturnDate();
                currentSelectedDate = request.getReturnDate();
            } else {

                baseDate = request.getDepartureDate().plusDays(7);
                currentSelectedDate = baseDate;
            }
            refreshDateNavigation();
            fetchReturnFlights();
            selectedReturnFlight = null;
            renderFlightResults(returnFlights, true);
            return;
        }

        if (selectedReturnFlight == null) {
            JOptionPane.showMessageDialog(this, "Select a return flight to finish booking.", "Pick a flight", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        confirmBooking(selectedOutboundFlight, selectedReturnFlight);
    }

    private void handleSecondaryAction() {
        if (request.isRoundTrip() && selectingReturn) {
            selectingReturn = false;
            renderFlightResults(outboundFlights, false);
        } else {
            if (previousFrame != null && previousFrame.isDisplayable()) {

                previousFrame.setExtendedState(JFrame.NORMAL);
                previousFrame.setVisible(true);
                previousFrame.toFront();
                previousFrame.requestFocus();
            }
            dispose();
        }
    }

    private void confirmBooking(Flight outbound, Flight inbound) {
        StringBuilder summary = new StringBuilder();
        summary.append("=== FLIGHT BOOKING SUMMARY ===\n\n");

        summary.append("DEPARTURE FLIGHT:\n");
        summary.append(outbound.getAirline()).append(" ").append(outbound.getFlightNumber())
               .append("\n").append(outbound.getDepartureTime()).append(" → ").append(outbound.getArrivalTime())
               .append("\n\nFare Breakdown:\n");
        
        double adultFare = outbound.getFare();
        double childFare = adultFare * 0.75; // 75% of adult fare
        double infantFare = adultFare * 0.10; // 10% of adult fare
        
        int adults = request.getAdults();
        int children = request.getChildren();
        int infants = request.getInfants();
        
        double departureTotal = 0;
        if (adults > 0) {
            double adultTotal = adultFare * adults;
            departureTotal += adultTotal;
            summary.append(String.format("  Adults (%d × ₹%,.0f): ₹%,.0f\n", adults, adultFare, adultTotal));
        }
        if (children > 0) {
            double childTotal = childFare * children;
            departureTotal += childTotal;
            summary.append(String.format("  Children (%d × ₹%,.0f): ₹%,.0f\n", children, childFare, childTotal));
        }
        if (infants > 0) {
            double infantTotal = infantFare * infants;
            departureTotal += infantTotal;
            summary.append(String.format("  Infants (%d × ₹%,.0f): ₹%,.0f\n", infants, infantFare, infantTotal));
        }
        summary.append(String.format("Departure Total: ₹%,.0f\n", departureTotal));

        double returnTotal = 0;
        if (inbound != null) {
            summary.append("\n\nRETURN FLIGHT:\n");
            summary.append(inbound.getAirline()).append(" ").append(inbound.getFlightNumber())
                   .append("\n").append(inbound.getDepartureTime()).append(" → ").append(inbound.getArrivalTime())
                   .append("\n\nFare Breakdown:\n");
            
            double returnAdultFare = inbound.getFare();
            double returnChildFare = returnAdultFare * 0.75;
            double returnInfantFare = returnAdultFare * 0.10;
            
            if (adults > 0) {
                double adultTotal = returnAdultFare * adults;
                returnTotal += adultTotal;
                summary.append(String.format("  Adults (%d × ₹%,.0f): ₹%,.0f\n", adults, returnAdultFare, adultTotal));
            }
            if (children > 0) {
                double childTotal = returnChildFare * children;
                returnTotal += childTotal;
                summary.append(String.format("  Children (%d × ₹%,.0f): ₹%,.0f\n", children, returnChildFare, childTotal));
            }
            if (infants > 0) {
                double infantTotal = returnInfantFare * infants;
                returnTotal += infantTotal;
                summary.append(String.format("  Infants (%d × ₹%,.0f): ₹%,.0f\n", infants, returnInfantFare, infantTotal));
            }
            summary.append(String.format("Return Total: ₹%,.0f\n", returnTotal));
        }

        if (previousFrame instanceof MultiCityFlowPage) {
            MultiCityFlowPage multiCityPage = (MultiCityFlowPage) previousFrame;

            FlightLeg leg = multiCityPage.getCurrentFlightLeg();
            if (leg != null) {
                multiCityPage.onFlightSelected(leg, outbound);
            }
            dispose();
            return;
        }

        double grandTotal = departureTotal + returnTotal;
        summary.append("\n═══════════════════════════\n");
        summary.append(String.format("GRAND TOTAL: ₹%,.0f", grandTotal));

        JOptionPane.showMessageDialog(this, summary.toString(), "Flight Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);

        dispose();
        new HotelPage(request.getDestinationCode(), request.getDepartureDate(), request.getReturnDate(), this);
    }

    private String buildPassengerSummary(FlightSearchRequest req) {
        StringBuilder builder = new StringBuilder();
        builder.append(req.getAdults()).append(" adults");
        if (req.getChildren() > 0) {
            builder.append(", ").append(req.getChildren()).append(" children");
        }
        if (req.getInfants() > 0) {
            builder.append(", ").append(req.getInfants()).append(" infants");
        }
        return builder.toString();
    }

    private String formatCurrency(double amount) {
        return String.format("₹%,.0f", amount);
    }

    public FlightSearchRequest getRequest() {
        return request;
    }
    
    public Flight getSelectedOutboundFlight() {
        return selectedOutboundFlight;
    }
    
    public Flight getSelectedReturnFlight() {
        return selectedReturnFlight;
    }

    private JLabel createAirlineLogo(String airline) {
        String logoPath = getAirlineLogoPath(airline);
        ImageIcon logoIcon = new ImageIcon(logoPath);
        
        if (logoIcon.getIconWidth() > 0 && logoIcon.getIconHeight() > 0) {
            Image scaled = logoIcon.getImage().getScaledInstance(90, 60, Image.SCALE_SMOOTH);
            JLabel label = new JLabel(new ImageIcon(scaled));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            return label;
        } else {
            JLabel fallback = new JLabel(airline);
            fallback.setFont(new Font("SansSerif", Font.PLAIN, 9));
            fallback.setHorizontalAlignment(SwingConstants.CENTER);
            fallback.setBorder(BorderFactory.createDashedBorder(new Color(200, 200, 200)));
            return fallback;
        }
    }

    private String getAirlineLogoPath(String airline) {
        String baseName = airline.toLowerCase().replace(" ", "_").replace("-", "_");
        return "resources/images/airlines/" + baseName + ".png";
    }
}

