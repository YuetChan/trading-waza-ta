package com.tycorp.tw_ta.command;

import com.tycorp.tw_ta.config.InfluxConfig;
import com.tycorp.tw_ta.extend_indicator.TD9_13Indicator;
import com.tycorp.tw_ta.script.TwCandle;
import lombok.SneakyThrows;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
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

            List<TwCandle> twCandles = new ArrayList();
            for(var val : vals){
                twCandles.add(
                        new TwCandle(
                                Double.parseDouble(val.get(5).toString()), Double.parseDouble(val.get(3).toString()),
                                Double.parseDouble(val.get(4).toString()), Double.parseDouble(val.get(1).toString()),
                                ZonedDateTime.parse(val.get(6).toString())));
            }

            BarSeries barSeries = TwCandlesToTa4jBarSeries.convert(twCandles, TwCandlesToTa4jBarSeries.Ta4jTimeframe.MINS, 15);
            ClosePriceIndicator closePriceI = new ClosePriceIndicator(barSeries);

            // ---------------------Provide your own indicators-------------------------

            GoldenCrossIndicator consensioCross200I = new GoldenCrossIndicator(
                    new TwSMAIndicator(closePriceI, 20),
                    new TwSMAIndicator(closePriceI, 50),
                    new TwSMAIndicator(closePriceI, 200));
            GoldenCrossIndicator consensioCross100I = new GoldenCrossIndicator(
                    new TwSMAIndicator(closePriceI, 20),
                    new TwSMAIndicator(closePriceI, 50),
                    new TwSMAIndicator(closePriceI, 100));

            TD9_13Indicator td9_13I = new TD9_13Indicator(barSeries);

            System.out.println("Processed " + ticker);

            List<String> tags = new ArrayList();
            if(consensioCross100I.getValue(barSeries.getEndIndex())) {
                tags.add("consensio_100");
                System.out.println(ticker + " has consensio 100");
                System.out.println("At " + barSeries.getLastBar().getEndTime());
            }

            if(consensioCross200I.getValue(barSeries.getEndIndex())) {
                tags.add("consensio_200");
                System.out.println(ticker + " has consensio 200");
                System.out.println("At " + barSeries.getLastBar().getEndTime());
            }

            if(td9_13I.getValue(barSeries.getEndIndex()) == 9) {
                tags.add("td_9");
                System.out.println(ticker + " has td 9");
                System.out.println("At " + barSeries.getLastBar().getEndTime());
            }

            // ---------------------------------------------------------------------------

            String line = tags.size() ==
                    0 ? ""
                    : ticker + ","
                    + tags.stream().collect(Collectors.joining(",") ) + ","
                    + DateTimeHelper.truncateTime(
                            Instant.now().atZone(ZoneId.of("America/New_York"))).toInstant().toEpochMilli();
            appendToFile(selectedTickersFname, line);
        }
    }

    public static void appendToFile(String fname, String line) throws IOException {
        try {
            if(line != "") {
                Files.write(Paths.get(fname), (line + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
            }
        }catch (IOException e) {
            throw e;
        }
    }

    public static void main(String[] args) {
        CommandLine.run(new ProcessCommand(), args);
    }

}
