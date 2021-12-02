package com.tycorp.tw_ta.core;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import java.time.*;
import java.util.List;

/**
 * Provider functions for converting TwCandles into Ta4jBarSeries
 */
public class CandlesToTa4jBarSeries {

  public enum Ta4jTimeframe { DAILY }

  public static BarSeries convert(List<Candle> candles, Ta4jTimeframe timeframe) {
    if(timeframe.equals(Ta4jTimeframe.DAILY)) {
      return candlesToBarSeries_daily(candles);
    }

    return null;
  }

  private static BarSeries candlesToBarSeriesMinsVariance(List<Candle> candles, int frequency) {
    BarSeries barSeries = new BaseBarSeries();

    double factor = frequency / 5;
    int factorCtr = 0;

    double open = 0d;
    double close = 0d;
    double high = 0d;
    double low = 0d;
    ZonedDateTime endTimeZdt = null;

    for(int i = 0; i < candles.size() ; i ++) {
      Candle ithCandle = candles.get(i);
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
      if(factorCtr == factor - 1 || isEndOfDay) {
        close = ithCandle.getClose();
        endTimeZdt = ithCandle.getStartTimeZdt().plusMinutes(5);
        barSeries.addBar(endTimeZdt, open, high, low, close);

        factorCtr = 0;
      }else if(factorCtr == 0) {
        open = ithCandle.getOpen();
        high = ithCandle.getHigh();
        low = ithCandle.getLow();

        factorCtr ++;
      }else {
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

  private static BarSeries candlesToBarSeries_daily(List<Candle> candles) {
    BarSeries barSeries = new BaseBarSeries();
    for(int i = 0; i < candles.size(); i ++){
      Candle ithCandle = candles.get(i);

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
