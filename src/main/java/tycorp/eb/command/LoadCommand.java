package tycorp.eb.command;

import com.studerw.tda.model.history.FrequencyType;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import tycorp.eb.config.InfluxConfig;
import tycorp.eb.script.EbCandlesProvider;

import java.io.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@CommandLine.Command(
        name = "Load",
        description = "Load stock data from TDA provider"
)
public class LoadCommand implements Runnable {

    @CommandLine.Option(
            names = {"-l", "--last"},
            required = true,
            description = "The txt for saving the last loaded ticker.")
    private String lastLoadedTickerFname;
    @CommandLine.Option(
            names = {"-t", "--ticker"},
            required = true,
            description = "The csv that contains tickers list.")
    private String tickersCSV;


    @SneakyThrows
    @Override
    public void run() {
        var influxDB = InfluxConfig.initInfluxConfig();

        var lastLoadedTicker = "";
        var loadedTickerCheck = true;

        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(lastLoadedTickerFname));
            lastLoadedTicker = reader.readLine();
            loadedTickerCheck = lastLoadedTicker == "" ? false : true;
        }catch(IOException e){
            throw e;
        }

        var loadedTickers = loadTickersFromCSV(tickersCSV);

        System.out.println("Total of " + loadedTickers.size() + " tickers");
        System.out.println("Last loaded ticker : " + lastLoadedTicker);

        for(var ticker : loadedTickers) {
            if(loadedTickerCheck && !lastLoadedTicker.equals(ticker) ) {
                continue;
            }else{
                loadedTickerCheck = false;
            }

            var measurement = InfluxConfig.generateMeasurement(ticker);

            var minsCandleResult = influxDB.query(
                    new Query("SELECT * FROM " + measurement + " WHERE frq=" + "'5_MINS' ORDER BY DESC"));
            var minsSeries = minsCandleResult.getResults().get(0).getSeries();
            var minsCandles = minsSeries == null
                    ? EbCandlesProvider.getEbCandlesSinceDefaultStartDate(ticker, FrequencyType.minute)
                    : EbCandlesProvider.getEbCandlesSinceLastEndDate(
                    ticker,
                    FrequencyType.minute,
                    ZonedDateTime.parse(
                            minsSeries.get(0).getValues().get(0).get(6).toString()));
            for(var candle : minsCandles){
                influxDB.write(Point.measurement(measurement)
                        .time(candle.getStartTimeZdt().toInstant().toEpochMilli(), TimeUnit.MILLISECONDS)
                        .addField("zdt", candle.getStartTimeZdt().toString())
                        .tag("frq", "5_MINS")
                        .addField("open", candle.getOpen())
                        .addField("high", candle.getHigh())
                        .addField("low", candle.getLow())
                        .addField("close", candle.getClose())
                        .build());
            }

            var dailyResult = influxDB.query(
                    new Query("SELECT * FROM " + measurement  + " WHERE frq=" + "'DAILY' ORDER BY DESC"));
            var dailySeries = dailyResult.getResults().get(0).getSeries();
            var dailyCandles = dailySeries == null
                    ? EbCandlesProvider.getEbCandlesSinceDefaultStartDate(ticker, FrequencyType.daily)
                    : EbCandlesProvider.getEbCandlesSinceLastEndDate(
                    ticker,
                    FrequencyType.daily,
                    ZonedDateTime.parse(
                            dailySeries.get(0).getValues().get(0).get(6).toString()));
            for(var candle : dailyCandles){
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

            writeToFile(lastLoadedTickerFname, ticker);

            System.out.println(ticker + " loaded");
            Thread.sleep(1500);
        }

        writeToFile(lastLoadedTickerFname, "");

        System.out.println("All tickers loaded");
        System.exit(0);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadCommand.class);

    public static List<String> loadTickersFromCSV(String filename) throws IOException {
        var tickers = new ArrayList<String>();
        CSVParser parser = new CSVParser(new FileReader(new File(filename)), CSVFormat.DEFAULT);

        var skippedFirst = false;
        List<CSVRecord> records = parser.getRecords();
        for(var record : records){
            if(!skippedFirst){
                skippedFirst = true;
                continue;
            }

            var ticker = record.get(0);
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
