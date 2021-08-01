package tycorp.eb.script;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import java.time.*;
import java.util.List;

public class EbCandlesToTa4jBarSeries {

    public enum Ta4jTimeframe { MINS, HOURLY, DAILY }

    public static BarSeries convert(List<EbCandle> candles, Ta4jTimeframe timeframe, int frequency) {
        if(timeframe.equals(Ta4jTimeframe.MINS)) {
            return candlesToBarSeriesMinsVariance(candles, frequency);
        }else if(timeframe.equals(Ta4jTimeframe.DAILY)) {
            return candlesToBarSeriesDaily(candles);
        }else {
            return null;
        }
    }

    private static BarSeries candlesToBarSeriesMinsVariance(List<EbCandle> candles, int frequency) {
        var barSeries = new BaseBarSeries();
        var factor = frequency / 5;

        for(int i = 0; i < candles.size(); i = i + factor){
            var lastCandle = candles.get(i + factor - 1);

            var open = candles.get(i).getOpen();
            var close = lastCandle.getClose();

            var high = 0d;
            var low = 0d;
            for(int j = i; j < i + factor; j++){
                var tmpHigh = candles.get(j).getHigh();
                high = high > tmpHigh ? high : tmpHigh;

                var tmpLow = candles.get(j).getLow();
                low = low > tmpLow ? low : tmpLow;
            }

            var endTimeZdt = lastCandle.getStartTimeZdt().plusMinutes(5);
            barSeries.addBar(endTimeZdt, open, high, low, close);
        }

        var rmdr = candles.size() % factor;
        var ith = candles.size() - rmdr;

        var lastCandle = candles.get(candles.size() - 1);

        var open = candles.get(ith).getOpen();
        var close = lastCandle.getClose();

        var high = 0d;
        var low = 0d;
        for(int i = ith; i < candles.size(); i++){
            var tmpHigh = candles.get(i).getHigh();
            high = high > tmpHigh ? high : tmpHigh;

            var tmpLow = candles.get(i).getLow();
            low = low > tmpLow ? low : tmpLow;
        }

        var endTimeZdt = lastCandle.getStartTimeZdt().plusMinutes(5);
        barSeries.addBar(endTimeZdt, open, high, low, close);
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
