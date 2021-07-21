package tycorp.eb.script;

import com.studerw.tda.model.history.Candle;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class CandlesToTa4jBarSeries {

    public enum Ta4jTimeframe { MINS, HOURLY, DAILY, WEEKLY }

    public static BarSeries convert(List<Candle> candles,
                                    Ta4jTimeframe timeFrame, int frequency) {
        if(timeFrame.equals(Ta4jTimeframe.MINS))
            return candlesToMinsBarSeries(candles, frequency);

        else if(timeFrame.equals(Ta4jTimeframe.DAILY))
            return candlesToDailyBarSeries(candles);

        else
            return null;
    }

    private static BaseBarSeries candlesToDailyBarSeries(List<Candle> candles) {
        var barSeries = new BaseBarSeries();
        LocalDate preDay_lD = null;

        for(var candle : candles){
            var currDay_lD = Instant.ofEpochMilli(candle.getDatetime())
                    .atZone(ZoneId.of("America/New_York")).toLocalDate();
            var currDay_zdt = currDay_lD.atStartOfDay(ZoneId.of("America/New_York"));

            if(preDay_lD != null){
                if(currDay_lD.isAfter(preDay_lD)){
                    barSeries.addBar(Duration.ofDays(1), currDay_zdt);
                    preDay_lD = currDay_lD;
                }
            }else {
                barSeries.addBar(Duration.ofDays(1), currDay_zdt);
                preDay_lD = currDay_lD;
            }

            barSeries.addPrice(candle.getOpen());
            barSeries.addPrice(candle.getHigh());
            barSeries.addPrice(candle.getLow());
            barSeries.addPrice(candle.getClose());
        }

        return barSeries;
    }

    private static BarSeries candlesToMinsBarSeries(List<Candle> candles, int frequency) {
        BarSeries barSeries = new BaseBarSeries();

        for(int i = 0; i < candles.size(); i = i + frequency){
            var lastCandle = candles.get(i + frequency - 1);

            var endTimeZdt = Instant.ofEpochMilli(lastCandle.getDatetime())
                    .atZone(ZoneId.of("America/New_York"))
                    .plusMinutes(5);

            var open = candles.get(i).getOpen();
            var close = lastCandle.getClose();

            var high = BigDecimal.ZERO;
            var low = BigDecimal.ZERO;
            for(int j = i; j < i + frequency; j++){
                var tmpHigh = candles.get(j).getHigh();
                high = high.compareTo(tmpHigh) == 1 ? high : tmpHigh;

                var tmpLow = candles.get(j).getLow();
                low = low.compareTo(tmpLow) == 1 ? low : tmpLow;
            }

            barSeries.addBar(endTimeZdt, open, high, low, close);
        }

        var rmdr = candles.size() % frequency;
        var rmdrIth = candles.size() - rmdr;

        var lastCandle = candles.get(candles.size() - 1);

        var endTimeZdt = Instant.ofEpochMilli(lastCandle.getDatetime())
                .atZone(ZoneId.of("America/New_York"))
                .plusMinutes(5);

        var open = candles.get(rmdrIth).getOpen();
        var close = lastCandle.getClose();

        var high = BigDecimal.ZERO;
        var low = BigDecimal.ZERO;
        for(int i = rmdrIth; i < candles.size(); i++){
            var tmpHigh = candles.get(i).getHigh();
            high = high.compareTo(tmpHigh) == 1 ? high : tmpHigh;

            var tmpLow = candles.get(i).getLow();
            low = low.compareTo(tmpLow) == 1 ? low : tmpLow;
        }

        barSeries.addBar(endTimeZdt, open, high, low, close);
        return barSeries;
    }


}
