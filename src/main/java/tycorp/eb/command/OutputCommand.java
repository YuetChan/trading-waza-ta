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
            description = "The file that contains the selected tickers with their tags and processed ats.")
    private String selectedTickersFname;
    @CommandLine.Option(
            names = {"-c", "--config"},
            required = true,
            description = "The file that contains output configs.")
    private String configFname;

    @SneakyThrows
    @Override
    public void run() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(configFname));

            var authUrl = reader.readLine();
            var uploadUrl = reader.readLine();
            var useremail = reader.readLine();
            var password = reader.readLine();

            HttpPost authPostReq = new HttpPost(authUrl);
            authPostReq.addHeader("content-type", "application/json");

            var authJson = new JsonObject();
            authJson.addProperty("useremail", useremail);
            authJson.addProperty("password", password);

            authPostReq.setEntity(new StringEntity(authJson.toString()));
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse res = httpClient.execute(authPostReq);

            String resStr = EntityUtils.toString(res.getEntity());
            var jwt = new JsonParser().parse(resStr).getAsJsonObject().get("jwt").toString();

            reader = new BufferedReader(new FileReader(selectedTickersFname));
            for(String line = reader.readLine(); line != null; line = reader.readLine()) {
                var words = line.split(",");

                var tickerName = words[0];
                var tagNames = new String[words.length - 2];
                System.arraycopy(words, 1, tagNames, 0, tagNames.length);

                var processedAt = words[words.length - 1];

                var threadJson = new JsonObject();
                threadJson.addProperty("processedAt", processedAt);
                threadJson.addProperty("slaveId", 1l);
                threadJson.addProperty("userId", 1l);

                threadJson.addProperty("title", "");
                threadJson.addProperty("dscription", "");
                threadJson.add("contents", new JsonArray());

                threadJson.add("tickerNames", GsonHelper.createJsonElement(Arrays.asList(tickerName))
                        .getAsJsonArray());
                threadJson.add("tagNames", GsonHelper.createJsonElement(Arrays.asList(tagNames))
                        .getAsJsonArray());

                HttpPost threadPostReq = new HttpPost(uploadUrl);
                threadPostReq.setHeader("Authorization", "Bearer " + jwt);
                threadPostReq.setEntity(new StringEntity(threadJson.toString()));

                res = httpClient.execute(threadPostReq);
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
