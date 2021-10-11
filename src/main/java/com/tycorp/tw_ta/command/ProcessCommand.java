package com.tycorp.tw_ta.command;

import com.tycorp.tw_ta.config.InfluxConfig;
import com.tycorp.tw_ta.extend_indicator.TD9_13Indicator;
import com.tycorp.tw_ta.script.TwCandle;
import lombok.SneakyThrows;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import picocli.CommandLine;
import com.tycorp.tw_ta.extend_indicator.GoldenCrossIndicator;
import com.tycorp.tw_ta.extend_indicator.TwSMAIndicator;
import com.tycorp.tw_ta.script.TwCandlesToTa4jBarSeries;
import com.tycorp.tw_ta.lib.DateTimeHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
            QueryResult dailyCandleResult = influxDB.query(
                    new Query("SELECT * FROM " + InfluxConfig.generateMeasurement(ticker)
                            + " WHERE frq=" + "'DAILY' ORDER BY ASC"));

            List<QueryResult.Series> dailySeries = dailyCandleResult.getResults().get(0).getSeries();
            if(dailySeries == null){
                continue;
            }

            List<List<Object>> vals = dailySeries.get(0).getValues();

            List<TwCandle> dailyCandles = new ArrayList();
            for(var val : vals){
                dailyCandles.add(
                        new TwCandle(
                                Double.parseDouble(val.get(5).toString()), Double.parseDouble(val.get(3).toString()),
                                Double.parseDouble(val.get(4).toString()), Double.parseDouble(val.get(1).toString()),
                                ZonedDateTime.parse(val.get(6).toString())));
            }

            BarSeries barSeries = TwCandlesToTa4jBarSeries.convert(dailyCandles, TwCandlesToTa4jBarSeries.Ta4jTimeframe.DAILY, 0);
            ClosePriceIndicator closePriceI = new ClosePriceIndicator(barSeries);

            // ----Provide your own indicators-------------------------------------------

            GoldenCrossIndicator goldenCross200I = new GoldenCrossIndicator(
                    new TwSMAIndicator(closePriceI, 20),
                    new TwSMAIndicator(closePriceI, 50),
                    new TwSMAIndicator(closePriceI, 200));
            GoldenCrossIndicator goldenCross100I = new GoldenCrossIndicator(
                    new TwSMAIndicator(closePriceI, 20),
                    new TwSMAIndicator(closePriceI, 50),
                    new TwSMAIndicator(closePriceI, 100));

            TD9_13Indicator td9_13I = new TD9_13Indicator(barSeries);

            // ----End--------------------------------------------------------------------

            System.out.println("Processed " + ticker);

            List<String> indicators = new ArrayList();
            List<String> priceDetail = new ArrayList();

            // tag[0], ... tag [3] are reserved for open, high, close and low price
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


            // ----Provide your own conditions based on your indicators----------

            if(goldenCross100I.getValue(barSeries.getEndIndex())) {
                indicators.add("golden_cross_100");
                System.out.println(ticker + " has consensio 100");
                System.out.println("At " + barSeries.getLastBar().getEndTime());
            }

            if(goldenCross200I.getValue(barSeries.getEndIndex())) {
                indicators.add("golden_cross_200");
                System.out.println(ticker + " has consensio 200");
                System.out.println("At " + barSeries.getLastBar().getEndTime());
            }

            if(td9_13I.getValue(barSeries.getEndIndex()) == 9) {
                indicators.add("td_9_top");
                System.out.println(ticker + " has td 9 top");
                System.out.println("At " + barSeries.getLastBar().getEndTime());
            }

            if(td9_13I.getValue(barSeries.getEndIndex()) == -9) {
                indicators.add("td_9_bottom");
                System.out.println(ticker + " has td 9 bottom");
                System.out.println("At " + barSeries.getLastBar().getEndTime());
            }

            // ----End-----------------------------------------------------------------


            String line = indicators.size() == 0
                    ? ""
                    : ticker + ","  + priceDetail.stream().collect(Collectors.joining(",")) + "," + indicators.stream().collect(Collectors.joining(",") ) + ","

                    // tag[length - 1] is reserved for processedAt
                    + DateTimeHelper.truncateTime(
                            Instant.now().atZone(ZoneId.of("America/New_York"))).toInstant().toEpochMilli();

            appendToFile(selectedTickersFname, line);
        }
    }

    public static void main(String[] args) {
        CommandLine.run(new ProcessCommand(), args);
    }

}
