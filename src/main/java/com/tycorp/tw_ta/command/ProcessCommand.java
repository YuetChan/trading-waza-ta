package com.tycorp.tw_ta.command;

import com.tycorp.tw_ta.extend_indicator.level_2.ConsensioIndicator;
import com.tycorp.tw_ta.extend_indicator.level_2.SMAGoldenCrossIndicator;
import com.tycorp.tw_ta.config.InfluxConfig;
import com.tycorp.tw_ta.extend_indicator.level_1.TD9_13Indicator;
import com.tycorp.tw_ta.extend_indicator.level_2.EMAGoldenCrossIndicator;
import com.tycorp.tw_ta.script.TwCandle;
import lombok.SneakyThrows;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.candles.BearishEngulfingIndicator;
import org.ta4j.core.indicators.candles.BearishHaramiIndicator;
import org.ta4j.core.indicators.candles.BullishEngulfingIndicator;
import org.ta4j.core.indicators.candles.BullishHaramiIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import picocli.CommandLine;
import com.tycorp.tw_ta.script.TwCandlesToTa4jBarSeries;
import com.tycorp.tw_ta.lib.DateTimeHelper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.tycorp.tw_ta.lib.FileHelper.appendToFile;

/**
 * This script will process the stock data based on customized implementation
 *
 * Skip to line 80 for more details
 */
@CommandLine.Command(
        name = "Process",
        description = "Process stock data based on specified indicator"
)
public class ProcessCommand implements Runnable {

    @CommandLine.Option(
            names = {"-s", "--selected"},
            required = true,
            description = "Filename that saves the selected tickers.")
    private String selectedTickersFname;
    @CommandLine.Option(
            names = {"-t", "--ticker"},
            required = true,
            description = "CSV filename that contains the tickers list.")
    private String tickersCSV;

    @SneakyThrows
    @Override
    public void run() {
        InfluxDB influxDB = InfluxConfig.initInfluxConfig();

        List<String> loadedTickers = LoadCommand.loadTickersFromCSV(tickersCSV);
        for(String ticker : loadedTickers) {
            QueryResult dailyCandleQry = influxDB.query(
                    new Query("SELECT * FROM " + InfluxConfig.generateMeasurement(ticker) + " WHERE frq=" + "'DAILY' ORDER BY ASC"));

            List<QueryResult.Series> dailySeries = dailyCandleQry.getResults().get(0).getSeries();
            if(dailySeries == null){
                continue;
            }

            List<List<Object>> vals = dailySeries.get(0).getValues();
            List<TwCandle> dailyCandles = new ArrayList();
            for(var val : vals) {
                dailyCandles.add(
                        new TwCandle(
                                Double.parseDouble(val.get(5).toString()), Double.parseDouble(val.get(3).toString()),
                                Double.parseDouble(val.get(4).toString()), Double.parseDouble(val.get(1).toString()),
                                ZonedDateTime.parse(val.get(6).toString())));
            }

            BarSeries barSeries = TwCandlesToTa4jBarSeries.convert(dailyCandles, TwCandlesToTa4jBarSeries.Ta4jTimeframe.DAILY);
            ClosePriceIndicator closePriceI = new ClosePriceIndicator(barSeries);

            // ----provide your own indicators-------------------------------------------

            // ----leve_2 indicator------------------------------------------------------

            EMAGoldenCrossIndicator goldenCrossEMA2050I = new EMAGoldenCrossIndicator(
                    new EMAIndicator(closePriceI, 20),
                    new EMAIndicator(closePriceI, 50));

            EMAGoldenCrossIndicator goldenCrossEMA50200I = new EMAGoldenCrossIndicator(
                    new EMAIndicator(closePriceI, 50),
                    new EMAIndicator(closePriceI, 200));

            EMAGoldenCrossIndicator goldenCrossEMA50100I = new EMAGoldenCrossIndicator(
                    new EMAIndicator(closePriceI, 50),
                    new EMAIndicator(closePriceI, 100));

            SMAGoldenCrossIndicator goldenCrossSMA2050I = new SMAGoldenCrossIndicator(
                    new SMAIndicator(closePriceI, 20),
                    new SMAIndicator(closePriceI, 50));

            SMAGoldenCrossIndicator goldenCrossSMA50200I = new SMAGoldenCrossIndicator(
                    new SMAIndicator(closePriceI, 50),
                    new SMAIndicator(closePriceI, 200));

            SMAGoldenCrossIndicator goldenCrossSMA50100I = new SMAGoldenCrossIndicator(
                    new SMAIndicator(closePriceI, 50),
                    new SMAIndicator(closePriceI, 100));

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

            BullishEngulfingIndicator bullishEngulfingI = new BullishEngulfingIndicator(barSeries);
            BearishEngulfingIndicator bearishEngulfingI = new BearishEngulfingIndicator(barSeries);

            BullishHaramiIndicator bullishHaramiI = new BullishHaramiIndicator(barSeries);
            BearishHaramiIndicator bearishHaramiI = new BearishHaramiIndicator(barSeries);

            // ----end--------------------------------------------------------------------

            System.out.println("Processed " + ticker);

            List<String> indicators = new ArrayList();
            List<String> priceDetail = new ArrayList();

            Bar lastBar = barSeries.getLastBar();

            priceDetail.add(lastBar.getOpenPrice().toString());
            priceDetail.add(lastBar.getHighPrice().toString());
            priceDetail.add(lastBar.getClosePrice().toString());
            priceDetail.add(lastBar.getLowPrice().toString());

            int endIndex = barSeries.getEndIndex();
            if(endIndex >= 1) {
                Double lastClosePrice = barSeries.getBar(endIndex).getClosePrice().doubleValue();
                Double closePrice = barSeries.getBar(endIndex - 1).getClosePrice().doubleValue();
                Double change = (lastClosePrice - closePrice) / closePrice;

                priceDetail.add(change.toString());
            }

            // ----provide your own conditions based on your indicators----------

            if(goldenCrossEMA2050I.getValue(endIndex)) {
                indicators.add("EMA_20_50_golden_cross");
                System.out.println(ticker + " has EMA 20 50 golden cross");
                System.out.println("At " + lastBar.getEndTime());
            }

            if(goldenCrossEMA50200I.getValue(endIndex)) {
                indicators.add("EMA_50_200_golden_cross");
                System.out.println(ticker + " has EMA 50 200 golden cross");
                System.out.println("At " + lastBar.getEndTime());
            }

            if(goldenCrossEMA50100I.getValue(endIndex)) {
                indicators.add("EMA_50_100_golden_cross");
                System.out.println(ticker + " has EMA 50 100 golden cross");
                System.out.println("At " + lastBar.getEndTime());
            }

            if(goldenCrossSMA2050I.getValue(endIndex)) {
                indicators.add("SMA_20_50_golden_cross");
                System.out.println(ticker + " has SMA 20 50 golden cross");
                System.out.println("At " + lastBar.getEndTime());
            }

            if(goldenCrossSMA50200I.getValue(endIndex)) {
                indicators.add("SMA_50_200_golden_cross");
                System.out.println(ticker + " has SMA 50 200 golden cross");
                System.out.println("At " + lastBar.getEndTime());
            }

            if(goldenCrossSMA50100I.getValue(endIndex)) {
                indicators.add("SMA_50_100_golden_cross");
                System.out.println(ticker + " has SMA 50 100 golden cross");
                System.out.println("At " + lastBar.getEndTime());
            }

            if(consensioSMA252I.getValue(endIndex)) {
                indicators.add("SMA_20_50_200_consensio");
                System.out.println(ticker + " has SMA 20 50 200 consensio");
                System.out.println("At " + lastBar.getEndTime());
            }

            if(consensioSMA251I.getValue(endIndex)) {
                indicators.add("SMA_20_50_100_consensio");
                System.out.println(ticker + " has SMA 20 50 100 consensio");
                System.out.println("At " + lastBar.getEndTime());
            }

            if(td9_13I.getValue(endIndex) == 9) {
                indicators.add("td_9_top");
                System.out.println(ticker + " has td 9 top");
                System.out.println("At " + lastBar.getEndTime());
            }

            if(td9_13I.getValue(endIndex) == -9) {
                indicators.add("td_9_bottom");
                System.out.println(ticker + " has td 9 bottom");
                System.out.println("At " + lastBar.getEndTime());
            }

            if(bullishEngulfingI.getValue(endIndex)) {
                indicators.add("bullish_engulfing");
                System.out.println(ticker + " bullish engulfing");
                System.out.println("At " + lastBar.getEndTime());
            }

            if(bearishEngulfingI.getValue(endIndex)) {
                indicators.add("bearish_engulfing");
                System.out.println(ticker + " bearish engulfing");
                System.out.println("At " + lastBar.getEndTime());
            }

            if(bullishHaramiI.getValue(endIndex)) {
                indicators.add("bullish_harami");
                System.out.println(ticker + " bullish harami");
                System.out.println("At " + lastBar.getEndTime());
            }

            if(bearishHaramiI.getValue(endIndex)) {
                indicators.add("bearish_harami");
                System.out.println(ticker + " bearish harami");
                System.out.println("At " + lastBar.getEndTime());
            }

            // ----End-----------------------------------------------------------------

            Long processedAt = lastBar.getEndTime().toInstant().toEpochMilli();
            ZonedDateTime processedAtZdt = DateTimeHelper.truncateTime(Instant.ofEpochMilli(processedAt).atZone(ZoneId.of("America/New_York")));
            Long truncatedProcessedAt = processedAtZdt.toInstant().toEpochMilli();

            String line = indicators.size() == 0
                    ? ""
                    : ticker + ","  + priceDetail.stream().collect(Collectors.joining(",")) + "," + indicators.stream().collect(Collectors.joining(",") ) + ","
                    + truncatedProcessedAt;

            if(line != "") {
                appendToFile(selectedTickersFname, line);
            }
        }

        System.out.println("All tickers processed");
        System.exit(0);
    }

    public static void main(String[] args) {
        CommandLine.run(new ProcessCommand(), args);
    }

}
