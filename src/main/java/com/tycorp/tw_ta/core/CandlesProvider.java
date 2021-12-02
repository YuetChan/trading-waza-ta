package com.tycorp.tw_ta.core;

import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.client.TdaClient;
import com.studerw.tda.model.history.FrequencyType;
import com.studerw.tda.model.history.PeriodType;
import com.studerw.tda.model.history.PriceHistReq;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.tycorp.tw_ta.lib.DateTimeHelper.*;

/**
 * Helper functions for getting stock data from TD Ameritrade API
 */
public class CandlesProvider {

  private static TdaClient CLIENT = new HttpTdaClient();

  public static List<Candle> getCandlesSinceDefaultStartDate(String ticker, FrequencyType frequencyType){
    PriceHistReq req = null;

    int retryCtr = 5;
    while(true) {
      try {
        if(frequencyType.equals(FrequencyType.daily)) {
          req = PriceHistReq.Builder.priceHistReq()
                  .withSymbol(ticker)
                  .withPeriodType(PeriodType.year)
                  .withPeriod(2)

                  .withExtendedHours(false)
                  .withFrequencyType(FrequencyType.daily)
                  .withFrequency(1)
                  .build();
        }

        return tdaCandlesToCandles(CLIENT.priceHistory(req).getCandles(), frequencyType);
      } catch (Exception e) {
        if (retryCtr == 0) {
          throw e;
        }else {
          retryCtr --;
        }
      }
    }
  }

  public static List<Candle> getCandlesSinceLastEndDate(String ticker, FrequencyType frequencyType, ZonedDateTime lastEndDateZdt){
    PriceHistReq req = null;
    long lastEndDateEpoch = zonedDateTimeToEpoch(lastEndDateZdt);

    int retryCtr = 5;
    while(true) {
      try {
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

        return tdaCandlesToCandles(CLIENT.priceHistory(req).getCandles(), frequencyType);
      }catch(Exception e){
        if (retryCtr == 0) {
          throw e;
        }else {
          retryCtr --;
        }
      }
    }
  }

  private static List<Candle> tdaCandlesToCandles(List<com.studerw.tda.model.history.Candle> tdaCandles, FrequencyType frequencyType) {
    List<Candle> candles = new ArrayList();
    for(var tdaCandle : tdaCandles) {
      candles.add(
              new Candle(
                      tdaCandle.getOpen().doubleValue(), tdaCandle.getHigh().doubleValue(),
                      tdaCandle.getLow().doubleValue(), tdaCandle.getClose().doubleValue(),
                      ZonedDateTime.ofInstant(Instant.ofEpochMilli(tdaCandle.getDatetime()), ZoneId.of("America/New_York"))));
    }

    if(frequencyType.equals(FrequencyType.daily)) {
      candles.forEach(candle ->
              candle.setStartTimeZdt(
                      candle.getStartTimeZdt()
                              .toLocalDate()
                              .atTime(LocalTime.parse("09:30")).atZone(ZoneId.of("America/New_York")))
      );
    }

    return candles;
  }

}
