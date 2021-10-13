package com.tycorp.tw_ta.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import picocli.CommandLine;
import com.tycorp.tw_ta.lib.GsonHelper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * This script will output the stock data from specified file into specified api
 *
 * Steps:
 *  It will execute auth request using the auth config extracted from specified file
 *  It will then extract the jwt from the auth response
 *  Finally, it will execute post requests and output the stock data to the specified api
 */
@CommandLine.Command(
        name = "Output",
        description = "Output stock data based on config"
)
public class OutputCommand implements Runnable {

    @CommandLine.Option(
            names = {"-s", "--selected"},
            required = true,
            description = "Filename that contains the selected tickers with their tags and processed ats.")
    private String selectedTickersFname;
    @CommandLine.Option(
            names = {"-c", "--config"},
            required = true,
            description = "Filename that contains output configs.")
    private String configFname;

    @SneakyThrows
    @Override
    public void run() {
        BufferedReader buffReader;
        try {
            buffReader = new BufferedReader(new FileReader(configFname));

            // Load the auth config from specified file
            String domain = buffReader.readLine();
            String useremail = buffReader.readLine();
            String password = buffReader.readLine();

            // Create auth request payload
            JsonObject authJson = new JsonObject();
            authJson.addProperty("useremail", useremail);
            authJson.addProperty("password", password);

            // Create auth request
            HttpPost authPostReq = new HttpPost(domain + "/users/signin");
            authPostReq.addHeader("Content-Type", "application/json");
            authPostReq.setEntity(new StringEntity(authJson.toString()));

            // Execute the auth request
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse res = httpClient.execute(authPostReq);

            // Convert the response to string and parse it as json, then extract the jwt
            String resStr = EntityUtils.toString(res.getEntity());

            String jwtStr = new JsonParser().parse(resStr).getAsJsonObject().get("jwt").toString();
            String jwt = jwtStr.substring(1, jwtStr.length() - 1);

            buffReader = new BufferedReader(new FileReader(selectedTickersFname));
            // Extract stock data from each line
            for(String line = buffReader.readLine(); line != null; line = buffReader.readLine()) {
                // Stock data separated by ,
                String[] words = line.split(",");

                // The last word is processedAt
                String processedAt = words[words.length - 1];

                // The first word is ticker
                String ticker = words[0];
                // The word in between first and last words are tags
                String[] priceDetail = new String[5];
                System.arraycopy(words, 1, priceDetail, 0, priceDetail.length);

                String[] indicators = new String[words.length - priceDetail.length - 2];
                System.arraycopy(words, 1 + priceDetail.length, indicators, 0, indicators.length);

                // Create post request payload
                JsonObject postJson = new JsonObject();
                postJson.addProperty("processedAt", processedAt);
                postJson.addProperty("slaveId", 1l);
                postJson.addProperty("userId", 1l);

                postJson.addProperty("ticker", ticker);

                JsonObject priceDetailJson = GsonHelper.getJsonObject();
                priceDetailJson.addProperty("open", Double.parseDouble(priceDetail[0]));
                priceDetailJson.addProperty("high", Double.parseDouble(priceDetail[1]));
                priceDetailJson.addProperty("close", Double.parseDouble(priceDetail[2]));
                priceDetailJson.addProperty("low", Double.parseDouble(priceDetail[3]));
                priceDetailJson.addProperty("change", Double.parseDouble(priceDetail[4]));

                postJson.add("priceDetail", priceDetailJson);
                postJson.add("indicators", GsonHelper.createJsonElement(Arrays.asList(indicators)).getAsJsonArray());

                // Create post request
                HttpPost postPostReq = new HttpPost(domain + "/rows");
                postPostReq.addHeader("Content-Type", "application/json");
                postPostReq.setHeader("Authorization", "Bearer " + jwt);
                postPostReq.setEntity(new StringEntity(postJson.toString()));

                // Execute post request
                res = httpClient.execute(postPostReq);
                resStr = EntityUtils.toString(res.getEntity());

                if(resStr.contains("message")) {
                    break;
                }
                System.out.println(resStr);
            }
        } catch(IOException e) {
            throw e;
        }
    }

    public static void main(String[] args) {
        CommandLine.run(new OutputCommand(), args);
    }

}
