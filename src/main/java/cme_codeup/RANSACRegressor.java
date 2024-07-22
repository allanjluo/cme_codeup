package cme_codeup;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.ArrayRealVector;

import java.util.ArrayList;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class RANSACRegressor {

    private double desiredProbability;
    private double[] bestModel;
    private Deque<Double> dataQueue;
    private List<Integer> inlierIndices;
    private int maxTrials;
    private double residualThreshold;
    private boolean useDefaultMaxTrials;
    private boolean useDefaultResidualThreshold;
    private int windowSize;
    private int lastFitLength; // To keep track of the length of the last fitted array

    // Constructor with desiredProbability and default maxTrials and residualThreshold
    public RANSACRegressor(double desiredProbability, int windowSize) {
        this.desiredProbability = desiredProbability;
        this.windowSize = windowSize;
        this.dataQueue = new ArrayDeque<>(windowSize);
        this.useDefaultMaxTrials = true;
        this.useDefaultResidualThreshold = true;
        this.lastFitLength = 0;
    }

    // Constructor with specified maxTrials and residualThreshold
    public RANSACRegressor(int maxTrials, double residualThreshold, int windowSize) {
        this.maxTrials = maxTrials;
        this.residualThreshold = residualThreshold;
        this.windowSize = windowSize;
        this.dataQueue = new ArrayDeque<>(windowSize);
        this.useDefaultMaxTrials = false;
        this.useDefaultResidualThreshold = false;
        this.lastFitLength = 0;
    }

    public void append(double value) {
        if (dataQueue.size() >= windowSize) {
            dataQueue.poll();
        }
        dataQueue.offer(value);
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

    public void fit() {
        if (dataQueue.isEmpty()) {
            throw new IllegalStateException("No data to fit the model.");
        }

        double[] y = dataQueue.stream().mapToDouble(Double::doubleValue).toArray();
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

        // Construct the design matrix X
        double[][] X = new double[n][2];
        for (int i = 0; i < n; i++) {
            X[i][0] = 1;  // Intercept term
            X[i][1] = x[i];
        }

        // Convert to RealMatrix
        RealMatrix XMatrix = new Array2DRowRealMatrix(X);

        // Convert y to RealVector
        RealVector yVector = new ArrayRealVector(y);

        // Compute X^T * X
        RealMatrix XT = XMatrix.transpose();
        RealMatrix XTX = XT.multiply(XMatrix);

        // Compute (X^T * X)^-1
        RealMatrix XTXInv = new LUDecomposition(XTX).getSolver().getInverse();

        // Compute X^T * y
        RealVector XTy = XT.operate(yVector);

        // Compute theta = (X^T * X)^-1 * X^T * y
        RealVector theta = XTXInv.operate(XTy);

        // Return as double array
        return theta.toArray();
    }

    // Simplified version for single y array
    private double[] fitLinearModel(double[] y) {
        int n = y.length;

        // Construct the design matrix X
        double[][] X = new double[n][2];
        for (int i = 0; i < n; i++) {
            X[i][0] = 1;  // Intercept term
            X[i][1] = i + 1;
        }

        // Convert to RealMatrix
        RealMatrix XMatrix = new Array2DRowRealMatrix(X);

        // Convert y to RealVector
        RealVector yVector = new ArrayRealVector(y);

        // Compute X^T * X
        RealMatrix XT = XMatrix.transpose();
        RealMatrix XTX = XT.multiply(XMatrix);

        // Compute (X^T * X)^-1
        RealMatrix XTXInv = new LUDecomposition(XTX).getSolver().getInverse();

        // Compute X^T * y
        RealVector XTy = XT.operate(yVector);

        // Compute theta = (X^T * X)^-1 * X^T * y
        RealVector theta = XTXInv.operate(XTy);

        // Return as double array
        return theta.toArray();
    }

    public double[] getParams() {
        if (bestModel == null) {
            throw new IllegalStateException("Model has not been fitted or no valid model found.");
        }
        return bestModel;
    }

    public static void main(String[] args) {
        RANSACRegressor ransacDefault = new RANSACRegressor(0.99, 10); // Desired probability of 99% and window size of 10

        double[] yValues = {3.4, 2.5, 3.5, 4.5, 3.0, 2.7, 3.5, 8.5, 4.5, 2.0};
        for (double y : yValues) {
            ransacDefault.append(y);
        }

        ransacDefault.fit();

        double[] params = ransacDefault.getParams();
        System.out.println("Slope: " + params[1]);
        System.out.println("Intercept: " + params[0]);

        double[] testPoints = {1, 7, 10};
        double[] predictionsDefault = ransacDefault.predict(testPoints);
        for (int i = 0; i < testPoints.length; i++) {
            System.out.println("Prediction with default values for time " + testPoints[i] + ": " + predictionsDefault[i]);
        }

        double[] nextPredictionsDefault = ransacDefault.next(2);
        for (int i = 0; i < nextPredictionsDefault.length; i++) {
            System.out.println("Next prediction with default values for time " + (yValues.length + i + 1) + ": " + nextPredictionsDefault[i]);
        }
    }
}
