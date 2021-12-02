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
        description = "Output result to remote repo based on config"
)
public class OutputResultToRemoteRepo implements Runnable {

  @CommandLine.Option(
          names = {"-r", "--result"},
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

  private String signInURL = "/users/signin";
  private String batchURL = "/rows/batch";

  private JsonArray postJsonBatch = new JsonArray();
  private JsonObject postJson = new JsonObject();

  private JsonObject priceDetailJson = GsonHelper.getJsonObject();

  private CloseableHttpClient httpClient = HttpClients.createDefault();

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

      // create auth request
      HttpPost authPostReq = new HttpPost(domain + signInURL);

      // create auth request payload
      authPostReq.addHeader("Content-Type", "application/json");
      authPostReq.setEntity(new StringEntity(getAuthJsonStr(useremail, password)));

      // execute the auth request
      CloseableHttpResponse res = httpClient.execute(authPostReq);

      // convert the response to string and parse it as json, then extract the jwt
      String resStr = EntityUtils.toString(res.getEntity());
      String jwt = extractJwtStr(resStr);

      buffReader = new BufferedReader(new FileReader(resultFname));

      // extract results from each line
      for(String line = buffReader.readLine(); line != null; line = buffReader.readLine()) {
        // result separated by ,
        String[] words = line.split(",");

        // the second last word is endTimeAt
        String endTimeAtAt = words[words.length - 2];
        // the last word is processedAt
        String processedAt = words[words.length - 1];

        // the first word is ticker
        String ticker = words[0];

        // the word in between first and last words are price details and indicators
        String[] priceDetail = new String[5];
        System.arraycopy(words, 1, priceDetail, 0, priceDetail.length);

        String[] indicators = new String[words.length - priceDetail.length - 2];
        System.arraycopy(words, 1 + priceDetail.length, indicators, 0, indicators.length);

        postJson.addProperty("endTimeAt", endTimeAtAt);
        postJson.addProperty("processedAt", processedAt);

        postJson.addProperty("userId", 1l);
        postJson.addProperty("ticker", ticker);

        priceDetailJson.addProperty("open", Double.parseDouble(priceDetail[0]));
        priceDetailJson.addProperty("high", Double.parseDouble(priceDetail[1]));
        priceDetailJson.addProperty("close", Double.parseDouble(priceDetail[2]));
        priceDetailJson.addProperty("low", Double.parseDouble(priceDetail[3]));

        priceDetailJson.addProperty("change", Double.parseDouble(priceDetail[4]));
        postJson.add("priceDetail", priceDetailJson);

        postJson.add("indicators", GsonHelper.createJsonElement(Arrays.asList(indicators)).getAsJsonArray());

        // added to postJsonBatch
        postJsonBatch.add(postJson);
      }

      // create post request
      HttpPost postPostReq = new HttpPost(domain + batchURL);
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

  public String getAuthJsonStr(String useremail, String password) {
    JsonObject authJson = new JsonObject();
    authJson.addProperty("useremail", useremail);
    authJson.addProperty("password", password);

    return authJson.toString();
  }

  public String extractJwtStr(String resStr) {
    String jwt = new JsonParser().parse(resStr).getAsJsonObject().get("jwt").toString();
    return jwt.substring(1, jwt.length() - 1);
  }

  public static void main(String[] args) {
    CommandLine.run(new OutputResultToRemoteRepo(), args);
  }

}
