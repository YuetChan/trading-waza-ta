package com.tycorp.eb_ta.config;

import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

public class InfluxConfig {

    private static String INFLUXDB_URL = "http://127.0.0.1:8086";
    private static String INFLUXDB_NAME = "eb_ta_db";

    private static String DEFAULT_RETENTION_POLICY = "eb_ta_default_retention";

    public static InfluxDB initInfluxConfig(){
        InfluxDB influxDB = InfluxDBFactory.connect(INFLUXDB_URL);

        influxDB.setDatabase(INFLUXDB_NAME);

        influxDB.setRetentionPolicy(DEFAULT_RETENTION_POLICY);
        influxDB.enableBatch(BatchOptions.DEFAULTS.actions(2000).flushDuration(100));

        return influxDB;
    }

    public static String generateMeasurement(String ticker) {
        return ticker + "_candles";
    }

}
