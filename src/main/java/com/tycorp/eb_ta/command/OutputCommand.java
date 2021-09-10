package com.tycorp.eb_ta.command;

import com.google.gson.JsonArray;
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
import com.tycorp.eb_ta.lib.GsonHelper;

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
            HttpPost authPostReq = new HttpPost(domain + "/signin");
            authPostReq.addHeader("content-type", "application/json");
            authPostReq.setEntity(new StringEntity(authJson.toString()));

            // Execute the auth request
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse res = httpClient.execute(authPostReq);

            // Convert the response to string and parse it as json, then extract the jwt
            String resStr = EntityUtils.toString(res.getEntity());
            String jwt = new JsonParser().parse(resStr).getAsJsonObject().get("jwt").toString();

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
                String[] tags = new String[words.length - 2];
                System.arraycopy(words, 1, tags, 0, tags.length);

                // Create post request payload
                JsonObject postJson = new JsonObject();
                postJson.addProperty("processedAt", processedAt);
                postJson.addProperty("slaveId", 1l);
                postJson.addProperty("userId", 1l);

                postJson.addProperty("title", "");
                postJson.addProperty("dscription", "");
                postJson.add("contents", new JsonArray());

                postJson.add(
                        "tickers", GsonHelper.createJsonElement(Arrays.asList(ticker)).getAsJsonArray());
                postJson.add(
                        "tags",
                        GsonHelper.createJsonElement(Arrays.asList(tags)).getAsJsonArray());

                // Create post request
                HttpPost postPostReq = new HttpPost(domain + "/posts");
                postPostReq.setHeader("Authorization", "Bearer " + jwt);
                postPostReq.setEntity(new StringEntity(postJson.toString()));

                // Execute post request
                res = httpClient.execute(postPostReq);
                resStr = EntityUtils.toString(res.getEntity());

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
