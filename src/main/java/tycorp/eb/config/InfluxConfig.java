package tycorp.eb.config;

import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;

public class InfluxConfig {

    private static String INFLUXDB_URL = "http://127.0.0.1:8086";
    private static String INFLUXDB_NAME = "eb_ta_db";

    private static String DEFAULT_RETENTION_POLICY = "eb_ta_default_retention";
//    private static String DEFAULT_RETENTION_DURATION = "3000d";

    public static InfluxDB initInfluxConfig(){
        var influxDB = InfluxDBFactory.connect(INFLUXDB_URL);

        influxDB.setDatabase(INFLUXDB_NAME);
//        influxDB.query(new Query("CREATE RETENTION POLICY " + DEFAULT_RETENTION_POLICY
//                + " ON " + INFLUXDB_NAME + " DURATION " + DEFAULT_RETENTION_DURATION + " REPLICATION 1 DEFAULT"));

        influxDB.setRetentionPolicy(DEFAULT_RETENTION_POLICY);
        influxDB.enableBatch(BatchOptions.DEFAULTS.actions(2000).flushDuration(100));

        return influxDB;
    }

    public static String generateMeasurement(String ticker) { return ticker + "_candles"; }

}
