package tycorp.eb.command;

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
import tycorp.eb.lib.GsonHelper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

@CommandLine.Command(
        name = "Output",
        description = "Output stock data"
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

            String authUrl = buffReader.readLine();
            String uploadUrl = buffReader.readLine();
            String useremail = buffReader.readLine();
            String password = buffReader.readLine();

            JsonObject authJson = new JsonObject();
            authJson.addProperty("useremail", useremail);
            authJson.addProperty("password", password);

            HttpPost authPostReq = new HttpPost(authUrl);
            authPostReq.addHeader("content-type", "application/json");
            authPostReq.setEntity(new StringEntity(authJson.toString()));

            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse res = httpClient.execute(authPostReq);

            String resStr = EntityUtils.toString(res.getEntity());
            String jwt = new JsonParser().parse(resStr).getAsJsonObject().get("jwt").toString();

            buffReader = new BufferedReader(new FileReader(selectedTickersFname));
            for(String line = buffReader.readLine(); line != null; line = buffReader.readLine()) {
                String[] words = line.split(",");

                String processedAt = words[words.length - 1];

                String ticker = words[0];
                String[] tags = new String[words.length - 2];
                System.arraycopy(words, 1, tags, 0, tags.length);

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

                HttpPost postPostReq = new HttpPost(uploadUrl);
                postPostReq.setHeader("Authorization", "Bearer " + jwt);
                postPostReq.setEntity(new StringEntity(postJson.toString()));

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
