package com.tycorp.tw_ta.script;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tycorp.tw_ta.core.UUIDRequest;
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
import java.time.Instant;
import java.util.Arrays;

import static com.tycorp.tw_ta.lib.FileHelper.appendToFile;

/**
 * This script will output the result from specified file to remote repo
 *
 * Steps:
 *  It will execute auth request using the auth config extracted from config file
 *  It will then extract the jwt from the auth response
 *  Finally, it will execute post requests and output the result to the remote repo via API
 */
@CommandLine.Command(
        name = "Output",
        description = "Output stock data based on config"
)
public class OutputResultToRemoteRepo implements Runnable {

  @CommandLine.Option(
          names = {"-s", "--selected"},
          required = true,
          description = "Filename that contains the result from processing/backtest script.")
  private String resultFname;
  @CommandLine.Option(
          names = {"-c", "--config"},
          required = true,
          description = "Filename that contains output configs.")
  private String configFname;

  @CommandLine.Option(
          names = {"-u", "--uuid"},
          required = true,
          description = "Filename that contains request UUID.")
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

      buffReader = new BufferedReader(new FileReader(resultFname));
      JsonArray postJsonBatch = new JsonArray();

      // extract stock data from each line
      for(String line = buffReader.readLine(); line != null; line = buffReader.readLine()) {
        // stock data separated by ,
        String[] words = line.split(",");

        // the last word is endTimeAt
        String endTimeAtAt = words[words.length - 1];

        // the first word is ticker
        String ticker = words[0];
        // the word in between first and last words are tags
        String[] priceDetail = new String[5];
        System.arraycopy(words, 1, priceDetail, 0, priceDetail.length);

        String[] indicators = new String[words.length - priceDetail.length - 2];
        System.arraycopy(words, 1 + priceDetail.length, indicators, 0, indicators.length);

        // create post request payload
        JsonObject postJson = new JsonObject();

        postJson.addProperty("endTimeAt", endTimeAtAt);
        postJson.addProperty("processedAt", Instant.now().toEpochMilli());

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

      // kong specific implementation
      postPostReq.setHeader("apikey", apikey);

      postPostReq.setEntity(new StringEntity(postJsonBatch.toString()));

      // execute post request
      res = httpClient.execute(postPostReq);
      resStr = EntityUtils.toString(res.getEntity());

      System.out.println(resStr);

      if(!resStr.contains("message")) {
        UUIDRequest uuidReq = new Gson().fromJson(resStr, UUIDRequest.class);
        appendToFile(uuidFname, uuidReq.getRequestUUID());
      }
    } catch(IOException e) {
      throw e;
    }
  }

  public static void main(String[] args) {
    CommandLine.run(new OutputResultToRemoteRepo(), args);
  }

}
