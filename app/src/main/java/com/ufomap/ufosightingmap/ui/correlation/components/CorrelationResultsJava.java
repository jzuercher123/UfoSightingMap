package com.ufomap.ufosightingmap.ui.correlation.components;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ufomap.ufosightingmap.utils.CorrelationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java implementation of correlation analysis for UFO sightings data.
 * This class provides methods for calculating different types of correlations,
 * and presenting the results in a format that can be visualized by the UI components.
 */
public class CorrelationResultsJava {

    /**
     * Types of correlation analysis available
     */
    public enum CorrelationType {
        PEARSON,
        SPEARMAN,
        KENDALL_TAU,
        DISTANCE,
        POINT_BISERIAL
    }

    /**
     * Types of factors that can be correlated with UFO sightings
     */
    public enum CorrelationFactor {
        MILITARY_BASE_DISTANCE,
        POPULATION_DENSITY,
        ASTRONOMICAL_EVENT,
        WEATHER_CONDITION,
        TIME_OF_DAY,
        SEASON,
        SHAPE,
        DURATION
    }

    /**
     * Calculates the correlation between two data arrays using the specified method.
     *
     * @param xData The independent variable data points
     * @param yData The dependent variable data points
     * @param type The type of correlation analysis to perform
     * @return The correlation coefficient, or Double.NaN if calculation failed
     */
    public static double calculateCorrelation(double[] xData, double[] yData, CorrelationType type) {
        try {
            switch (type) {
                case PEARSON:
                    return CorrelationUtils.calculatePearsonCorrelationCoefficient(xData, yData);
                case SPEARMAN:
                    return CorrelationUtils.calculateSpearmanCorrelationCoefficient(xData, yData);
                case KENDALL_TAU:
                    return CorrelationUtils.calculateKendallTauCoefficient(xData, yData);
                case DISTANCE:
                    // This requires our own implementation because CorrelationUtils might not have it
                    return calculateDistanceCorrelation(xData, yData);
                case POINT_BISERIAL:
                    return CorrelationUtils.pointBiserialCorrelationCoefficient(xData, yData);
                default:
                    return Double.NaN;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Double.NaN;
        }
    }

    /**
     * Simple implementation of distance correlation.
     * This is a placeholder for a more sophisticated implementation.
     */
    private static double calculateDistanceCorrelation(double[] xData, double[] yData) {
        // This should be replaced with a proper distance correlation implementation
        return 0.0;
    }

    /**
     * Class to hold correlation analysis results
     */
    public static class CorrelationResult {
        private final CorrelationType type;
        private final CorrelationFactor factor;
        private final double coefficient;
        private final double pValue;
        private final int sampleSize;
        private final String interpretation;
        private final List<ChartPoint> dataPoints;

        public CorrelationResult(
                CorrelationType type,
                CorrelationFactor factor,
                double coefficient,
                double pValue,
                int sampleSize,
                String interpretation,
                List<ChartPoint> dataPoints) {
            this.type = type;
            this.factor = factor;
            this.coefficient = coefficient;
            this.pValue = pValue;
            this.sampleSize = sampleSize;
            this.interpretation = interpretation;
            this.dataPoints = dataPoints;
        }

        public CorrelationType getType() {
            return type;
        }

        public CorrelationFactor getFactor() {
            return factor;
        }

        public double getCoefficient() {
            return coefficient;
        }

        public double getPValue() {
            return pValue;
        }

        public int getSampleSize() {
            return sampleSize;
        }

        public String getInterpretation() {
            return interpretation;
        }

        public List<ChartPoint> getDataPoints() {
            return dataPoints;
        }

        /**
         * Converts this result to a string rating based on the correlation coefficient
         */
        public String getStrengthRating() {
            double absCoef = Math.abs(coefficient);
            if (absCoef >= 0.8) return "Very Strong";
            if (absCoef >= 0.6) return "Strong";
            if (absCoef >= 0.4) return "Moderate";
            if (absCoef >= 0.2) return "Weak";
            return "Very Weak";
        }

        /**
         * Determines if the correlation is positive, negative, or neutral
         */
        public String getDirection() {
            if (coefficient > 0.05) return "Positive";
            if (coefficient < -0.05) return "Negative";
            return "Neutral";
        }

        /**
         * Determines if the correlation is statistically significant based on p-value
         */
        public boolean isSignificant() {
            return pValue < 0.05; // Standard significance level
        }
    }

    /**
     * Data point for correlation charts
     */
    public static class ChartPoint {
        private final double x;
        private final double y;
        private final String label;

        public ChartPoint(double x, double y, @Nullable String label) {
            this.x = x;
            this.y = y;
            this.label = label;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        @Nullable
        public String getLabel() {
            return label;
        }
    }

    /**
     * Builder for creating correlation analysis
     */
    public static class CorrelationAnalysisBuilder {
        private final CorrelationType type;
        private final CorrelationFactor factor;
        private double[] xData;
        private double[] yData;
        private final List<ChartPoint> dataPoints = new ArrayList<>();
        private int sampleSize;

        public CorrelationAnalysisBuilder(CorrelationType type, CorrelationFactor factor) {
            this.type = type;
            this.factor = factor;
        }

        public CorrelationAnalysisBuilder withData(double[] xData, double[] yData) {
            this.xData = xData;
            this.yData = yData;
            this.sampleSize = Math.min(xData.length, yData.length);

            // Create chart points from data
            for (int i = 0; i < this.sampleSize; i++) {
                dataPoints.add(new ChartPoint(xData[i], yData[i], null));
            }

            return this;
        }

        public CorrelationAnalysisBuilder withDataPoints(List<ChartPoint> dataPoints) {
            this.dataPoints.clear();
            this.dataPoints.addAll(dataPoints);

            // Extract xData and yData from points
            this.sampleSize = dataPoints.size();
            this.xData = new double[sampleSize];
            this.yData = new double[sampleSize];

            for (int i = 0; i < sampleSize; i++) {
                ChartPoint point = dataPoints.get(i);
                xData[i] = point.getX();
                yData[i] = point.getY();
            }

            return this;
        }

        public CorrelationResult build() {
            if (xData == null || yData == null || xData.length == 0 || yData.length == 0) {
                throw new IllegalStateException("Data must be provided for correlation analysis");
            }

            double coefficient = calculateCorrelation(xData, yData, type);

            // For simplicity, we're using a placeholder p-value calculation
            double pValue = calculatePlaceholderPValue(coefficient, sampleSize);

            String interpretation = generateInterpretation(coefficient, pValue, type, factor);

            return new CorrelationResult(
                    type,
                    factor,
                    coefficient,
                    pValue,
                    sampleSize,
                    interpretation,
                    dataPoints
            );
        }

        private double calculatePlaceholderPValue(double coefficient, int sampleSize) {
            // This is a simple approximation, not statistically rigorous
            double absCoef = Math.abs(coefficient);
            if (sampleSize < 10) return 0.2;
            if (sampleSize < 30) return absCoef < 0.4 ? 0.1 : 0.05;
            return absCoef < 0.2 ? 0.1 : 0.01;
        }

        private String generateInterpretation(
                double coefficient,
                double pValue,
                CorrelationType type,
                CorrelationFactor factor
        ) {
            StringBuilder sb = new StringBuilder();

            // Strength description
            double absCoef = Math.abs(coefficient);
            String strength;
            if (absCoef >= 0.8) strength = "very strong";
            else if (absCoef >= 0.6) strength = "strong";
            else if (absCoef >= 0.4) strength = "moderate";
            else if (absCoef >= 0.2) strength = "weak";
            else strength = "very weak";

            // Direction
            String direction = coefficient > 0 ? "positive" : "negative";

            // Significance
            String significance = pValue < 0.05 ? "statistically significant" : "not statistically significant";

            // Create interpretation
            sb.append(String.format("The analysis shows a %s %s correlation (%s) between UFO sightings and %s. ",
                    strength, direction, significance, factorToString(factor)));

            // Add factor-specific interpretation
            switch (factor) {
                case MILITARY_BASE_DISTANCE:
                    sb.append(coefficient < 0 ?
                            "This suggests that UFO sightings tend to occur more frequently near military bases." :
                            "This suggests that UFO sightings do not appear to cluster around military installations.");
                    break;
                case POPULATION_DENSITY:
                    sb.append(coefficient > 0 ?
                            "This suggests that UFO sightings are more common in areas with higher population density." :
                            "This suggests that population density doesn't strongly predict UFO sighting frequency.");
                    break;
                case ASTRONOMICAL_EVENT:
                    sb.append(coefficient > 0 ?
                            "This suggests an increased likelihood of UFO reports during astronomical events." :
                            "This suggests that astronomical events don't appear to trigger UFO sightings.");
                    break;
                case WEATHER_CONDITION:
                    sb.append("The data indicates that certain weather conditions may influence sighting reports.");
                    break;
                default:
                    // No specific interpretation for other factors
                    break;
            }

            return sb.toString();
        }

        private String factorToString(CorrelationFactor factor) {
            switch (factor) {
                case MILITARY_BASE_DISTANCE:
                    return "proximity to military bases";
                case POPULATION_DENSITY:
                    return "population density";
                case ASTRONOMICAL_EVENT:
                    return "astronomical events";
                case WEATHER_CONDITION:
                    return "weather conditions";
                case TIME_OF_DAY:
                    return "time of day";
                case SEASON:
                    return "seasonal patterns";
                case SHAPE:
                    return "sighting shape";
                case DURATION:
                    return "sighting duration";
                default:
                    return "the selected factor";
            }
        }
    }
}