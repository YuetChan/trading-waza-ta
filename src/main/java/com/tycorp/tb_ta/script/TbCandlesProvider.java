package com.tycorp.tb_ta.script;

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

import static com.tycorp.tb_ta.lib.DateTimeHelper.*;

/**
 * Helper functions for getting stock data from TD Ameritrade api
 */
public class TbCandlesProvider {

    private static TdaClient CLIENT = new HttpTdaClient();

    public static List<TbCandle> getTbCandlesSinceDefaultStartDate(String ticker, FrequencyType frequencyType){
        PriceHistReq req = null;

        int retryCtr = 5;
        while(true) {
            try {
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

                return candlesToTbCandles(CLIENT.priceHistory(req).getCandles(), frequencyType);
            } catch (Exception e) {
                if (retryCtr == 0) {
                    throw e;
                }else {
                    retryCtr --;
                }
            }
        }
    }

    public static List<TbCandle> getTbCandlesSinceLastEndDate(String ticker, FrequencyType frequencyType, ZonedDateTime lastEndDateZdt){
        PriceHistReq req = null;
        long lastEndDateEpoch = zonedDateTimeToEpoch(lastEndDateZdt);

        int retryCtr = 5;
        while(true) {
            try {
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

                return candlesToTbCandles(CLIENT.priceHistory(req).getCandles(), frequencyType);
            }catch(Exception e){
                if (retryCtr == 0) {
                    throw e;
                }else {
                    retryCtr --;
                }
            }
        }
    }

    private static List<TbCandle> candlesToTbCandles(List<Candle> candles, FrequencyType frequencyType) {
        List<TbCandle> tbCandles = new ArrayList();
        for(var candle : candles){
            tbCandles.add(
                    new TbCandle(
                            candle.getOpen().doubleValue(), candle.getHigh().doubleValue(),
                            candle.getLow().doubleValue(), candle.getClose().doubleValue(),
                            ZonedDateTime.ofInstant(
                                    Instant.ofEpochMilli(candle.getDatetime()),
                                    ZoneId.of("America/New_York"))));
        }

        if(frequencyType.equals(FrequencyType.daily)) {
            tbCandles.forEach(tbCandle ->
                tbCandle.setStartTimeZdt(
                        tbCandle.getStartTimeZdt()
                                .toLocalDate()
                                .atTime(LocalTime.parse("09:30"))
                                .atZone(ZoneId.of("America/New_York")))
            );
        }

        return tbCandles;
    }

}
