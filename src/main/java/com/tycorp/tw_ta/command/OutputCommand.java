package com.tycorp.tw_ta.command;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tycorp.tw_ta.script.TwUUIDRequest;
import lombok.SneakyThrows;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import picocli.CommandLine;
import com.tycorp.tw_ta.lib.GsonHelper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import static com.tycorp.tw_ta.lib.FileHelper.appendToFile;

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

    @CommandLine.Option(
            names = {"-u", "--uuid"},
            required = true,
            description = "Filename that contains request UUIDs.")
    private String uuidFname;

    @Value("${kong.apikey}")
    private String apikey;

    @SneakyThrows
    @Override
    public void run() {
        BufferedReader buffReader;

        try {
            buffReader = new BufferedReader(new FileReader(configFname));

            // load the auth config from specified file
            String domain = buffReader.readLine();
            String useremail = buffReader.readLine();
            String password = buffReader.readLine();

            // create auth request payload
            JsonObject authJson = new JsonObject();
            authJson.addProperty("useremail", useremail);
            authJson.addProperty("password", password);

            // create auth request
            HttpPost authPostReq = new HttpPost(domain + "/users/signin");
            authPostReq.addHeader("Content-Type", "application/json");
            authPostReq.setEntity(new StringEntity(authJson.toString()));

            // execute the auth request
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse res = httpClient.execute(authPostReq);

            // convert the response to string and parse it as json, then extract the jwt
            String resStr = EntityUtils.toString(res.getEntity());

            String jwtStr = new JsonParser().parse(resStr).getAsJsonObject().get("jwt").toString();
            String jwt = jwtStr.substring(1, jwtStr.length() - 1);

            buffReader = new BufferedReader(new FileReader(selectedTickersFname));
            JsonArray postJsonBatch = new JsonArray();

            // extract stock data from each line
            for(String line = buffReader.readLine(); line != null; line = buffReader.readLine()) {
                // stock data separated by ,
                String[] words = line.split(",");

                // the last word is processedAt
                String processedAt = words[words.length - 1];

                // the first word is ticker
                String ticker = words[0];
                // the word in between first and last words are tags
                String[] priceDetail = new String[5];
                System.arraycopy(words, 1, priceDetail, 0, priceDetail.length);

                String[] indicators = new String[words.length - priceDetail.length - 2];
                System.arraycopy(words, 1 + priceDetail.length, indicators, 0, indicators.length);

                // create post request payload
                JsonObject postJson = new JsonObject();
                postJson.addProperty("processedAt", processedAt);
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

                // added to postJson Batch
                postJsonBatch.add(postJson);
            }

            // create post request
            HttpPost postPostReq = new HttpPost(domain + "/rows/batch");
            postPostReq.addHeader("Content-Type", "application/json");
            postPostReq.setHeader("Authorization", "Bearer " + jwt);

            // replace {{}} with your kong consumer apikey for key-auth plugin
            postPostReq.setHeader("apikey", apikey);

            postPostReq.setEntity(new StringEntity(postJsonBatch.toString()));

            // execute post request
            res = httpClient.execute(postPostReq);
            resStr = EntityUtils.toString(res.getEntity());

            System.out.println(resStr);

            if(!resStr.contains("message")) {
                TwUUIDRequest uuidReq = new Gson().fromJson(resStr, TwUUIDRequest.class);
                appendToFile(uuidFname, uuidReq.getRequestUUID());
            }
        } catch(IOException e) {
            throw e;
        }
    }

    public static void main(String[] args) {
        CommandLine.run(new OutputCommand(), args);
    }

}
