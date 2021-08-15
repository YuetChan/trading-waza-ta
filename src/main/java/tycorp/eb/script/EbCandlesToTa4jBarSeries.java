package tycorp.eb.script;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import java.time.*;
import java.util.List;

public class EbCandlesToTa4jBarSeries {

    public enum Ta4jTimeframe {
        MINS, HOURLY, DAILY
    }

    public static BarSeries convert(List<EbCandle> candles, Ta4jTimeframe timeframe, int frequency) {
        if(timeframe.equals(Ta4jTimeframe.MINS)) {
            return candlesToBarSeriesMinsVariance(candles, frequency);
        }
        if(timeframe.equals(Ta4jTimeframe.DAILY)) {
            return candlesToBarSeriesDaily(candles);
        }

        return null;
    }

    private static BarSeries candlesToBarSeriesMinsVariance(List<EbCandle> candles, int frequency) {
        var barSeries = new BaseBarSeries();

        var factor = frequency / 5;
        var factorCtr = 0;

        var open = 0d;
        var close = 0d;
        var high = 0d;
        var low = 0d;
        ZonedDateTime endTimeZdt = null;

        for(int i = 0; i < candles.size() ; i ++) {
            var ithCandle = candles.get(i);
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

            var endOfDay = !ithCandle.getStartTimeZdt().getDayOfWeek().equals(candles.get(i + 1).getStartTimeZdt().getDayOfWeek());
            if(factorCtr == factor - 1 || endOfDay){
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

    private static BarSeries candlesToBarSeriesDaily(List<EbCandle> candles) {
        var barSeries = new BaseBarSeries();
        for(int i = 0; i < candles.size(); i ++){
            var ithCandle = candles.get(i);

            var open = ithCandle.getOpen();
            var close = ithCandle.getClose();
            var high = ithCandle.getHigh();
            var low = ithCandle.getLow();

            var endTimeZdt = ithCandle.getStartTimeZdt()
                    .toLocalDate()
                    .atTime(LocalTime.parse("20:00"))
                    .atZone(ZoneId.of("America/New_York"));

            barSeries.addBar(endTimeZdt, open, high, low, close);
        }

        return barSeries;
    }

}
