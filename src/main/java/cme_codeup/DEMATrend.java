package cme_codeup;

public class DEMATrend {
    public static enum ETrend {
        UP,
        DOWN,
        FLAT,
        NA;

        private double trendValue = 0;

        public double getTrendValue() {
            return trendValue;
        }

        public void setTrendValue(double trendValue) {
            this.trendValue = trendValue;
        }
    }

    private final int LONG_TREND_LEN;
    private final int SHORT_TREND_LEN;

    private DEMA ST;
    private DEMA LT;

    private long sampleCount = 0;

    public double FLAT_RANGE = 0;

    public DEMATrend(int longTrendLen, int shortTrendLen) {
        this(longTrendLen, shortTrendLen, 0);
    }

    public DEMATrend(int longTrendLen, int shortTrendLen, double flatRange) {
        LONG_TREND_LEN = longTrendLen;
        SHORT_TREND_LEN = shortTrendLen;
        FLAT_RANGE = flatRange;
        ST = new DEMA(SHORT_TREND_LEN);
        LT = new DEMA(LONG_TREND_LEN);
    }

    public synchronized void add(int dataPoint) {
        ST.addData(dataPoint);
        LT.addData(dataPoint);
        ++sampleCount;
    }

    public synchronized ETrend getTrendRatio() {
        double diff = 0;
        if (sampleCount < SHORT_TREND_LEN || sampleCount < LONG_TREND_LEN) {
            return ETrend.NA;
        }
        diff = ST.getMean() / LT.getMean();
        if (diff > 1) {
            ETrend.UP.setTrendValue(diff);
            return ETrend.UP;
        } else if (diff < 1) {
            ETrend.DOWN.setTrendValue(diff);
            return ETrend.DOWN;
        } else {
            return ETrend.FLAT;
        }
    }

    public static void main(String[] args) throws IOException {
        int[] data = new int[1000];
        char[] buf = new char[6000];
        // Provide a valid file path for testing
        // FileReader r = new FileReader("path_to_file");
        // r.read(buf);
        // String content = new String(buf);
        // r.close();
        // Example data points
        int[] exampleData = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        DEMATrend demaTrend = new DEMATrend(5, 3);
        for (int point : exampleData) {
            demaTrend.add(point);
        }
        System.out.println(demaTrend.getTrendRatio());
    }
}
