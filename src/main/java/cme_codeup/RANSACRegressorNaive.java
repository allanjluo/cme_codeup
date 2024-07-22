package cme_codeup;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RANSACRegressorNaive {

    private double desiredProbability;
    private double[] bestModel;
    private List<Integer> inlierIndices;
    private int maxTrials;
    private double residualThreshold;
    private boolean useDefaultMaxTrials;
    private boolean useDefaultResidualThreshold;
    private int lastFitLength; // To keep track of the length of the last fitted array

    // Constructor with desiredProbability and default maxTrials and residualThreshold
    public RANSACRegressorNaive(double desiredProbability) {
        this.desiredProbability = desiredProbability;
        this.useDefaultMaxTrials = true;
        this.useDefaultResidualThreshold = true;
        this.lastFitLength = 0;
    }

    // Constructor with specified maxTrials and residualThreshold
    public RANSACRegressorNaive(int maxTrials, double residualThreshold) {
        this.maxTrials = maxTrials;
        this.residualThreshold = residualThreshold;
        this.useDefaultMaxTrials = false;
        this.useDefaultResidualThreshold = false;
        this.lastFitLength = 0;
    }

    private void calculateMaxTrials(int nSamples, double outlierRatio, int minSamples) {
        if (useDefaultMaxTrials) {
            this.maxTrials = (int) Math.ceil(Math.log(1 - desiredProbability) / Math.log(1 - Math.pow(1 - outlierRatio, minSamples)));
            System.out.println("Calculated maxTrials: " + maxTrials);
        }
    }

    private void calculateResidualThreshold(double[] y) {
        if (useDefaultResidualThreshold) {
            double[] initialModel = fitLinearModel(y);
            double sumSquaredResiduals = 0;
            for (int i = 0; i < y.length; i++) {
                double residual = y[i] - predict(new double[]{i + 1}, initialModel);
                sumSquaredResiduals += residual * residual;
            }
            double variance = sumSquaredResiduals / y.length;
            this.residualThreshold = Math.sqrt(variance);
            System.out.println("Calculated residualThreshold: " + residualThreshold);

            // If the calculated residual threshold is zero, use a default small value
            if (this.residualThreshold == 0) {
                this.residualThreshold = 1e-6;
                System.out.println("Residual threshold was zero, set to default small value: " + this.residualThreshold);
            }
        }
    }

    public void fit(double[] y) {
        int nSamples = y.length;
        int minSamples = 2; // Minimum two points needed to fit a line
        double outlierRatio = 0.5; // Assuming half the data might be outliers

        // Calculate maxTrials if not provided
        calculateMaxTrials(nSamples, outlierRatio, minSamples);

        // Calculate residualThreshold if not provided
        calculateResidualThreshold(y);

        Random random = new Random();

        bestModel = null;
        int bestInlierCount = 0;

        for (int trial = 0; trial < maxTrials; trial++) {
            // Randomly select a subset of the data
            int[] subsetIndices = random.ints(0, nSamples).distinct().limit(minSamples).toArray();
            double[] xSubset = new double[subsetIndices.length];
            double[] ySubset = new double[subsetIndices.length];
            for (int i = 0; i < subsetIndices.length; i++) {
                xSubset[i] = subsetIndices[i] + 1;
                ySubset[i] = y[subsetIndices[i]];
            }

            // Fit a linear model to the subset
            double[] model = fitLinearModel(xSubset, ySubset);

            // Count inliers
            List<Integer> inliers = new ArrayList<>();
            for (int i = 0; i < nSamples; i++) {
                double residual = Math.abs(y[i] - predict(new double[]{i + 1}, model));
                if (residual < residualThreshold) {
                    inliers.add(i);
                }
            }

            // Update the best model if we have found a better one
            if (inliers.size() > bestInlierCount) {
                bestInlierCount = inliers.size();
                bestModel = model;
                inlierIndices = inliers;
            }
        }

        // Refit the best model using all inliers
        if (bestModel != null) {
            double[] xInliers = new double[inlierIndices.size()];
            double[] yInliers = new double[inlierIndices.size()];
            for (int i = 0; i < inlierIndices.size(); i++) {
                xInliers[i] = inlierIndices.get(i) + 1;
                yInliers[i] = y[inlierIndices.get(i)];
            }
            bestModel = fitLinearModel(xInliers, yInliers);
        } else {
            System.out.println("No valid model found. Try adjusting parameters.");
        }

        // Update the lastFitLength
        lastFitLength = y.length;
    }

    public double[] predict(double... xValues) {
        if (bestModel == null) {
            throw new IllegalStateException("Model has not been fitted or no valid model found.");
        }
        double[] predictions = new double[xValues.length];
        for (int i = 0; i < xValues.length; i++) {
            predictions[i] = predict(new double[]{xValues[i]}, bestModel);
        }
        return predictions;
    }

    public double[] next(int x) {
        if (bestModel == null) {
            throw new IllegalStateException("Model has not been fitted or no valid model found.");
        }
        double[] predictions = new double[x];
        for (int i = 0; i < x; i++) {
            predictions[i] = predict(new double[]{lastFitLength + i + 1}, bestModel);
        }
        return predictions;
    }

    private double predict(double[] x, double[] model) {
        return model[0] + model[1] * x[0];
    }

    private double[] fitLinearModel(double[] x, double[] y) {
        int n = x.length;
        double xSum = 0, ySum = 0, xySum = 0, xxSum = 0;
        for (int i = 0; i < n; i++) {
            xSum += x[i];
            ySum += y[i];
            xySum += x[i] * y[i];
            xxSum += x[i] * x[i];
        }
        double[] theta = new double[2];
        theta[1] = (n * xySum - xSum * ySum) / (n * xxSum - xSum * xSum);
        theta[0] = (ySum - theta[1] * xSum) / n;
        return theta;
    }

    private double[] fitLinearModel(double[] y) {
        int n = y.length;
        double xSum = 0, ySum = 0, xySum = 0, xxSum = 0;
        for (int i = 0; i < n; i++) {
            double x = i + 1;
            xSum += x;
            ySum += y[i];
            xySum += x * y[i];
            xxSum += x * x;
        }
        double[] theta = new double[2];
        theta[1] = (n * xySum - xSum * ySum) / (n * xxSum - xSum * xSum);
        theta[0] = (ySum - theta[1] * xSum) / n;
        return theta;
    }

    public static void main(String[] args) {
        double[] y = {
            3.4, 2.5, 3.5, 4.5, 3.0,
            2.7, 3.5, 8.5, 4.5, 2.0,
            3.6, 5.2, 7.0, 2.9, 3.8
        };

        // Using default maxTrials and residualThreshold
        RANSACRegressorNaive ransacDefault = new RANSACRegressorNaive(0.99); // Desired probability of 99%
        ransacDefault.fit(y);

        double[] testPoints = {1, 7, 10, 15};
        double[] predictionsDefault = ransacDefault.predict(testPoints);
        for (int i = 0; i < testPoints.length; i++) {
            System.out.println("Prediction with default values for time " + testPoints[i] + ": " + predictionsDefault[i]);
        }

        double[] nextPredictionsDefault = ransacDefault.next(2);
        for (int i = 0; i < nextPredictionsDefault.length; i++) {
            System.out.println("Next prediction with default values for time " + (y.length + i + 1) + ": " + nextPredictionsDefault[i]);
        }

        // Using specified maxTrials and residualThreshold
        RANSACRegressorNaive ransacSpecified = new RANSACRegressorNaive(100, 1.0); // maxTrials 100, residualThreshold 1.0
        ransacSpecified.fit(y);

        double[] predictionsSpecified = ransacSpecified.predict(testPoints);
        for (int i = 0; i < testPoints.length; i++) {
            System.out.println("Prediction with specified values for time " + testPoints[i] + ": " + predictionsSpecified[i]);
        }

        double[] nextPredictionsSpecified = ransacSpecified.next(2);
        for (int i = 0; i < nextPredictionsSpecified.length; i++) {
            System.out.println("Next prediction with specified values for time " + (y.length + i + 1) + ": " + nextPredictionsSpecified[i]);
        }
    }
}
