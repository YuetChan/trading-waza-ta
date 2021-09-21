package com.tycorp.tw_ta.command;

import com.studerw.tda.model.history.FrequencyType;
import com.tycorp.tw_ta.config.InfluxConfig;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import picocli.CommandLine;
import com.tycorp.tw_ta.script.TwCandle;
import com.tycorp.tw_ta.script.TwCandlesProvider;

import java.io.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This script will load the stock data from TD Ameritrade api into database
 * It will load the daily and mins candles data.
 *
 * Steps:
 *  It will extract the tickers in the given CSV file and query each of their stock data in influex db.
 *  If the query is empty, it will load the stock data since default start date.
 *  If the query is not empty, it will load the latest stock data since last end date.
 *  After each load, it will save stock data into influx db
 */
@CommandLine.Command(
        name = "Load",
        description = "Load stock data from TD Ameritrade api into database"
)
public class LoadCommand implements Runnable {

    @CommandLine.Option(
            names = {"-l", "--last"},
            required = true,
            description = "Filename for saving the last loaded ticker.")
    private String lastLoadedTickerFname;
    @CommandLine.Option(
            names = {"-t", "--ticker"},
            required = true,
            description = "CSV filename that contains the tickers list.")
    private String tickersCSV;

    @SneakyThrows
    @Override
    public void run() {
        InfluxDB influxDB = InfluxConfig.initInfluxConfig();

        String lastLoadedTicker = "";
        boolean skip = true;

        BufferedReader buffReader;
        try {
            buffReader = new BufferedReader(new FileReader(lastLoadedTickerFname));
            lastLoadedTicker = buffReader.readLine();
            // Skip to last loaded ticker
            skip = (lastLoadedTicker == null ||  lastLoadedTicker == "") ? false : true;
        }catch(IOException e) {
            throw e;
        }

        List<String> tickers = loadTickersFromCSV(tickersCSV);

        System.out.println("Total of " + tickers.size() + " tickers");
        System.out.println("Last loaded ticker : " + (lastLoadedTicker == null ? "" : lastLoadedTicker));

        for(String ticker : tickers) {
            // Skip until find the last loaded ticker
            if(skip) {
                if(!lastLoadedTicker.equals(ticker)){
                    continue;
                }else {
                    skip = false;
                }
            }

            // Generate standard measurement
            String measurement = InfluxConfig.generateMeasurement(ticker);

            // Query the candles
//            QueryResult minsCandleResult = influxDB.query(
//                    new Query("SELECT * FROM " + measurement + " WHERE frq=" + "'5_MINS' ORDER BY DESC"));
//            List<QueryResult.Series> minsCandlesSeries = minsCandleResult.getResults().get(0).getSeries();
//
//            // If there are no candles, get the candle since default start date
//            // Otherwise, get new candles since last end date
//            List<TwCandle> minsCandles = minsCandlesSeries == null
//                    ? TwCandlesProvider.getTwCandlesSinceDefaultStartDate(ticker, FrequencyType.minute)
//                    : TwCandlesProvider.getTwCandlesSinceLastEndDate(
//                            ticker, FrequencyType.minute, ZonedDateTime.parse(minsCandlesSeries.get(0).getValues().get(0).get(6).toString()));
//
//            // Write the candles to influx db
//            for(TwCandle candle : minsCandles){
//                influxDB.write(Point.measurement(measurement)
//                        .time(candle.getStartTimeZdt().toInstant().toEpochMilli(), TimeUnit.MILLISECONDS)
//                        .addField("zdt", candle.getStartTimeZdt().toString())
//                        .tag("frq", "5_MINS")
//                        .addField("open", candle.getOpen())
//                        .addField("high", candle.getHigh())
//                        .addField("low", candle.getLow())
//                        .addField("close", candle.getClose())
//                        .build());
//            }

            // Query the candles
            QueryResult dailyCandlesResult = influxDB.query(
                    new Query("SELECT * FROM " + measurement  + " WHERE frq=" + "'DAILY' ORDER BY DESC"));
            List<QueryResult.Series> dailyCandlesSeries = dailyCandlesResult.getResults().get(0).getSeries();

            // If there are no candles, get the candle since default start date
            // Otherwise, get new candles since last end date
            List<TwCandle> dailyCandles = dailyCandlesSeries == null
                    ? TwCandlesProvider.getTwCandlesSinceDefaultStartDate(ticker, FrequencyType.daily)
                    : TwCandlesProvider.getTwCandlesSinceLastEndDate(
                            ticker, FrequencyType.daily, ZonedDateTime.parse(dailyCandlesSeries.get(0).getValues().get(0).get(6).toString()));

            // Write the candles to influx db
            for(TwCandle candle : dailyCandles){
                influxDB.write(Point.measurement(measurement)
                        .time(candle.getStartTimeZdt().toInstant().toEpochMilli(), TimeUnit.MILLISECONDS)
                        .addField("zdt", candle.getStartTimeZdt().toString())
                        .tag("frq", "DAILY")
                        .addField("open", candle.getOpen())
                        .addField("high", candle.getHigh())
                        .addField("low", candle.getLow())
                        .addField("close", candle.getClose())
                        .build());
            }

            // Track the last loaded ticker
            writeToFile(lastLoadedTickerFname, ticker);

            System.out.println(ticker + " loaded");
            // Prevent rate limit issue
            Thread.sleep(600);
        }

        writeToFile(lastLoadedTickerFname, "");

        System.out.println("All tickers loaded");
        System.exit(0);
    }

    public static List<String> loadTickersFromCSV(String filename) throws IOException {
        List<String> tickers = new ArrayList();
        CSVParser parser = new CSVParser(new FileReader(new File(filename)), CSVFormat.DEFAULT);

        boolean skipFirst = false;
        List<CSVRecord> records = parser.getRecords();
        for(CSVRecord record : records){
            if(!skipFirst){
                skipFirst = true;
                continue;
            }

            String ticker = record.get(0);
            if(ticker.matches ("[a-zA-Z]+\\.?")){
                tickers.add(ticker);
            }
        }

        return tickers;
    }

    public static void writeToFile(String filename, String content) throws IOException {
        try {
            FileWriter fWriter = new FileWriter(filename);
            fWriter.write(content);
            fWriter.close();
        } catch (IOException e) {
            throw e;
        }
    }

    public static void main(String[] args) {
        CommandLine.run(new LoadCommand(), args);
    }

}
