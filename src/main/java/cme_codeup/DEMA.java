package cme_codeup;

public class DEMA {

    private final int period;
    private double currentDEMA;
    private EMA ema;
    private EMA emaOfEma;
    private int datapoints = 0;

    // Constructor to initialize period
    public DEMA(int period) {
        this.period = period;
        this.ema = new EMA(period);
        this.emaOfEma = new EMA(period);
        this.currentDEMA = 0.0;
    }

    // Function to add new data
    public void addData(double num) {
        ema.addData(num);
        emaOfEma.addData(ema.getMean());
        if (datapoints < period) {
            datapoints++;
            currentDEMA = ema.getMean();
        } else {
            currentDEMA = 2 * ema.getMean() - emaOfEma.getMean();
        }
    }

    public double getMean() {
        return currentDEMA;
    }

    private static void test() {
        double[] data = { 1.55, 2.0, 1.58, 1.3, 1.0, 1.6, 1.7, 1.0, 1.5, 2.0, 2.8, 2.1, 1.75, 1.55, 1.6 };
        DEMA d = new DEMA(3);
        for (double num : data) {
            d.addData(num);
            System.out.println(d.getMean());
        }
    }

    public static void main(String[] args) {
        test();
    }
}
