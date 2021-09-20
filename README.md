## Trading Board TA
Command line application for loading, processing and outputting(to Trading Board) stock data.

### Requirements
- Trading Board running locally / remotely
- docker and docker compose installed
### Build
    mvn package
### Run
    docker-compose up
    cd target

A "influxdb" directory would be created in parent directory for storing influxdb data
### Usage
Load stock data
       
    java -cp <name>-<version>-jar-with-dependencies.jar com.tycorp.tb_ta.command.LoadCommand -l "lastLoadedTicker.txt" -t "tickers.csv"
    
Process stock data
    
    java -cp <name>-<version>-jar-with-dependencies.jar com.tycorp.tb_ta.command.ProcessCommand -s "selectedTickers.txt" -t "tickers.csv"

Output stock data

This will output stock data to Trading Board via Post example.com/posts.
    
    java -cp <name>-<version>-jar-with-dependencies.jar com.tycorp.tb_ta.command.ProcessCommand -s "selectedTickers.txt" -c "config.txt"
    
User should specify "domain", "useremail" and "password" in config.txt.
    
    example.com
    example@gmail.com
    password