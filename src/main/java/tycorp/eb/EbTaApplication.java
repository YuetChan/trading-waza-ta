package tycorp.eb;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.studerw.tda.model.history.Candle;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tycorp.eb.script.CandlesProvider;
import tycorp.eb.script.CandlesToTa4jBarSeries;
import tycorp.eb.script.SetupDetector;
import tycorp.eb.script.DateTimeHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class EbTaApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(EbTaApplication.class);

    public static Config initRedissonConfig(){
        var config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");

        return config;
    }

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
        var redissonConfig = initRedissonConfig();
        var redissonClient = Redisson.create(redissonConfig);

        RMap<String, List<Candle>> candlesMap = redissonClient.getMap("candlesMap");
        RMap<String, String> initedMap = redissonClient.getMap("initedMap");

        var tickers = loadTickersFromCSV("/home/yuet/Desktop/personal-project/nasdaq_screener_1626647785339.csv");
        var tickerToCandlesMapKey = new HashMap<String, String>();

        for(var ticker : tickers) {
            var candlesMapKey = ticker + " candles" + ":" + DateTimeHelper.getTodayMidNightZdt().toString();
            if(initedMap.get(ticker) == null) {
                initedMap.put(ticker, DateTimeHelper.getTodayMidNightZdt().toString());
                candlesMap.put(
                        candlesMapKey,
                        CandlesProvider.getCandlesSinceDefaultStartDate(ticker));
            }else
                candlesMap.put(
                        candlesMapKey,
                        CandlesProvider.getCandlesSinceYesterday(ticker));


            tickerToCandlesMapKey.put(ticker, candlesMapKey);
        }


        var detecteds = new ArrayList<String>();
        for(var ticker : tickers) {
            if(SetupDetector.isSatified(
                    Arrays.asList(SetupDetector.Setup.SMA_CROSSOVER),
                    CandlesToTa4jBarSeries.convert(
                            candlesMap.get(tickerToCandlesMapKey.get(ticker)),
                            CandlesToTa4jBarSeries.Ta4jTimeframe.DAILY, 1)))
                detecteds.add(ticker);

        }


        redissonClient
                .getMap("alertMap")
                .put(DateTimeHelper.getTodayMidNightZdt().toString(), detecteds);


        System.exit(0);
    }

}
