package com.tycorp.tw_ta.script;

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
import com.tycorp.tw_ta.core.Candle;
import com.tycorp.tw_ta.core.CandlesProvider;

import java.io.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tycorp.tw_ta.lib.FileHelper.writeToFile;

/**
 * This script will load the stock data from TD Ameritrade API into local datasource
 * It will load the data of daily candles .
 *
 * Steps:
 *  It will extract the tickers in the given CSV file and query each of their stock data in local datasource.
 *
 *  If the query is empty, it will load the stock data since default start date.
 *  If the query is not empty, it will load the latest stock data since last end date.
 *  After each load, it will save stock data into local datasource
 */
@CommandLine.Command(
        name = "LoadDataToLocalDatasource",
        description = "Loads stock data from TD Ameritrade API into local datasource"
)
public class LoadDataToLocalDatasource implements Runnable {

  @CommandLine.Option(
          names = {"-l", "--last"},
          required = true,
          description = "Filename for saving the last ticker in case of sudden termination.")
  private String lastTickerFname;
  @CommandLine.Option(
          names = {"-c", "--csv"},
          required = true,
          description = "CSV filename that contains the tickers list.")
  private String csv;


  private InfluxDB influxDB;

  @SneakyThrows
  @Override
  public void run() {
    influxDB = InfluxConfig.initInfluxConfig();

    String lastTicker = "";
    boolean skip = true;

    BufferedReader buffReader;
    try {
      buffReader = new BufferedReader(new FileReader(lastTickerFname));
      lastTicker = buffReader.readLine();

      // skip to last loaded ticker
      skip = (lastTicker == null ||  lastTicker == "") ? false : true;
    }catch(IOException e) {
      throw e;
    }

    List<String> tickers = loadFromCSV(csv);

    System.out.println("Total of " + tickers.size() + " tickers");
    System.out.println("Last loaded ticker : " + (lastTicker == null ? "" : lastTicker));

    for(String ticker : tickers) {
      // skip until it finds the last ticker
      if(skip) {
        if(!lastTicker.equals(ticker)){
          continue;
        }else {
          skip = false;
        }
      }

      // generate standard measurement
      String measurement = InfluxConfig.generateMeasurement(ticker);

      // query the candles
      QueryResult dailyCandlesQry = influxDB.query(new Query("SELECT * FROM " + measurement  + " WHERE frq=" + "'DAILY' ORDER BY DESC"));
      List<QueryResult.Series> series = dailyCandlesQry.getResults().get(0).getSeries();

      // if there are no candles, get the candle since default start date
      // otherwise, get new candles since last end date
      List<Candle> candles = series == null
              ? CandlesProvider.getCandlesSinceDefaultStartDate(ticker, FrequencyType.daily)
              : CandlesProvider.getCandlesSinceLastEndDate(ticker, FrequencyType.daily, ZonedDateTime.parse(series.get(0).getValues().get(0).get(6).toString()));

      // write the candles to influx db
      for(Candle candle : candles){
        influxDB.write(Point.measurement(measurement)
                .time(candle.getStartTimeZdt().toInstant().toEpochMilli(), TimeUnit.MILLISECONDS)

                .addField("open", candle.getOpen())
                .addField("high", candle.getHigh())
                .addField("low", candle.getLow())
                .addField("close", candle.getClose())
                .addField("zdt", candle.getStartTimeZdt().toString())

                .tag("frq", "DAILY")
                .build());
      }

      // track the last loaded ticker in case of script termination
      writeToFile(lastTickerFname, ticker);

      System.out.println(ticker + " loaded");

      // prevent rate limiting issue
      Thread.sleep(600);
    }

    // empty string signals that all tickers are loaded
    writeToFile(lastTickerFname, "");

    System.out.println("All tickers loaded");
    System.exit(0);
  }

  public static List<String> loadFromCSV(String fname) throws IOException {
    List<String> tickers = new ArrayList();
    CSVParser parser = new CSVParser(new FileReader(fname), CSVFormat.DEFAULT);

    // skip header row
    boolean skipFirst = false;
    List<CSVRecord> records = parser.getRecords();
    for(CSVRecord record : records){
      if(!skipFirst){
        skipFirst = true;
        continue;
      }

      String ticker = record.get(0);
      if(ticker.matches ("[a-zA-Z]+\\.?")) {
        tickers.add(ticker);
      }
    }

    return tickers;
  }

  public static void main(String[] args) {
    CommandLine.run(new LoadDataToLocalDatasource(), args);
  }

}
