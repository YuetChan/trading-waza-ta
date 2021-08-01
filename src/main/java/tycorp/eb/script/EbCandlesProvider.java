package tycorp.eb.script;

import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.client.TdaClient;
import com.studerw.tda.model.history.Candle;
import com.studerw.tda.model.history.FrequencyType;
import com.studerw.tda.model.history.PeriodType;
import com.studerw.tda.model.history.PriceHistReq;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static tycorp.eb.script.EbDateTimeHelper.*;

public class EbCandlesProvider {

    private static TdaClient CLIENT = new HttpTdaClient();

    public static List<EbCandle> getEbCandlesSinceDefaultStartDate(String ticker, FrequencyType frequencyType){
        PriceHistReq req = null;
        if(frequencyType.equals(FrequencyType.minute)){
            req = PriceHistReq.Builder.priceHistReq()
                    .withSymbol(ticker)
                    .withStartDate(
                            zonedDateTimeToEpoch(
                                    ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("America/New_York"))
                                            .minusDays(730)))
                    .withPeriodType(PeriodType.day)
                    .withExtendedHours(false)

                    .withFrequencyType(FrequencyType.minute)
                    .withFrequency(5)
                    .build();
        }

        if(frequencyType.equals(FrequencyType.daily)){
            req = PriceHistReq.Builder.priceHistReq()
                    .withSymbol(ticker)
                    .withPeriodType(PeriodType.year)
                    .withPeriod(2)

                    .withExtendedHours(false)
                    .withFrequencyType(FrequencyType.daily)
                    .withFrequency(1)
                    .build();
        }

        return candlesToEbCandles(CLIENT.priceHistory(req).getCandles(), frequencyType);
    }

    public static List<EbCandle> getEbCandlesSinceLastEndDate(String ticker, FrequencyType frequencyType, ZonedDateTime lastEndDateZdt){
        PriceHistReq req = null;
        var lastEndDateEpoch = zonedDateTimeToEpoch(lastEndDateZdt);

        if(frequencyType == FrequencyType.minute){
            req = PriceHistReq.Builder.priceHistReq()
                    .withSymbol(ticker)
                    .withStartDate(lastEndDateEpoch)
                    .withExtendedHours(false)

                    .withPeriodType(PeriodType.day)
                    .withFrequencyType(FrequencyType.minute)
                    .withFrequency(5)
                    .build();
        }

        if(frequencyType == FrequencyType.daily){
            req = PriceHistReq.Builder.priceHistReq()
                    .withSymbol(ticker)
                    .withStartDate(lastEndDateEpoch)
                    .withExtendedHours(false)

                    .withPeriodType(PeriodType.month)
                    .withFrequencyType(FrequencyType.daily)
                    .withFrequency(1)
                    .build();
        }

        return candlesToEbCandles(CLIENT.priceHistory(req).getCandles(), frequencyType);
    }

    public static List<EbCandle> candlesToEbCandles(List<Candle> candles, FrequencyType frequencyType) {
        var ebCandles = new ArrayList<EbCandle>();
        for(var candle : candles){
            ebCandles.add(
                    new EbCandle(
                            candle.getOpen().doubleValue(), candle.getHigh().doubleValue(),
                            candle.getLow().doubleValue(), candle.getClose().doubleValue(),
                            ZonedDateTime.ofInstant(
                                    Instant.ofEpochMilli(candle.getDatetime()),
                                    ZoneId.of("America/New_York"))));
        }

        if(frequencyType.equals(FrequencyType.daily)) {
            ebCandles.forEach(ebCandle ->
                ebCandle.setStartTimeZdt(
                        ebCandle.getStartTimeZdt()
                                .toLocalDate()
                                .atTime(LocalTime.parse("09:30"))
                                .atZone(ZoneId.of("America/New_York")))
            );
        }

        return ebCandles;
    }

}
