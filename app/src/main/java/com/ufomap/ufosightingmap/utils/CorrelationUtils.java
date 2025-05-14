package com.ufomap.ufosightingmap.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Utility class for various statistical correlation calculations and related functions.
 * This class cannot be instantiated and all methods are static.
 */
public final class CorrelationUtils {

    // Private constructor to prevent instantiation of this utility class.
    private CorrelationUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    /**
     * Calculates the Pearson product-moment correlation coefficient.
     *
     * @param x Array of double values for the first variable.
     * @param y Array of double values for the second variable.
     * @return The Pearson correlation coefficient, or Double.NaN if calculation is not possible.
     * @throws IllegalArgumentException if x or y is null, if arrays have different lengths, or if length is less than 2.
     */
    public static double calculatePearsonCorrelationCoefficient(double[] x, double[] y) {
        validateInputArrays(x, y);
        int n = x.length;

        double sumX = 0.0;
        double sumY = 0.0;
        double sumXSquare = 0.0;
        double sumYSquare = 0.0;
        double sumXY = 0.0;

        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXSquare += x[i] * x[i];
            sumYSquare += y[i] * y[i];
            sumXY += x[i] * y[i];
        }

        double numerator = n * sumXY - sumX * sumY;
        double denominatorXPart = n * sumXSquare - sumX * sumX;
        double denominatorYPart = n * sumYSquare - sumY * sumY;

        // Check for zero variance in either dataset
        if (denominatorXPart <= 0 || denominatorYPart <= 0) {
            // If variance is zero for one or both, correlation is undefined or 0.
            // Returning NaN is a common way to indicate this.
            // If both have zero variance and are constant, perfect correlation is not meaningful.
            // If one has zero variance, it's often considered 0 or undefined.
            return Double.NaN;
        }

        return numerator / (Math.sqrt(denominatorXPart) * Math.sqrt(denominatorYPart));
    }

    /**
     * Calculates the Spearman rank correlation coefficient.
     * This is Pearson's correlation coefficient applied to the ranks of the data.
     *
     * @param x Array of double values for the first variable.
     * @param y Array of double values for the second variable.
     * @return The Spearman correlation coefficient, or Double.NaN if calculation is not possible.
     * @throws IllegalArgumentException if x or y is null, if arrays have different lengths, or if length is less than 2.
     */
    public static double calculateSpearmanCorrelationCoefficient(double[] x, double[] y) {
        validateInputArrays(x, y); // Validation is handled here

        double[] xRanks = getRanks(x);
        double[] yRanks = getRanks(y);

        return calculatePearsonCorrelationCoefficient(xRanks, yRanks);
    }

    /**
     * Calculates Kendall's Tau-b correlation coefficient.
     * This version handles ties.
     *
     * @param x Array of double values for the first variable.
     * @param y Array of double values for the second variable.
     * @return Kendall's Tau-b correlation coefficient, or Double.NaN if length < 2.
     * @throws IllegalArgumentException if x or y is null, or if arrays have different lengths.
     */
    public static double calculateKendallTauCoefficient(double[] x, double[] y) {
        validateInputArrays(x, y, false); // Allow length 0 or 1 for Kendall as it might return defined values for specific cases or be handled by formula
        int n = x.length;
        if (n < 2) {
            return Double.NaN; // Meaningful correlation requires at least 2 points
        }

        long concordantPairs = 0;
        long discordantPairs = 0;
        long tiesX = 0; // Number of pairs tied only in x
        long tiesY = 0; // Number of pairs tied only in y
        // long tiesXY = 0; // Not explicitly needed for Tau-b formula directly, but part of general tie consideration

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double x_i = x[i], x_j = x[j];
                double y_i = y[i], y_j = y[j];

                int signX = Double.compare(x_i, x_j);
                int signY = Double.compare(y_i, y_j);

                if (signX == 0 && signY == 0) {
                    // tiesXY++; // Tied on both
                } else if (signX == 0) {
                    tiesX++; // Tied on x only
                } else if (signY == 0) {
                    tiesY++; // Tied on y only
                } else if (signX == signY) { // (x_i < x_j and y_i < y_j) or (x_i > x_j and y_i > y_j)
                    concordantPairs++;
                } else { // (x_i < x_j and y_i > y_j) or (x_i > x_j and y_i < y_j)
                    discordantPairs++;
                }
            }
        }

        // Total number of pairs
        long totalPairs = (long) n * (n - 1) / 2;

        // For Tau-b, we need to account for ties in x and y separately for the denominator
        long n0 = totalPairs; // Number of pairs not tied on X or Y (used in some Tau versions)
        long n1 = 0; // Count of ties in x (sum of t_i * (t_i - 1) / 2 for groups of ties in x)
        long n2 = 0; // Count of ties in y (sum of u_j * (u_j - 1) / 2 for groups of ties in y)

        // Calculate n1 (ties in x)
        double[] sortedX = Arrays.copyOf(x, n);
        Arrays.sort(sortedX);
        for (int i = 0; i < n; ) {
            int j = i;
            while (j < n && sortedX[j] == sortedX[i]) {
                j++;
            }
            long count = j - i;
            if (count > 1) {
                n1 += count * (count - 1) / 2;
            }
            i = j;
        }

        // Calculate n2 (ties in y)
        double[] sortedY = Arrays.copyOf(y, n);
        Arrays.sort(sortedY);
        for (int i = 0; i < n; ) {
            int j = i;
            while (j < n && sortedY[j] == sortedY[i]) {
                j++;
            }
            long count = j - i;
            if (count > 1) {
                n2 += count * (count - 1) / 2;
            }
            i = j;
        }

        double denominator = Math.sqrt((double)(totalPairs - n1) * (totalPairs - n2));
        if (denominator == 0) {
            return Double.NaN; // Cannot compute if all pairs are tied in x or all in y
        }

        return (concordantPairs - discordantPairs) / denominator;
    }


    /**
     * Placeholder for Distance Correlation Coefficient.
     * The calculation is complex and typically involves matrix operations.
     *
     * @param x Array of double values for the first variable.
     * @param y Array of double values for the second variable.
     * @return Currently throws UnsupportedOperationException.
     * @throws IllegalArgumentException if x or y is null, if arrays have different lengths, or if length is less than 2.
     */
    public static double distanceCorrelationCoefficient(double[] x, double[] y) {
        validateInputArrays(x, y);
        // Implementation of distance correlation is non-trivial.
        // It involves:
        // 1. Constructing Euclidean distance matrices for x and y.
        // 2. Double-centering these distance matrices.
        // 3. Calculating dCov^2(x,y) (distance covariance squared) as the average entry-wise product of the centered matrices.
        // 4. Calculating dVar^2(x) and dVar^2(y) (distance variances squared).
        // 5. dCor(x,y) = sqrt(dCov^2(x,y) / sqrt(dVar^2(x) * dVar^2(y)))
        // This often requires libraries for efficient matrix manipulation or a very careful implementation.
        throw new UnsupportedOperationException("Distance Correlation Coefficient is complex and not yet implemented.");
    }

    /**
     * Calculates the Point-Biserial Correlation Coefficient.
     * Assumes y is a dichotomous variable (contains only two distinct values, typically 0 and 1).
     * x is the continuous variable.
     *
     * @param continuousData (x) Array of continuous variable data.
     * @param dichotomousData (y) Array of dichotomous variable data (e.g., 0s and 1s).
     * @return The Point-Biserial correlation coefficient, or Double.NaN if calculation is not possible.
     * @throws IllegalArgumentException if arrays are null, different lengths, length < 2, or dichotomousData is not binary.
     */
    public static double pointBiserialCorrelationCoefficient(double[] continuousData, double[] dichotomousData) {
        validateInputArrays(continuousData, dichotomousData);
        int n = continuousData.length;

        List<Double> group0 = new ArrayList<>();
        List<Double> group1 = new ArrayList<>();

        // Determine the two distinct values in dichotomousData
        double val1 = Double.NaN;
        double val2 = Double.NaN;

        for (double val : dichotomousData) {
            if (Double.isNaN(val1)) {
                val1 = val;
            } else if (val != val1 && Double.isNaN(val2)) {
                val2 = val;
            } else if (val != val1 && val != val2) {
                throw new IllegalArgumentException("Dichotomous data must contain only two distinct values.");
            }
        }
        if (Double.isNaN(val2)) { // Only one distinct value found
            throw new IllegalArgumentException("Dichotomous data must contain two distinct values for point-biserial correlation.");
        }


        for (int i = 0; i < n; i++) {
            if (dichotomousData[i] == val1) {
                group0.add(continuousData[i]);
            } else if (dichotomousData[i] == val2) { // Make sure it's the other value
                group1.add(continuousData[i]);
            }
            // Cases where dichotomousData[i] is neither val1 nor val2 are implicitly ignored if we assume clean binary input,
            // but a stricter check for only two values in dichotomousData should be done. (Added above)
        }

        if (group0.isEmpty() || group1.isEmpty()) {
            // One group is empty, correlation is undefined or not meaningful.
            return Double.NaN;
        }

        double n0 = group0.size();
        double n1 = group1.size();

        double mean0 = group0.stream().mapToDouble(d -> d).average().orElse(Double.NaN);
        double mean1 = group1.stream().mapToDouble(d -> d).average().orElse(Double.NaN);

        if (Double.isNaN(mean0) || Double.isNaN(mean1)) return Double.NaN;

        // Calculate overall standard deviation of the continuous variable
        double overallMean = Arrays.stream(continuousData).average().orElse(Double.NaN);
        if(Double.isNaN(overallMean)) return Double.NaN;

        double sumSqDiff = Arrays.stream(continuousData).map(d -> (d - overallMean) * (d - overallMean)).sum();
        double stdDev = Math.sqrt(sumSqDiff / n); // Population standard deviation for the formula

        if (stdDev == 0) {
            return Double.NaN; // No variance in continuous data
        }

        // Proportions
        double p = n1 / n;
        double q = n0 / n;

        return (mean1 - mean0) / stdDev * Math.sqrt(p * q);
    }

    /**
     * Performs simple linear regression (y = beta0 + beta1*x).
     *
     * @param x Array of independent variable values.
     * @param y Array of dependent variable values.
     * @return A double array where result[0] is the intercept (beta0) and result[1] is the slope (beta1).
     * Returns {NaN, NaN} if calculation is not possible.
     * @throws IllegalArgumentException if x or y is null, if arrays have different lengths, or if length is less than 2.
     */
    public static double[] linearRegression(double[] x, double[] y) {
        validateInputArrays(x, y);
        int n = x.length;

        double sumX = Arrays.stream(x).sum();
        double sumY = Arrays.stream(y).sum();
        double meanX = sumX / n;
        double meanY = sumY / n;

        double sumXY = 0.0;
        double sumXSquare = 0.0;

        for (int i = 0; i < n; i++) {
            sumXY += (x[i] * y[i]);
            sumXSquare += (x[i] * x[i]);
        }

        // Slope (beta1)
        double numeratorSlope = n * sumXY - sumX * sumY;
        double denominatorSlope = n * sumXSquare - sumX * sumX;

        if (denominatorSlope == 0) {
            // This means all x values are the same, slope is undefined (vertical line) or could be considered 0 if no relationship
            return new double[]{Double.NaN, Double.NaN};
        }
        double beta1 = numeratorSlope / denominatorSlope;

        // Intercept (beta0)
        double beta0 = meanY - beta1 * meanX;

        return new double[]{beta0, beta1};
    }

    /**
     * Placeholder for Polychoric Correlation Coefficient.
     * This is a complex statistical estimation typically requiring iterative methods
     * and assumptions about underlying normally distributed latent variables.
     *
     * @param x Array of ordinal data for the first variable.
     * @param y Array of ordinal data for the second variable.
     * @return Currently throws UnsupportedOperationException.
     * @throws IllegalArgumentException if x or y is null, if arrays have different lengths, or if length is less than 2.
     */
    public static double polychoricCorrelationCoefficient(double[] x, double[] y) {
        validateInputArrays(x, y); // Basic validation
        // Implementation of polychoric correlation is highly complex and often involves:
        // 1. Estimating thresholds for the ordinal categories based on assumed normal distributions.
        // 2. Using maximum likelihood estimation or other iterative numerical methods to find the correlation
        //    of the underlying bivariate normal distribution.
        // This is generally done using specialized statistical software (R, SPSS, SAS) or libraries (e.g., in Python or Java).
        throw new UnsupportedOperationException("Polychoric Correlation Coefficient is complex and typically requires specialized libraries.");
    }


    // --- Helper Methods ---

    /**
     * Validates input arrays for correlation/regression methods.
     *
     * @param x First array.
     * @param y Second array.
     * @param requireMinLengthTwo If true, requires arrays to have at least 2 elements.
     * @throws IllegalArgumentException if inputs are invalid.
     */
    private static void validateInputArrays(double[] x, double[] y, boolean requireMinLengthTwo) {
        if (x == null || y == null) {
            throw new IllegalArgumentException("Input arrays (x and y) must not be null.");
        }
        if (x.length != y.length) {
            throw new IllegalArgumentException("Input arrays (x and y) must have the same length.");
        }
        if (requireMinLengthTwo && x.length < 2) {
            throw new IllegalArgumentException("Input arrays must contain at least two data points for meaningful correlation.");
        }
        if (!requireMinLengthTwo && x.length == 0) { // For methods that might accept empty arrays or single points for specific results
            // Allow empty or single point if requireMinLengthTwo is false, but methods should handle this.
            // Or throw: throw new IllegalArgumentException("Input arrays cannot be empty.");
        }
    }

    /**
     * Overloaded validation method defaulting to requiring minimum length of 2.
     */
    private static void validateInputArrays(double[] x, double[] y) {
        validateInputArrays(x, y, true);
    }


    /**
     * Computes the ranks of the elements in an array.
     * Tied values are assigned the average of the ranks they would have occupied.
     *
     * @param data The input array of doubles.
     * @return An array of doubles representing the ranks.
     */
    private static double[] getRanks(double[] data) {
        int n = data.length;
        if (n == 0) {
            return new double[0];
        }

        // Create pairs of (value, originalIndex) to sort data while keeping track of original positions
        IndexedValue[] indexedValues = new IndexedValue[n];
        for (int i = 0; i < n; i++) {
            indexedValues[i] = new IndexedValue(data[i], i);
        }

        // Sort by value
        Arrays.sort(indexedValues, Comparator.comparingDouble(iv -> iv.value));

        double[] ranks = new double[n];
        int i = 0;
        while (i < n) {
            int j = i;
            // Find all items with the same value (ties)
            while (j < n - 1 && indexedValues[j].value == indexedValues[j + 1].value) {
                j++;
            }

            // Calculate the average rank for the tied group
            // The ranks they would occupy are from i+1 to j+1
            double averageRank = 0;
            for (int k = i; k <= j; k++) {
                averageRank += (k + 1);
            }
            averageRank /= (j - i + 1);

            // Assign the average rank to all tied items
            for (int k = i; k <= j; k++) {
                ranks[indexedValues[k].originalIndex] = averageRank;
            }
            i = j + 1; // Move to the next untied item or next group of ties
        }
        return ranks;
    }

    /**
     * Helper class to store a value and its original index. Used for ranking.
     */
    private static class IndexedValue {
        final double value;
        final int originalIndex;

        IndexedValue(double value, int originalIndex) {
            this.value = value;
            this.originalIndex = originalIndex;
        }
    }

    // Note on method consolidation:
    // - `calculateKendallCorrelationCoefficient` and `calculateKendallTauCoefficient` are effectively the same.
    //   I've implemented `calculateKendallTauCoefficient` as it's the more common name for the coefficient.
    // - `calculateSpearmanCorrelationCoefficient` and `spearmanRankCorrelationCoefficient` are also the same.
    //   I've implemented `calculateSpearmanCorrelationCoefficient`.
    // You can remove the duplicate method signatures if you wish.

    // Consider removing the unused imports if they are no longer needed:
    // import java.util.Collections;
    // import java.util.List;
    // import java.util.stream.Collectors;
    // (Kept `Arrays` and `ArrayList`, `Comparator` for now as they are used in helpers)
}