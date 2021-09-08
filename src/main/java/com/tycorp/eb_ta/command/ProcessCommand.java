package com.tycorp.eb_ta.command;

import com.tycorp.eb_ta.config.InfluxConfig;
import com.tycorp.eb_ta.script.EbCandle;
import lombok.SneakyThrows;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import picocli.CommandLine;
import com.tycorp.eb_ta.extend_indicator.ConsensioCrossIndicator;
import com.tycorp.eb_ta.extend_indicator.EbSMAIndicator;
import com.tycorp.eb_ta.script.EbCandlesToTa4jBarSeries;
import com.tycorp.eb_ta.lib.DateTimeHelper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @CommandLine.Option(
            names = {"-i", "--indicator"},
            required = true,
            description = "Indicator of choice for processing the data.")
    private String indicator;

    @SneakyThrows
    @Override
    public void run() {
        InfluxDB influxDB = InfluxConfig.initInfluxConfig();

        List<String> loadedTickers = LoadCommand.loadTickersFromCSV(tickersCSV);
        if(indicator.equals(EbSMAIndicator.class.getSimpleName())) {
            for(String ticker : loadedTickers) {
                QueryResult dailyCandleResult = influxDB.query(
                        new Query("SELECT * FROM " + InfluxConfig.generateMeasurement(ticker)
                                + " WHERE frq=" + "'DAILY' ORDER BY ASC"));

                System.out.println(dailyCandleResult);
                List<QueryResult.Series> dailySeries = dailyCandleResult.getResults().get(0).getSeries();
                List<List<Object>> vals = dailySeries.get(0).getValues();

                List<EbCandle> ebCandles = new ArrayList();
                for(var val : vals){
                    ebCandles.add(
                            new EbCandle(
                                    Double.parseDouble(val.get(5).toString()), Double.parseDouble(val.get(3).toString()),
                                    Double.parseDouble(val.get(4).toString()), Double.parseDouble(val.get(1).toString()),
                                    ZonedDateTime.parse(val.get(6).toString())));
                }

                BarSeries barSeries = EbCandlesToTa4jBarSeries.convert(ebCandles, EbCandlesToTa4jBarSeries.Ta4jTimeframe.MINS, 15);
                ClosePriceIndicator closePriceI = new ClosePriceIndicator(barSeries);

                ConsensioCrossIndicator consensioCrossI200 = new ConsensioCrossIndicator(
                        new EbSMAIndicator(closePriceI, 20),
                        new EbSMAIndicator(closePriceI, 50),
                        new EbSMAIndicator(closePriceI, 200));
                ConsensioCrossIndicator consensioCrossI100 = new ConsensioCrossIndicator(
                        new EbSMAIndicator(closePriceI, 20),
                        new EbSMAIndicator(closePriceI, 50),
                        new EbSMAIndicator(closePriceI, 100));

                System.out.println("Processed " + ticker);

                List<String> tags = new ArrayList();
                if(consensioCrossI100.getValue(barSeries.getEndIndex())) {
                    tags.add("consensio 100");
                    System.out.println(ticker + " has consensio 100");
                    System.out.println("At " + barSeries.getLastBar().getEndTime());
                }

                if(consensioCrossI200.getValue(barSeries.getEndIndex())) {
                    tags.add("consensio 200");
                    System.out.println(ticker + " has consensio 200");
                    System.out.println("At " + barSeries.getLastBar().getEndTime());
                }

                String line = tags.size() == 0 ? "" : ticker + "," + tags.stream().collect(Collectors.joining(",") ) + "," + DateTimeHelper.truncateTime(Instant.now().atZone(ZoneId.of("America/New_York")));
                appendToFile(selectedTickersFname, line);
            }
        }
    }

    public static void appendToFile(String filename, String line) throws IOException {
        BufferedWriter buffWriter = null;
        try {
            buffWriter = new BufferedWriter(new FileWriter(filename));
            buffWriter.write(line);
            buffWriter.newLine();
            buffWriter.flush();
        }catch (IOException e) {
            throw e;
        }finally {
            if (buffWriter != null) {
                try {
                    buffWriter.close();
                } catch (IOException e) {
                    throw e;
                }
            }
        }
    }

    public static void main(String[] args) {
        CommandLine.run(new ProcessCommand(), args);
    }

}
