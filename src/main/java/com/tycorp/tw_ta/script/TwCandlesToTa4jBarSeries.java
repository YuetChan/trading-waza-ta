package com.tycorp.tw_ta.script;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import java.time.*;
import java.util.List;

/**
 * Helper functions for converting TwCandle into Ta4jBarSeries
 * Currently support convertion to different frequencies for daily and mins TwCandle
 */
public class TwCandlesToTa4jBarSeries {

    public enum Ta4jTimeframe {
        MINS, DAILY
    }

    public static BarSeries convert(List<TwCandle> candles, Ta4jTimeframe timeframe, int frequency) {
        if(timeframe.equals(Ta4jTimeframe.MINS)) {
            return candlesToBarSeriesMinsVariance(candles, frequency);
        }
        if(timeframe.equals(Ta4jTimeframe.DAILY)) {
            return candlesToBarSeriesDaily(candles);
        }

        return null;
    }

    private static BarSeries candlesToBarSeriesMinsVariance(List<TwCandle> candles, int frequency) {
        BarSeries barSeries = new BaseBarSeries();

        double factor = frequency / 5;
        int factorCtr = 0;

        double open = 0d;
        double close = 0d;
        double high = 0d;
        double low = 0d;
        ZonedDateTime endTimeZdt = null;

        for(int i = 0; i < candles.size() ; i ++) {
            TwCandle ithCandle = candles.get(i);
            if(i == 0) {
                open = ithCandle.getOpen();
                close = ithCandle.getClose();
                high = ithCandle.getHigh();
                low = ithCandle.getLow();
                endTimeZdt = ithCandle.getStartTimeZdt().plusMinutes(5);
                factorCtr ++;
            }

            if(i == candles.size() - 1) {
                close = ithCandle.getClose();
                endTimeZdt = ithCandle.getStartTimeZdt().plusMinutes(5);
                barSeries.addBar(endTimeZdt, open, high, low, close);
                break;
            }

            boolean isEndOfDay = !ithCandle.getStartTimeZdt().getDayOfWeek().equals(candles.get(i + 1).getStartTimeZdt().getDayOfWeek());
            if(factorCtr == factor - 1 || isEndOfDay){
                close = ithCandle.getClose();
                endTimeZdt = ithCandle.getStartTimeZdt().plusMinutes(5);
                barSeries.addBar(endTimeZdt, open, high, low, close);
                factorCtr = 0;
            }else if(factorCtr == 0){
                open = ithCandle.getOpen();
                high = ithCandle.getHigh();
                low = ithCandle.getLow();
                factorCtr ++;
            }else{
                if(ithCandle.getHigh() > high){
                    high = ithCandle.getHigh();
                }
                if(ithCandle.getLow() < low){
                    low = ithCandle.getLow();
                }
                factorCtr ++;
            }

        }

        return barSeries;
    }

    private static BarSeries candlesToBarSeriesDaily(List<TwCandle> candles) {
        BarSeries barSeries = new BaseBarSeries();
        for(int i = 0; i < candles.size(); i ++){
            TwCandle ithCandle = candles.get(i);

            double open = ithCandle.getOpen();
            double close = ithCandle.getClose();
            double high = ithCandle.getHigh();
            double low = ithCandle.getLow();

            ZonedDateTime endTimeZdt = ithCandle.getStartTimeZdt()
                    .toLocalDate()
                    .atTime(LocalTime.parse("20:00"))
                    .atZone(ZoneId.of("America/New_York"));

            barSeries.addBar(endTimeZdt, open, high, low, close);
        }

        return barSeries;
    }

}
