## Trading Waza TA
Command line application for loading, processing and outputting(to Trading Waza) stock data.

### Requirements
- Trading Waza running locally / remotely
- docker and docker compose installed
### Build
    mvn package
### Run
    docker-compose up
    cd target

A "influxdb" directory would be created in parent directory for storing influxdb data
### Usage
Load stock data
       
    java -cp <name>-<version>-jar-with-dependencies.jar com.tycorp.tw_ta.script.LoadDataToLocalDatasource -l "lastLoadedTicker.txt" -t "tickers.csv"
    
Process stock data
    
    java -cp <name>-<version>-jar-with-dependencies.jar com.tycorp.tw_ta.script.GenerateProcessingResult -s "selectedTickers.txt" -t "tickers.csv"

Output stock data

This will output stock data to Trading Waza via Post example.com/posts.
    
    java -cp <name>-<version>-jar-with-dependencies.jar com.tycorp.tw_ta.script.GenerateProcessingResult -s "selectedTickers.txt" -c "config.txt"
    
User should specify "domain", "useremail" and "password" in config.txt.
    
    example.com
    example@gmail.com
    password