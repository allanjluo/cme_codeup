package cme_codeup;

public class EMA {

    // missing logger
    private final int period;
    private double currentEMA;
    private final double K;
    private final double SMOOTHING_COEFFICIENT = 8;
    private int datapoints = 0;
    private double sum = 0;

    // constructor to initalize period
    public EMA(int period) {
        this.period = period;
        this.currentEMA = 0.0;
        K = SMOOTHING_COEFFICIENT / (double) (period + 1);
    }

    // function to add new data in the list
    public void addData(double num) {
        if (datapoints < period) {
            sum += num;
            datapoints++;
            currentEMA = sum / datapoints;
        } else {
            datapoints++;
            currentEMA = K * num + currentEMA * (1 - K);
        }
    }

    public double getMean() {
        return currentEMA;
    }

    private static void test2() {
        double[] data = { 1.55, 2.0, 1.58, 1.3, 1.0, 1.6, 1.7, 1.0, 1.5, 2.0, 2.8, 2.1, 1.75, 1.55, 1.6 };
        EMA s = new EMA(3);
        for (double d : data) {
            s.addData(d);
            System.out.println(s.getMean());
        }
    }

    public static void main(String[] args) {
        test2();
    }

}
