package tycorp.eb;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import tycorp.eb.extend_indicator.EbConsensioCrossIndicator;
import tycorp.eb.extend_indicator.EbSMAIndicator;
import tycorp.eb.extend_indicator.TD9_13Indicator;
import tycorp.eb.script.EbCandle;
import tycorp.eb.script.EbCandlesToTa4jBarSeries;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EbTaApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(EbTaApplication.class);

    private static String INFLUXDB_URL = "http://127.0.0.1:8086";
    private static String INFLUXDB_NAME = "eb_ta_db";

    private static String DEFAULT_RETENTION_POLICY = "eb_ta_default_retention";
    private static String DEFAULT_RETENTION_DURATION = "3000d";

    public static InfluxDB initInfluxConfig(){
        var influxDB = InfluxDBFactory.connect(INFLUXDB_URL);

//        influxDB.query(new Query("CREATE DATABASE " + dbName));
        influxDB.setDatabase(INFLUXDB_NAME);
        influxDB.query(new Query("CREATE RETENTION POLICY " + DEFAULT_RETENTION_POLICY
                + " ON " + INFLUXDB_NAME + " DURATION " + DEFAULT_RETENTION_DURATION + " REPLICATION 1 DEFAULT"));

        influxDB.setRetentionPolicy(DEFAULT_RETENTION_POLICY);
        influxDB.enableBatch(BatchOptions.DEFAULTS);

        return influxDB;
    }

    public static BatchPoints initInfluxBatchConfig() {
        return BatchPoints
                .database(INFLUXDB_NAME)
                .retentionPolicy(DEFAULT_RETENTION_POLICY)
                .build();
    }

    public static String generateMeasurement(String ticker) { return ticker + "_candles"; }

    public static List<String> loadTickersFromCSV(String filename) throws FileNotFoundException {
        var tickers = new ArrayList<String>();
        try {
            CSVReader csvReader = new CSVReader(new FileReader(new File(filename)));

            String[] values = null;
            while ((values = csvReader.readNext()) != null) {
                var ticker = values[0];
                if(ticker.matches ("[a-zA-Z]+\\.?"))
                    tickers.add(ticker);

            }
        } catch (CsvValidationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tickers;
    }

    public static void main(String[] args) throws FileNotFoundException {
        var influxDB = initInfluxConfig();
        var batchPts = initInfluxBatchConfig();
        var batchCtr = 5000;

//        var tickers = loadTickersFromCSV("/home/yuet/Desktop/personal-project/nasdaq_screener_1626647785339.csv");

        var loadedTickers = Arrays.asList("CRM");
        for(var ticker : loadedTickers) {
//            var measurement = generateMeasurement(ticker);
//
//            var minsCandleResult = influxDB.query(
//                    new Query("SELECT * FROM " + measurement + " WHERE frq=" + "'5_MINS' ORDER BY DESC"));
//            var minsSeries = minsCandleResult.getResults().get(0).getSeries();
//            var minsCandles = minsSeries == null
//                    ? EbCandlesProvider.getEbCandlesSinceDefaultStartDate(ticker, FrequencyType.minute)
//                    : EbCandlesProvider.getEbCandlesSinceLastEndDate(
//                            ticker,
//                            FrequencyType.minute,
//                            ZonedDateTime.parse(
//                                    minsSeries.get(0).getValues().get(0).get(6).toString()));
//            for(var candle : minsCandles){
//                batchPts.point(Point.measurement(measurement)
//                        .time(candle.getStartTimeZdt().toInstant().toEpochMilli(), TimeUnit.MILLISECONDS)
//                        .addField("zdt", candle.getStartTimeZdt().toString())
//                        .tag("frq", "5_MINS")
//                        .addField("open", candle.getOpen())
//                        .addField("high", candle.getHigh())
//                        .addField("low", candle.getLow())
//                        .addField("close", candle.getClose())
//                        .build());
//                batchCtr --;
//
//                if(batchCtr == 0) {
//                    influxDB.write(batchPts);
//                    batchCtr = 5000;
//                }
//            }
//
//            influxDB.write(batchPts);
//            batchCtr = 5000;
//
//            var dailyResult = influxDB.query(
//                    new Query("SELECT * FROM " + measurement  + " WHERE frq=" + "'DAILY' ORDER BY DESC"));
//            var dailySeries = dailyResult.getResults().get(0).getSeries();
//            var dailyCandles = dailySeries == null
//                    ? EbCandlesProvider.getEbCandlesSinceDefaultStartDate(ticker, FrequencyType.daily)
//                    : EbCandlesProvider.getEbCandlesSinceLastEndDate(
//                            ticker,
//                            FrequencyType.daily,
//                            ZonedDateTime.parse(
//                                    dailySeries.get(0).getValues().get(0).get(6).toString()));
//            for(var candle : dailyCandles){
//                batchPts.point(Point.measurement(measurement)
//                        .time(candle.getStartTimeZdt().toInstant().toEpochMilli(), TimeUnit.MILLISECONDS)
//                        .addField("zdt", candle.getStartTimeZdt().toString())
//                        .tag("frq", "DAILY")
//                        .addField("open", candle.getOpen())
//                        .addField("high", candle.getHigh())
//                        .addField("low", candle.getLow())
//                        .addField("close", candle.getClose())
//                        .build());
//                batchCtr --;
//
//                if(batchCtr == 0) {
//                    influxDB.write(batchPts);
//                    batchCtr = 5000;
//                }
//            }
//
//            influxDB.write(batchPts);
        }

        for(var ticker : loadedTickers) {
            var dailyCandleResult = influxDB.query(
                    new Query("SELECT * FROM " + generateMeasurement(ticker)
                            + " WHERE frq=" + "'DAILY' ORDER BY ASC"));

            var series = dailyCandleResult.getResults().get(0).getSeries();
            var vals = series.get(0).getValues();

            var ebCandles = new ArrayList<EbCandle>();
            for(var val : vals){
                ebCandles.add(
                        new EbCandle(
                                Double.parseDouble(val.get(5).toString()), Double.parseDouble(val.get(3).toString()),
                                Double.parseDouble(val.get(4).toString()), Double.parseDouble(val.get(1).toString()),
                                ZonedDateTime.parse(val.get(6).toString())));

            }

            var barSeries = EbCandlesToTa4jBarSeries.convert(ebCandles, EbCandlesToTa4jBarSeries.Ta4jTimeframe.DAILY, 1);

            var closePriceI = new ClosePriceIndicator(barSeries);
            var consensioCrossI = new EbConsensioCrossIndicator(
                    new EbSMAIndicator(closePriceI, 20),
                    new EbSMAIndicator(closePriceI, 50),
                    new EbSMAIndicator(closePriceI, 200));

            var td9And13I = new TD9_13Indicator(barSeries);

            for(int i = 0; i < barSeries.getBarCount(); i++){
//                if(consensioCrossI.getValue(i)){
//                    System.out.println(barSeries.getBar(i));
//                }
                System.out.println(td9And13I.getValue(i));
            }

        }

        System.exit(0);
    }

}
