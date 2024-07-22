package cme_codeup;

import java.io.*;

public class EMATrend {
    public static enum ETrend {
        UP,
        DOWN,
        FLAT,
        NA;

        private double trendValue = 0; //holds thet diff or the ratio of the short and long trend

        public double getTrendValue() {return trendValue;}

        public void setTrendValue(double trendValue) {this.trendValue = trendValue;}
    }

    private final int LONG_TREND_LEN;
    private final int SHORT_TREND_LEN;

    private EMA ST;
    private EMA LT;

    private long sampleCount = 0;

    public double FLAT_RANGE = 0;

    public EMATrend(int longTrendLen, int shortTrendLen) {this(longTrendLen, shortTrendLen, 0);}

    public EMATrend(int longTrendLen, int shortTrendLen, double flatRange){
        LONG_TREND_LEN = longTrendLen;
        SHORT_TREND_LEN = shortTrendLen;
        FLAT_RANGE = flatRange;
        ST = new EMA(SHORT_TREND_LEN);
        LT = new EMA(LONG_TREND_LEN);
    }

    public synchronized void add(int dataPoint){
        ST.addData(dataPoint);
        LT.addData(dataPoint);
        ++sampleCount;
    }

    public synchronized ETrend getTrendRatio(){
        double diff = 0;
        if (sampleCount < SHORT_TREND_LEN || sampleCount < LONG_TREND_LEN){
            return ETrend.NA;
        }
        diff = ST.getMean()/LT.getMean();
        if (diff > 1){
            ETrend.UP.setTrendValue(diff);
            return ETrend.UP;
        } else if (diff < 1){
            ETrend.DOWN.setTrendValue(diff);
            return ETrend.DOWN;
        } else {
            return ETrend.FLAT;
        }
    }

    public static void main(String[] args) throws IOException{
        int[] data = new int[1000];
        char[] buf = new char[6000];
        FileReader r = new FileReader()
    }
}
