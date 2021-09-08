## Evening Brew TA
Command line application for loading, processing and outputting stock data.

### Requirements
- Evening Brew Portal running locally / remotely
- Influx db running locally
### Build
    mvn package
    cd target

### Usage
Loading stock data
       
    java -cp <name>-<version>-jar-with-dependencies.jar tycorp.eb_ta.command.LoadCommand -l "lastLoadedTicker.txt" -t "tickers.csv"
    

Processing stock data
    
    java -cp <name>-<version>-jar-with-dependencies.jar tycorp.eb_ta.command.ProcessCommand -s "selectedTickers.txt" -t "tickers.csv" -i "indicator"

Outputting stock data

    java -cp <name>-<version>-jar-with-dependencies.jar tycorp.eb_ta.command.ProcessCommand -s "selectedTickers.txt" -c "config.txt"