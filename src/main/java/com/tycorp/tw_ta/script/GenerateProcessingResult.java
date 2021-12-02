package com.tycorp.tw_ta.script;

import com.tycorp.tw_ta.extend_indicator.level_2.ConsensioIndicator;
import com.tycorp.tw_ta.extend_indicator.level_2.SMAGoldenCrossIndicator;
import com.tycorp.tw_ta.config.InfluxConfig;
import com.tycorp.tw_ta.extend_indicator.level_1.TD9_13Indicator;
import com.tycorp.tw_ta.extend_indicator.level_2.EMAGoldenCrossIndicator;
import com.tycorp.tw_ta.core.Candle;
import lombok.SneakyThrows;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import picocli.CommandLine;
import com.tycorp.tw_ta.core.CandlesToTa4jBarSeries;
import com.tycorp.tw_ta.lib.DateTimeHelper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.tycorp.tw_ta.lib.FileHelper.appendToFile;

/**
 * This script will process the stock data based on customized indicator
 *
 * Skip to line 84 for more details
 */
@CommandLine.Command(
        name = "GenerateProcessingResult",
        description = "Processes stock data based on specified indicators"
)
public class GenerateProcessingResult implements Runnable {

  @CommandLine.Option(
          names = {"-r", "--result"},
          required = true,
          description = "Filename that saves the result.")
  private String resultFname;

  @CommandLine.Option(
          names = {"-c", "--csv"},
          required = true,
          description = "CSV filename that contains the tickers list.")
  private String csvFname;


  private InfluxDB influxDB;

  @SneakyThrows
  @Override
  public void run() {
    influxDB = InfluxConfig.initInfluxConfig();

    List<String> tickers = LoadDataToLocalDatasource.loadFromCSV(csvFname);
    for(String ticker : tickers) {
      List<QueryResult.Series> series = getInfluxDbSeriesByMeasurement(InfluxConfig.generateMeasurement(ticker));
      if(series == null) {
        continue;
      }

      List<List<Object>> vals = series.get(0).getValues();
      List<Candle> candles = new ArrayList();
      for(var val : vals) {
        double open = Double.parseDouble(val.get(5).toString());
        double high = Double.parseDouble(val.get(3).toString());
        double low = Double.parseDouble(val.get(4).toString());
        double close = Double.parseDouble(val.get(1).toString());
        ZonedDateTime zdt = ZonedDateTime.parse(val.get(6).toString());

        candles.add(new Candle(open, high, low, close, zdt));
      }

      BarSeries barSeries = CandlesToTa4jBarSeries.convert(candles, CandlesToTa4jBarSeries.Ta4jTimeframe.DAILY);
      ClosePriceIndicator closePriceI = new ClosePriceIndicator(barSeries);

      // ----provide your own indicators-------------------------------------------

      // ----leve_2 indicator------------------------------------------------------

      EMAGoldenCrossIndicator goldenCrossEMA2050I = new EMAGoldenCrossIndicator(
              new EMAIndicator(closePriceI, 20), new EMAIndicator(closePriceI, 50));
      EMAGoldenCrossIndicator goldenCrossEMA50200I = new EMAGoldenCrossIndicator(
              new EMAIndicator(closePriceI, 50), new EMAIndicator(closePriceI, 200));
      EMAGoldenCrossIndicator goldenCrossEMA50100I = new EMAGoldenCrossIndicator(
              new EMAIndicator(closePriceI, 50), new EMAIndicator(closePriceI, 100));

      SMAGoldenCrossIndicator goldenCrossSMA2050I = new SMAGoldenCrossIndicator(
              new SMAIndicator(closePriceI, 20), new SMAIndicator(closePriceI, 50));
      SMAGoldenCrossIndicator goldenCrossSMA50200I = new SMAGoldenCrossIndicator(
              new SMAIndicator(closePriceI, 50), new SMAIndicator(closePriceI, 200));
      SMAGoldenCrossIndicator goldenCrossSMA50100I = new SMAGoldenCrossIndicator(
              new SMAIndicator(closePriceI, 50), new SMAIndicator(closePriceI, 100));

      ConsensioIndicator consensioSMA252I = new ConsensioIndicator(
              new SMAIndicator(closePriceI, 20),
              new SMAIndicator(closePriceI, 50),
              new SMAIndicator(closePriceI, 200));
      ConsensioIndicator consensioSMA251I = new ConsensioIndicator(
              new SMAIndicator(closePriceI, 20),
              new SMAIndicator(closePriceI, 50),
              new SMAIndicator(closePriceI, 100));

      // ----leve_1 indicator-------------------------------------------------------

      TD9_13Indicator td9_13I = new TD9_13Indicator(barSeries);

      // ----end--------------------------------------------------------------------

      int num = 30;

      if(barSeries.getBarCount() - num < 200 ) {
        continue;
      }

      for(int i = barSeries.getBarCount() - num; i < barSeries.getBarCount(); i ++) {
        Bar ithBar = barSeries.getBar(i);

        List<String> priceDetail = new ArrayList();

        priceDetail.add(ithBar.getOpenPrice().toString());
        priceDetail.add(ithBar.getHighPrice().toString());
        priceDetail.add(ithBar.getClosePrice().toString());
        priceDetail.add(ithBar.getLowPrice().toString());

        double lastClosePrice = barSeries.getBar(i).getClosePrice().doubleValue();
        double beforeLastClosePrice = barSeries.getBar(i - 1).getClosePrice().doubleValue();
        Double change = (lastClosePrice - beforeLastClosePrice) / beforeLastClosePrice;

        priceDetail.add(change.toString());

        List<String> indicators = new ArrayList();

        if(goldenCrossEMA2050I.getValue(i)) {
          indicators.add("EMA_20_50_golden_cross");
          System.out.println(ticker + " has EMA 20 50 golden cross");
          System.out.println("At " + ithBar.getEndTime());
        }

        if(goldenCrossEMA50200I.getValue(i)) {
          indicators.add("EMA_50_200_golden_cross");
          System.out.println(ticker + " has EMA 50 200 golden cross");
          System.out.println("At " + ithBar.getEndTime());
        }

        if(goldenCrossEMA50100I.getValue(i)) {
          indicators.add("EMA_50_100_golden_cross");
          System.out.println(ticker + " has EMA 50 100 golden cross");
          System.out.println("At " + ithBar.getEndTime());
        }

        if(goldenCrossSMA2050I.getValue(i)) {
          indicators.add("SMA_20_50_golden_cross");
          System.out.println(ticker + " has SMA 20 50 golden cross");
          System.out.println("At " + ithBar.getEndTime());
        }

        if(goldenCrossSMA50200I.getValue(i)) {
          indicators.add("SMA_50_200_golden_cross");
          System.out.println(ticker + " has SMA 50 200 golden cross");
          System.out.println("At " + ithBar.getEndTime());
        }

        if(goldenCrossSMA50100I.getValue(i)) {
          indicators.add("SMA_50_100_golden_cross");
          System.out.println(ticker + " has SMA 50 100 golden cross");
          System.out.println("At " + ithBar.getEndTime());
        }

        if(consensioSMA252I.getValue(i)) {
          indicators.add("SMA_20_50_200_consensio");
          System.out.println(ticker + " has SMA 20 50 200 consensio");
          System.out.println("At " + ithBar.getEndTime());
        }

        if(consensioSMA251I.getValue(i)) {
          indicators.add("SMA_20_50_100_consensio");
          System.out.println(ticker + " has SMA 20 50 100 consensio");
          System.out.println("At " + ithBar.getEndTime());
        }

        if(td9_13I.getValue(i) == 9) {
          indicators.add("td_9_top");
          System.out.println(ticker + " has td 9 top");
          System.out.println("At " + ithBar.getEndTime());
        }

        if(td9_13I.getValue(i) == -9) {
          indicators.add("td_9_bottom");
          System.out.println(ticker + " has td 9 bottom");
          System.out.println("At " + ithBar.getEndTime());
        }

        Long endTimeAt = ithBar.getEndTime().toInstant().toEpochMilli();
        Long processedAt = Instant.now().toEpochMilli();

        ZonedDateTime processedAtZdt = DateTimeHelper.truncateTime(Instant.ofEpochMilli(processedAt).atZone(ZoneId.of("America/New_York")));
        Long truncatedProcessedAt = processedAtZdt.toInstant().toEpochMilli();

        String line = indicators.size() == 0
                ? ""
                : ticker + ","  + priceDetail.stream().collect(Collectors.joining(","))
                + "," + indicators.stream().collect(Collectors.joining(",") )
                + "," + endTimeAt + "," + truncatedProcessedAt;

        if(line != "") {
          appendToFile(resultFname, line);
        }
      }
    }

    System.out.println("All tickers processed");
    System.exit(0);
  }

  public List<QueryResult.Series> getInfluxDbSeriesByMeasurement(String measurement) {
    // 1500 days ago would guarantee to query at least 200 candles
    QueryResult candleQry = this.influxDB.query(
            new Query("SELECT * FROM " + measurement + " WHERE frq=" + "'DAILY' AND time > now() - 1500d ORDER BY ASC"));
    return candleQry.getResults().get(0).getSeries();
  }

  public static void main(String[] args) {
        CommandLine.run(new GenerateProcessingResult(), args);
    }

}
