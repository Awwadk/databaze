package com.dlightplanner.services;

import java.time.LocalDate;
import java.util.Random;

/**
 * Weather Service - Provides weather information for itinerary planning
 * Currently uses mock data, can be replaced with real API later
 */
public class WeatherService {
    
    private static final Random random = new Random();
    
    /**
     * Get weather forecast for a specific date and location
     */
    public WeatherForecast getWeatherForecast(String destinationCode, LocalDate date) {


        
        String condition = generateWeatherCondition(destinationCode, date);
        int temperature = generateTemperature(destinationCode, date);
        int humidity = 50 + random.nextInt(30); // 50-80%
        double precipitation = generatePrecipitation(condition);
        
        return new WeatherForecast(condition, temperature, humidity, precipitation, date);
    }
    
    /**
     * Generate weather condition based on destination and date
     */
    private String generateWeatherCondition(String destinationCode, LocalDate date) {
        int month = date.getMonthValue();

        if (destinationCode.equals("KUU") || destinationCode.equals("SLV") || destinationCode.equals("IXB")) {
            if (month >= 6 && month <= 9) {
                return random.nextDouble() < 0.6 ? "Rainy" : "Cloudy";
            } else if (month >= 11 || month <= 2) {
                return random.nextDouble() < 0.3 ? "Snow" : "Clear";
            } else {
                return random.nextDouble() < 0.2 ? "Cloudy" : "Clear";
            }
        }

        if (destinationCode.equals("GOI") || destinationCode.equals("IXZ")) {
            if (month >= 6 && month <= 9) {
                return random.nextDouble() < 0.7 ? "Rainy" : "Cloudy";
            } else {
                return random.nextDouble() < 0.1 ? "Cloudy" : "Clear";
            }
        }

        if (month >= 6 && month <= 9) {
            return random.nextDouble() < 0.5 ? "Rainy" : "Cloudy";
        }

        if (month >= 11 || month <= 2) {
            return random.nextDouble() < 0.15 ? "Cloudy" : "Clear";
        }

        return random.nextDouble() < 0.1 ? "Cloudy" : "Clear";
    }
    
    /**
     * Generate temperature based on destination and date
     */
    private int generateTemperature(String destinationCode, LocalDate date) {
        int month = date.getMonthValue();
        int baseTemp;

        if (destinationCode.equals("KUU") || destinationCode.equals("SLV") || destinationCode.equals("IXB")) {
            baseTemp = (month >= 11 || month <= 2) ? 5 : (month >= 6 && month <= 9) ? 18 : 22;
            return baseTemp + random.nextInt(8) - 4;
        }

        if (destinationCode.equals("GOI") || destinationCode.equals("IXZ")) {
            baseTemp = (month >= 11 || month <= 2) ? 28 : (month >= 6 && month <= 9) ? 26 : 32;
            return baseTemp + random.nextInt(5) - 2;
        }

        if (month >= 11 || month <= 2) {
            baseTemp = 20; // Winter
        } else if (month >= 3 && month <= 5) {
            baseTemp = 35; // Summer
        } else if (month >= 6 && month <= 9) {
            baseTemp = 28; // Monsoon
        } else {
            baseTemp = 25; // Post-monsoon
        }
        
        return baseTemp + random.nextInt(8) - 4;
    }
    
    /**
     * Generate precipitation probability
     */
    private double generatePrecipitation(String condition) {
        switch (condition) {
            case "Rainy":
                return 70 + random.nextDouble() * 30; // 70-100%
            case "Cloudy":
                return 20 + random.nextDouble() * 30; // 20-50%
            case "Snow":
                return 60 + random.nextDouble() * 40; // 60-100%
            default:
                return random.nextDouble() * 10; // 0-10%
        }
    }
    
    /**
     * Check if weather is suitable for outdoor activities
     */
    public boolean isSuitableForOutdoor(WeatherForecast forecast) {
        return !forecast.getCondition().equals("Rainy") && 
               !forecast.getCondition().equals("Snow") &&
               forecast.getPrecipitation() < 30;
    }
    
    /**
     * Get weather recommendation message
     */
    public String getWeatherRecommendation(WeatherForecast forecast) {
        if (forecast.getCondition().equals("Rainy")) {
            return "Rain expected - Consider indoor activities or carry umbrella";
        } else if (forecast.getCondition().equals("Snow")) {
            return "Snow expected - Dress warmly and check road conditions";
        } else if (forecast.getCondition().equals("Cloudy")) {
            return "Cloudy weather - Perfect for outdoor activities, less heat";
        } else if (forecast.getTemperature() > 35) {
            return "Hot weather - Stay hydrated, visit early morning or evening";
        } else {
            return "Perfect weather for outdoor activities";
        }
    }
    
    /**
     * Weather Forecast Data Class
     */
    public static class WeatherForecast {
        private final String condition; // Clear, Cloudy, Rainy, Snow
        private final int temperature; // in Celsius
        private final int humidity; // percentage
        private final double precipitation; // probability percentage
        private final LocalDate date;
        
        public WeatherForecast(String condition, int temperature, int humidity, 
                             double precipitation, LocalDate date) {
            this.condition = condition;
            this.temperature = temperature;
            this.humidity = humidity;
            this.precipitation = precipitation;
            this.date = date;
        }
        
        public String getCondition() { return condition; }
        public int getTemperature() { return temperature; }
        public int getHumidity() { return humidity; }
        public double getPrecipitation() { return precipitation; }
        public LocalDate getDate() { return date; }
        
        public String getConditionIcon() {
            switch (condition) {
                case "Clear": return "☀️";
                case "Cloudy": return "☁️";
                case "Rainy": return "🌧️";
                case "Snow": return "❄️";
                default: return "🌤️";
            }
        }
    }
}

