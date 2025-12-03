package com.dlightplanner.services;

import com.dlightplanner.models.Hotel;
import com.dlightplanner.models.TouristSpot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.*;
import java.util.List;

/**
 * Smart Itinerary Generator Service
 * Generates optimized day-by-day itineraries based on selected tourist spots
 */
public class ItineraryGenerator {
    
    private static final int MAX_HOURS_PER_DAY = 10; // 9 AM to 7 PM with breaks (extendable to 9 PM if needed)
    private static final int EXTENDED_HOURS_PER_DAY = 12; // Extended hours for packed days (9 AM to 9 PM)
    private static final int TRAVEL_BUFFER_MINUTES = 30; // Minimum buffer between spots
    private static final int LUNCH_BREAK_HOURS = 1; // Lunch break duration
    private static final double MIN_SPOT_DURATION_HOURS = 1.0; // Minimum duration per spot (reduced for maximization)
    private static final double REDUCED_SPOT_DURATION_HOURS = 1.5; // Reduced duration for fitting more spots (1-1.5 hours)
    
    /**
     * Calculate optimal number of days needed based on selected spots
     */
    public int calculateOptimalDays(List<TouristSpot> spots, Hotel hotel) {
        if (spots == null || spots.isEmpty()) {
            return 1; // At least 1 day for hotel stay
        }

        int totalActivityHours = (int) Math.ceil(spots.stream()
                .mapToDouble(TouristSpot::getEstimatedDuration)
                .sum());

        int estimatedTravelHours = (int) Math.ceil(spots.size() / 3.0);

        int bufferHours = (int) Math.ceil(spots.size() * 0.5);

        int estimatedDays = (int) Math.ceil((totalActivityHours + estimatedTravelHours + bufferHours) / (double) MAX_HOURS_PER_DAY);
        int lunchHours = estimatedDays * LUNCH_BREAK_HOURS;

        int totalHours = totalActivityHours + estimatedTravelHours + bufferHours + lunchHours;

        int days = (int) Math.ceil(totalHours / (double) MAX_HOURS_PER_DAY);
        
        return Math.max(1, days); // At least 1 day
    }
    
    /**
     * Get adjusted duration for a spot based on number of days and spots to maximize display
     * Reduces to 1-1.5 hours to fit more spots (at least 4 per day)
     */
    private double getAdjustedDuration(TouristSpot spot, int numberOfDays, int totalSpots) {
        double originalDuration = spot.getEstimatedDuration();


        double variation = (spot.getId() % 10) / 20.0; // 0.0 to 0.45 hours variation
        double targetDuration = MIN_SPOT_DURATION_HOURS + variation; // 1.0 to 1.45 hours

        targetDuration = Math.min(targetDuration, REDUCED_SPOT_DURATION_HOURS);

        return Math.max(MIN_SPOT_DURATION_HOURS, Math.min(originalDuration, targetDuration));
    }
    
    /**
     * Group spots by proximity and distribute across days
     * MAXIMIZES spots shown while ensuring ALL selected spots are included
     * Ensures at least 4 spots per day with 1-1.5 hour durations
     */
    public Map<LocalDate, List<TouristSpot>> groupSpotsByProximity(
            List<TouristSpot> spots, Hotel hotel, LocalDate startDate, int numberOfDays) {
        
        Map<LocalDate, List<TouristSpot>> dayWiseSpots = new LinkedHashMap<>();

        for (int i = 0; i < numberOfDays; i++) {
            dayWiseSpots.put(startDate.plusDays(i), new ArrayList<>());
        }
        
        if (spots == null || spots.isEmpty()) {
            return dayWiseSpots;
        }
        
        int totalSpots = spots.size();



        List<TouristSpot> shuffledSpots = new ArrayList<>(spots);
        Collections.shuffle(shuffledSpots, new Random(spots.hashCode()));

        int spotIndex = 0;
        for (TouristSpot spot : shuffledSpots) {
            int dayIndex = spotIndex % numberOfDays;
            LocalDate dayDate = startDate.plusDays(dayIndex);
            dayWiseSpots.get(dayDate).add(spot);
            spotIndex++;
        }

        int minSpotsPerDay = 4;
        int totalAvailable = totalSpots;

        if (totalAvailable >= minSpotsPerDay * numberOfDays) {

            for (int day = 0; day < numberOfDays; day++) {
                LocalDate dayDate = startDate.plusDays(day);
                List<TouristSpot> daySpots = dayWiseSpots.get(dayDate);

                while (daySpots.size() < minSpotsPerDay) {

                    LocalDate donorDay = null;
                    int maxSpots = 0;
                    for (int d = 0; d < numberOfDays; d++) {
                        LocalDate dDate = startDate.plusDays(d);
                        int count = dayWiseSpots.get(dDate).size();
                        if (count > maxSpots && count > minSpotsPerDay) {
                            maxSpots = count;
                            donorDay = dDate;
                        }
                    }
                    
                    if (donorDay != null && dayWiseSpots.get(donorDay).size() > minSpotsPerDay) {
                        List<TouristSpot> donorSpots = dayWiseSpots.get(donorDay);
                        if (!donorSpots.isEmpty()) {
                            TouristSpot movedSpot = donorSpots.remove(donorSpots.size() - 1);
                            daySpots.add(movedSpot);
                        } else {
                            break; // No more spots to move
                        }
                    } else {
                        break; // No days have extra spots
                    }
                }
            }
        }

        Set<Integer> distributedIds = new HashSet<>();
        for (List<TouristSpot> daySpots : dayWiseSpots.values()) {
            for (TouristSpot spot : daySpots) {
                distributedIds.add(spot.getId());
            }
        }

        for (TouristSpot spot : spots) {
            if (!distributedIds.contains(spot.getId())) {

                LocalDate bestDay = startDate;
                int minCount = Integer.MAX_VALUE;
                for (int d = 0; d < numberOfDays; d++) {
                    LocalDate dDate = startDate.plusDays(d);
                    int count = dayWiseSpots.get(dDate).size();
                    if (count < minCount) {
                        minCount = count;
                        bestDay = dDate;
                    }
                }
                dayWiseSpots.get(bestDay).add(spot);
            }
        }
        
        return dayWiseSpots;
    }
    
    /**
     * Calculate total duration for a day's spots including travel time
     * Uses adjusted durations to maximize spots shown
     */
    private double calculateDayDuration(List<TouristSpot> daySpots, int numberOfDays, int totalSpots) {
        if (daySpots.isEmpty()) {
            return 0;
        }
        
        double totalDuration = getAdjustedDuration(daySpots.get(0), numberOfDays, totalSpots);
        for (int i = 1; i < daySpots.size(); i++) {
            TouristSpot prev = daySpots.get(i - 1);
            TouristSpot curr = daySpots.get(i);
            int travelTime = calculateTravelTimeMinutes(prev, curr);
            totalDuration += getAdjustedDuration(curr, numberOfDays, totalSpots) + (travelTime / 60.0);
        }
        
        return totalDuration;
    }
    
    /**
     * Calculate total duration for a day's spots using original durations (for backwards compatibility)
     */
    private int calculateDayDuration(List<TouristSpot> daySpots) {
        if (daySpots.isEmpty()) {
            return 0;
        }
        
        double totalDuration = daySpots.get(0).getEstimatedDuration();
        for (int i = 1; i < daySpots.size(); i++) {
            TouristSpot prev = daySpots.get(i - 1);
            TouristSpot curr = daySpots.get(i);
            int travelTime = calculateTravelTimeMinutes(prev, curr);
            totalDuration += curr.getEstimatedDuration() + (travelTime / 60.0);
        }
        
        return (int) Math.ceil(totalDuration);
    }
    
    /**
     * Optimize route within a day using nearest-neighbor algorithm
     */
    public List<TouristSpot> optimizeRoute(List<TouristSpot> daySpots, Hotel hotel) {
        if (daySpots == null || daySpots.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<TouristSpot> optimizedRoute = new ArrayList<>();
        List<TouristSpot> unvisited = new ArrayList<>(daySpots);

        TouristSpot current = findClosestToHotel(unvisited, hotel);
        if (current == null && !unvisited.isEmpty()) {
            current = unvisited.get(0);
        }
        
        while (current != null) {
            optimizedRoute.add(current);
            unvisited.remove(current);
            
            if (unvisited.isEmpty()) break;

            current = findClosestSpot(unvisited, current);
        }
        
        return optimizedRoute;
    }
    
    /**
     * Calculate travel time in minutes between two spots
     * Rounds to nearest 15 minutes (00, 15, 30, 45)
     */
    public int calculateTravelTimeMinutes(TouristSpot from, TouristSpot to) {
        double distance = calculateDistance(
                from.getLatitude(), from.getLongitude(),
                to.getLatitude(), to.getLongitude()
        );


        int minutes = (int) Math.ceil((distance / 30.0) * 60);

        minutes = Math.max(15, minutes + TRAVEL_BUFFER_MINUTES);

        return roundToNearest15(minutes);
    }
    
    /**
     * Round minutes to nearest 15-minute interval (00, 15, 30, 45)
     */
    private int roundToNearest15(int minutes) {
        int remainder = minutes % 15;
        if (remainder == 0) {
            return minutes; // Already on 15-minute mark
        } else if (remainder <= 7) {
            return minutes - remainder; // Round down
        } else {
            return minutes + (15 - remainder); // Round up
        }
    }
    
    /**
     * Get travel information between two spots
     * Times rounded to nearest 15 minutes
     */
    public TravelInfo getTravelInfo(TouristSpot from, TouristSpot to) {
        double distance = calculateDistance(
                from.getLatitude(), from.getLongitude(),
                to.getLatitude(), to.getLongitude()
        );
        
        int autoMinutes = (int) Math.ceil((distance / 30.0) * 60); // Auto/Taxi: 30 km/h
        int metroMinutes = (int) Math.ceil((distance / 20.0) * 60); // Metro/Bus: 20 km/h
        int walkingMinutes = (int) Math.ceil((distance / 5.0) * 60); // Walking: 5 km/h

        autoMinutes = roundToNearest15(autoMinutes);
        metroMinutes = roundToNearest15(metroMinutes);
        walkingMinutes = roundToNearest15(walkingMinutes);

        autoMinutes = Math.max(15, autoMinutes);
        metroMinutes = Math.max(15, metroMinutes);
        walkingMinutes = Math.max(15, walkingMinutes);

        int autoCost = (int) Math.ceil(distance * 12); // ₹12 per km
        int metroCost = distance < 5 ? 20 : 40; // Fixed fare
        int walkingCost = 0;
        
        return new TravelInfo(distance, autoMinutes, metroMinutes, walkingMinutes, 
                             autoCost, metroCost, walkingCost);
    }
    
    /**
     * Get crowd level message for a spot on a specific date
     */
    public String getCrowdLevelMessage(TouristSpot spot, LocalDate date) {
        if (spot.getCrowdLevel() == null || spot.getCrowdLevel().isEmpty()) {
            return "";
        }
        
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        
        String crowdLevel = spot.getCrowdLevel();
        String message = "";
        
        if (crowdLevel.equalsIgnoreCase("High") || crowdLevel.equalsIgnoreCase("Very High")) {
            if (isWeekend) {
                message = "Very high crowd expected (weekend + popular spot)";
            } else {
                message = "High crowd expected - Visit early morning to avoid rush";
            }
        } else if (crowdLevel.equalsIgnoreCase("Medium")) {
            if (isWeekend) {
                message = "Moderate crowd expected (weekend)";
            } else {
                message = "Moderate crowd - Good time to visit";
            }
        } else {
            message = "Low crowd expected - Peaceful visit";
        }
        
        return message;
    }
    
    /**
     * Check if time slot conflicts with peak hours
     */
    public boolean isPeakHour(LocalTime time, TouristSpot spot) {
        if (spot.getPeakHours() == null || spot.getPeakHours().isEmpty()) {
            return false;
        }

        return time.isAfter(LocalTime.of(10, 0)) && time.isBefore(LocalTime.of(14, 0));
    }
    
    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in kilometers
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * Schedule time slots for spots in a day
     * All times rounded to 15-minute intervals (00, 15, 30, 45)
     * Groups spots by preferred time windows (morning, afternoon, evening)
     * Ensures no two spots have conflicting time slots
     * Uses adjusted durations to maximize spots shown
     */
    public Map<TouristSpot, TimeSlot> scheduleTimeSlots(List<TouristSpot> orderedSpots, Hotel hotel) {
        return scheduleTimeSlots(orderedSpots, hotel, 1, orderedSpots.size());
    }
    
    /**
     * Schedule time slots for spots in a day with duration adjustment parameters
     */
    public Map<TouristSpot, TimeSlot> scheduleTimeSlots(List<TouristSpot> orderedSpots, Hotel hotel, 
                                                         int numberOfDays, int totalSpots) {
        Map<TouristSpot, TimeSlot> schedule = new LinkedHashMap<>();
        
        if (orderedSpots == null || orderedSpots.isEmpty()) {
            return schedule;
        }

        List<TouristSpot> morningSpots = new ArrayList<>();
        List<TouristSpot> afternoonSpots = new ArrayList<>();
        List<TouristSpot> eveningSpots = new ArrayList<>();
        List<TouristSpot> anytimeSpots = new ArrayList<>();
        
        for (TouristSpot spot : orderedSpots) {
            String bestTime = spot.getBestVisitingTime();
            if (bestTime == null || bestTime.isEmpty()) {
                anytimeSpots.add(spot);
            } else {
                bestTime = bestTime.toLowerCase();
                if (bestTime.contains("early morning") || bestTime.contains("morning")) {
                    morningSpots.add(spot);
                } else if (bestTime.contains("evening")) {
                    eveningSpots.add(spot);
                } else if (bestTime.contains("afternoon")) {
                    afternoonSpots.add(spot);
                } else {
                    anytimeSpots.add(spot);
                }
            }
        }

        LocalTime currentTime = LocalTime.of(9, 0); // Start at 9 AM

        currentTime = scheduleSpotsInWindow(morningSpots, currentTime, LocalTime.of(9, 0), LocalTime.of(12, 0), 
                                            schedule, numberOfDays, totalSpots);

        if (currentTime.isBefore(LocalTime.of(13, 0))) {
            currentTime = LocalTime.of(13, 0);
        }

        currentTime = scheduleSpotsInWindow(afternoonSpots, currentTime, LocalTime.of(13, 0), LocalTime.of(16, 0), 
                                            schedule, numberOfDays, totalSpots);

        currentTime = scheduleSpotsInWindow(eveningSpots, currentTime, LocalTime.of(16, 0), LocalTime.of(19, 0), 
                                            schedule, numberOfDays, totalSpots);

        LocalTime maxEndTime = orderedSpots.size() > 5 || numberOfDays == 1 ? LocalTime.of(21, 0) : LocalTime.of(19, 0);
        scheduleSpotsInWindow(anytimeSpots, currentTime, LocalTime.of(9, 0), maxEndTime, 
                             schedule, numberOfDays, totalSpots);

        resolveConflicts(schedule, orderedSpots);

        LocalTime defaultStartTime = LocalTime.of(9, 0);
        for (TouristSpot spot : orderedSpots) {
            if (!schedule.containsKey(spot) || schedule.get(spot) == null) {

                LocalTime latestEndTime = defaultStartTime;
                for (TimeSlot existingSlot : schedule.values()) {
                    if (existingSlot != null && existingSlot.getEndTime().isAfter(latestEndTime)) {
                        latestEndTime = existingSlot.getEndTime();
                    }
                }

                LocalTime startTime = latestEndTime.plusMinutes(30); // 30 min buffer
                startTime = roundTimeTo15Minutes(startTime); // Round to 15-minute interval
                if (startTime.isAfter(LocalTime.of(21, 0))) {
                    startTime = defaultStartTime; // Reset if too late
                }
                
                double adjustedDuration = getAdjustedDuration(spot, numberOfDays, totalSpots);
                int durationHours = (int) adjustedDuration;
                int durationMinutes = (int) ((adjustedDuration - durationHours) * 60);
                LocalTime endTime = startTime.plusHours(durationHours).plusMinutes(durationMinutes);
                endTime = roundTimeTo15Minutes(endTime); // Round to 15-minute interval
                
                schedule.put(spot, new TimeSlot(startTime, endTime));
            }
        }
        
        return schedule;
    }
    
    /**
     * Schedule spots within a time window
     * Checks for conflicts before scheduling each spot
     * Uses adjusted durations to maximize spots shown
     */
    private LocalTime scheduleSpotsInWindow(List<TouristSpot> spots, LocalTime startTime, 
                                           LocalTime windowStart, LocalTime windowEnd, 
                                           Map<TouristSpot, TimeSlot> schedule,
                                           int numberOfDays, int totalSpots) {
        LocalTime currentTime = startTime;

        if (currentTime.isBefore(windowStart)) {
            currentTime = windowStart;
        }
        
        for (int i = 0; i < spots.size(); i++) {
            TouristSpot spot = spots.get(i);

            currentTime = roundTimeTo15Minutes(currentTime);

            double adjustedDuration = getAdjustedDuration(spot, numberOfDays, totalSpots);
            int adjustedDurationHours = (int) adjustedDuration; // Whole hours
            int adjustedDurationMinutes = (int) ((adjustedDuration - adjustedDurationHours) * 60); // Remaining minutes

            LocalTime spotEndTime = currentTime.plusHours(adjustedDurationHours).plusMinutes(adjustedDurationMinutes);
            if (spotEndTime.isAfter(windowEnd) && i < spots.size() - 1) {

                break;
            }

            LocalTime startTimeForSpot = adjustTimeForBestVisiting(spot, currentTime);

            if (startTimeForSpot.isBefore(windowStart)) {
                startTimeForSpot = windowStart;
            }
            LocalTime maxStartTime = windowEnd.minusHours(adjustedDurationHours).minusMinutes(adjustedDurationMinutes);
            if (startTimeForSpot.isAfter(maxStartTime)) {
                startTimeForSpot = maxStartTime;
            }
            
            startTimeForSpot = roundTimeTo15Minutes(startTimeForSpot);
            LocalTime endTime = startTimeForSpot.plusHours(adjustedDurationHours).plusMinutes(adjustedDurationMinutes);
            endTime = roundTimeTo15Minutes(endTime);

            TimeSlot proposedSlot = new TimeSlot(startTimeForSpot, endTime);
            if (hasConflict(proposedSlot, spot, schedule)) {

                TimeSlot adjustedSlot = findNextAvailableSlot(spot, currentTime, windowEnd, schedule, 
                                                             adjustedDurationHours, adjustedDurationMinutes);
                if (adjustedSlot != null) {
                    startTimeForSpot = adjustedSlot.getStartTime();
                    endTime = adjustedSlot.getEndTime();
                } else {

                    break;
                }
            }
            
            schedule.put(spot, new TimeSlot(startTimeForSpot, endTime));

            if (i < spots.size() - 1) {
                TouristSpot nextSpot = spots.get(i + 1);
                int travelMinutes = calculateTravelTimeMinutes(spot, nextSpot);
                currentTime = endTime.plusMinutes(travelMinutes);
                currentTime = roundTimeTo15Minutes(currentTime);

                if (currentTime.isAfter(LocalTime.of(23, 0))) {
                    break; // Too late, stop scheduling
                }
            } else {
                currentTime = endTime;
            }
        }
        
        return currentTime;
    }
    
    /**
     * Check if a time slot conflicts with any already scheduled slot
     * Two slots conflict if they overlap (even partially)
     */
    private boolean hasConflict(TimeSlot newSlot, TouristSpot newSpot, Map<TouristSpot, TimeSlot> schedule) {
        if (newSlot == null || schedule == null || schedule.isEmpty()) {
            return false;
        }
        
        LocalTime newStart = newSlot.getStartTime();
        LocalTime newEnd = newSlot.getEndTime();
        
        for (Map.Entry<TouristSpot, TimeSlot> entry : schedule.entrySet()) {
            TouristSpot existingSpot = entry.getKey();
            TimeSlot existingSlot = entry.getValue();

            if (existingSpot.equals(newSpot)) {
                continue;
            }
            
            LocalTime existingStart = existingSlot.getStartTime();
            LocalTime existingEnd = existingSlot.getEndTime();


            if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) {
                return true; // Conflict detected
            }
        }
        
        return false;
    }
    
    /**
     * Find the next available non-conflicting time slot for a spot
     * Uses adjusted duration parameters
     */
    private TimeSlot findNextAvailableSlot(TouristSpot spot, LocalTime startFrom, 
                                          LocalTime windowEnd, Map<TouristSpot, TimeSlot> schedule,
                                          int durationHours, int durationMinutes) {
        LocalTime currentTime = startFrom;
        LocalTime latestStart = windowEnd.minusHours(durationHours).minusMinutes(durationMinutes);

        while (currentTime.isBefore(latestStart) || currentTime.equals(latestStart)) {
            currentTime = roundTimeTo15Minutes(currentTime);
            LocalTime endTime = currentTime.plusHours(durationHours).plusMinutes(durationMinutes);
            endTime = roundTimeTo15Minutes(endTime);
            
            TimeSlot testSlot = new TimeSlot(currentTime, endTime);
            if (!hasConflict(testSlot, spot, schedule)) {
                return testSlot;
            }

            currentTime = currentTime.plusMinutes(15);

            if (currentTime.isAfter(LocalTime.of(23, 0))) {
                break;
            }
        }
        
        return null; // No available slot found
    }
    
    /**
     * Overloaded method for backwards compatibility
     */
    private TimeSlot findNextAvailableSlot(TouristSpot spot, LocalTime startFrom, 
                                          LocalTime windowEnd, Map<TouristSpot, TimeSlot> schedule) {
        double duration = spot.getEstimatedDuration();
        return findNextAvailableSlot(spot, startFrom, windowEnd, schedule, (int) Math.ceil(duration), 0);
    }
    
    /**
     * Resolve any conflicts in the schedule by adjusting time slots
     * This is a final validation pass to ensure no conflicts exist
     */
    private void resolveConflicts(Map<TouristSpot, TimeSlot> schedule, List<TouristSpot> orderedSpots) {
        if (schedule == null || schedule.isEmpty()) {
            return;
        }

        boolean hasConflicts = true;
        int maxIterations = 10; // Prevent infinite loops
        int iteration = 0;
        
        while (hasConflicts && iteration < maxIterations) {
            hasConflicts = false;
            iteration++;
            
            List<TouristSpot> spotsList = new ArrayList<>(schedule.keySet());
            
            for (int i = 0; i < spotsList.size(); i++) {
                TouristSpot spot1 = spotsList.get(i);
                TimeSlot slot1 = schedule.get(spot1);
                
                if (slot1 == null) continue;
                
                for (int j = i + 1; j < spotsList.size(); j++) {
                    TouristSpot spot2 = spotsList.get(j);
                    TimeSlot slot2 = schedule.get(spot2);
                    
                    if (slot2 == null) continue;

                    if (slotsOverlap(slot1, slot2)) {
                        hasConflicts = true;

                        LocalTime newStart = slot1.getEndTime();

                        newStart = newStart.plusMinutes(15); // Minimum 15-minute buffer
                        newStart = roundTimeTo15Minutes(newStart);
                        
                        LocalTime newEnd = newStart.plusMinutes((int) Math.round(spot2.getEstimatedDuration() * 60));
                        newEnd = roundTimeTo15Minutes(newEnd);

                        TimeSlot newSlot = new TimeSlot(newStart, newEnd);
                        if (!hasConflict(newSlot, spot2, schedule)) {
                            schedule.put(spot2, newSlot);
                        } else {

                            TimeSlot adjustedSlot = findNextAvailableSlot(spot2, newStart, LocalTime.of(19, 0), schedule);
                            if (adjustedSlot != null) {
                                schedule.put(spot2, adjustedSlot);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Check if two time slots overlap
     */
    private boolean slotsOverlap(TimeSlot slot1, TimeSlot slot2) {
        if (slot1 == null || slot2 == null) {
            return false;
        }
        
        LocalTime start1 = slot1.getStartTime();
        LocalTime end1 = slot1.getEndTime();
        LocalTime start2 = slot2.getStartTime();
        LocalTime end2 = slot2.getEndTime();

        return start1.isBefore(end2) && end1.isAfter(start2);
    }
    
    /**
     * Validate that a schedule has no conflicts
     * Returns true if schedule is conflict-free, false otherwise
     */
    public boolean validateSchedule(Map<TouristSpot, TimeSlot> schedule) {
        if (schedule == null || schedule.isEmpty()) {
            return true; // Empty schedule is valid
        }
        
        List<TouristSpot> spots = new ArrayList<>(schedule.keySet());
        
        for (int i = 0; i < spots.size(); i++) {
            TouristSpot spot1 = spots.get(i);
            TimeSlot slot1 = schedule.get(spot1);
            
            if (slot1 == null) continue;
            
            for (int j = i + 1; j < spots.size(); j++) {
                TouristSpot spot2 = spots.get(j);
                TimeSlot slot2 = schedule.get(spot2);
                
                if (slot2 == null) continue;
                
                if (slotsOverlap(slot1, slot2)) {
                    return false; // Conflict found
                }
            }
        }
        
        return true; // No conflicts found
    }
    
    /**
     * Round LocalTime to nearest 15-minute interval (00, 15, 30, 45)
     */
    private LocalTime roundTimeTo15Minutes(LocalTime time) {
        int minutes = time.getMinute();
        int roundedMinutes = roundToNearest15(minutes);
        
        if (roundedMinutes >= 60) {
            return time.plusHours(1).withMinute(0);
        } else {
            return time.withMinute(roundedMinutes).withSecond(0).withNano(0);
        }
    }
    
    /**
     * Check if spot is open on given date
     */
    public boolean checkOpeningHours(TouristSpot spot, LocalDate date) {
        if (spot.getClosedDays() == null || spot.getClosedDays().isEmpty()) {
            return true;
        }
        
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        String closedDays = spot.getClosedDays().toLowerCase();
        
        return !closedDays.contains(dayOfWeek.toString().toLowerCase());
    }
    
    /**
     * Get best time to visit spot based on bestVisitingTime field
     * Returns times on 15-minute intervals (00, 15, 30, 45)
     */
    public LocalTime getBestTimeForSpot(TouristSpot spot) {
        String bestTime = spot.getBestVisitingTime();
        if (bestTime == null || bestTime.isEmpty()) {
            return LocalTime.of(10, 0); // Default: 10:00 AM
        }
        
        bestTime = bestTime.toLowerCase();
        if (bestTime.contains("early morning") || bestTime.contains("morning")) {
            return LocalTime.of(9, 0); // 9:00 AM
        } else if (bestTime.contains("evening")) {
            return LocalTime.of(16, 0); // 4:00 PM
        } else if (bestTime.contains("afternoon")) {
            return LocalTime.of(14, 0); // 2:00 PM
        } else {
            return LocalTime.of(10, 0); // Default: 10:00 AM
        }
    }
    
    /**
     * Adjust time based on best visiting time preference
     * Returns times rounded to 15-minute intervals
     * Prioritizes best visiting time window
     */
    private LocalTime adjustTimeForBestVisiting(TouristSpot spot, LocalTime suggestedTime) {
        String bestTimeStr = spot.getBestVisitingTime();
        if (bestTimeStr == null || bestTimeStr.isEmpty()) {
            return suggestedTime; // No preference, use suggested time
        }
        
        bestTimeStr = bestTimeStr.toLowerCase();
        LocalTime bestTime = getBestTimeForSpot(spot);
        int suggestedHour = suggestedTime.getHour();

        if (bestTimeStr.contains("early morning") || bestTimeStr.contains("morning")) {

            if (suggestedHour >= 9 && suggestedHour < 12) {
                return suggestedTime;
            }

            return bestTime;
        }

        if (bestTimeStr.contains("evening")) {

            if (suggestedHour >= 16 && suggestedHour < 19) {
                return suggestedTime;
            }

            return bestTime;
        }

        if (bestTimeStr.contains("afternoon")) {

            if (suggestedHour >= 12 && suggestedHour < 16) {
                return suggestedTime;
            }

            return bestTime;
        }

        return suggestedTime;
    }
    
    /**
     * Find closest spot to hotel
     */
    private TouristSpot findClosestToHotel(List<TouristSpot> spots, Hotel hotel) {
        if (spots.isEmpty() || hotel == null) {
            return spots.isEmpty() ? null : spots.get(0);
        }


        return spots.stream()
                .min(Comparator.comparingDouble(TouristSpot::getDistanceFromCityCenter))
                .orElse(null);
    }
    
    /**
     * Find closest spot to given spot
     */
    private TouristSpot findClosestSpot(List<TouristSpot> spots, TouristSpot from) {
        if (spots.isEmpty()) return null;
        
        return spots.stream()
                .min(Comparator.comparingDouble(spot -> 
                    calculateDistance(from.getLatitude(), from.getLongitude(),
                                    spot.getLatitude(), spot.getLongitude())))
                .orElse(null);
    }
    
    /**
     * Inner class to represent time slot
     */
    public static class TimeSlot {
        private final LocalTime startTime;
        private final LocalTime endTime;
        
        public TimeSlot(LocalTime startTime, LocalTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        public LocalTime getStartTime() { return startTime; }
        public LocalTime getEndTime() { return endTime; }
    }
    
    /**
     * Travel information between spots
     */
    public static class TravelInfo {
        private final double distanceKm;
        private final int autoMinutes;
        private final int metroMinutes;
        private final int walkingMinutes;
        private final int autoCost;
        private final int metroCost;
        private final int walkingCost;
        
        public TravelInfo(double distanceKm, int autoMinutes, int metroMinutes, int walkingMinutes,
                         int autoCost, int metroCost, int walkingCost) {
            this.distanceKm = distanceKm;
            this.autoMinutes = autoMinutes;
            this.metroMinutes = metroMinutes;
            this.walkingMinutes = walkingMinutes;
            this.autoCost = autoCost;
            this.metroCost = metroCost;
            this.walkingCost = walkingCost;
        }
        
        public double getDistanceKm() { return distanceKm; }
        public int getAutoMinutes() { return autoMinutes; }
        public int getMetroMinutes() { return metroMinutes; }
        public int getWalkingMinutes() { return walkingMinutes; }
        public int getAutoCost() { return autoCost; }
        public int getMetroCost() { return metroCost; }
        public int getWalkingCost() { return walkingCost; }
        
        public String getBestOption() {
            if (distanceKm < 1.0) {
                return "Walking (" + walkingMinutes + " min, Free)";
            } else if (distanceKm < 5.0) {
                return "Auto/Taxi (" + autoMinutes + " min, ₹" + autoCost + ")";
            } else {
                return "Metro/Bus (" + metroMinutes + " min, ₹" + metroCost + ")";
            }
        }
    }
}

