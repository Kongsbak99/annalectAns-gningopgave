import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Main {

    private static HttpURLConnection connection;

    static String APIkey = "e924a1cf13f34f34ac3160824213108";

    static LocalDate today = LocalDate.now();
    static LocalDate date = today.minusDays(1);

    static String location;

    //Paths
    static String WeatherURL = "http://api.weatherapi.com/v1/history.json?key=" + APIkey + "&q=" + location + "&dt=" + date;
    static String CSVSaveLocationPath = "C:\\Users\\Bruger\\OneDrive\\Programmeringsprojekter\\annalectApplication\\CSVFiles\\day";

    public static void main(String[] args) throws IOException {
        //java.net.HttpURLConnection
        BufferedReader reader; // reader
        String result; //readers line result.
        StringBuffer responseContent = new StringBuffer(); //Final result

        // We start by creating a simple loop, since we wish to get the weather for the last 5 day.
        for (int i = 0; i < 5; i++) {
            //Change the date each time we loop.
            date = date.minusDays(1);

            //We then create another loop, to get the weather from the 3 different location.
            for (int j = 0; j < 3; j++) {
                if (j == 0){
                    location = "Copenhagen";
                }else if(j == 1){
                    location = "Århus";
                }else{
                    location = "Odense";
                }

                //Setup connection and start reading the data
                try {
                    URL url = new URL(WeatherURL);
                    connection = (HttpURLConnection) url.openConnection();

                    // Request setup
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);

                    // Check connection status. Response Code should be 200.
                    int status = connection.getResponseCode();
                    // System.out.println(status);

                    // in case of a connection problem - (Response code too high)
                    if (status > 299){
                        reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                        while((result = reader.readLine()) != null) {
                            responseContent.append(result);
                        }
                        reader.close();
                    } else { //if connection is OK, we want to read the result.
                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        while((result = reader.readLine()) != null) {
                            responseContent.append(result);

                        }
                        reader.close();
                    }

                    // System.out.println(responseContent.toString());

                } catch (Exception e) {
                    e.printStackTrace();
                } finally { // Disconnect HttpURLConnection.
                    connection.disconnect();
                }

                //Using Jackson we will write our CSV file from our jsonObject.
                //Firstly creating a JSon Mapper to read our JSon result from the previous reader
                JsonNode jsonTree = new ObjectMapper().readTree(String.valueOf(responseContent));

                // Then creating a CsvSchema to create the headers for the different columns in our desired CSV file.
                CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
                JsonNode firstObject = jsonTree.findValue("day");
                firstObject.fieldNames().forEachRemaining(
                        fieldName -> {
                            csvSchemaBuilder.addColumn(fieldName);
                        });
                CsvSchema csvSchema = csvSchemaBuilder.build().withHeader().withColumnSeparator('\n');


                //Finally we create a Mapper with the Schema to write our jsonTree to our CSV file.
                CsvMapper mapper = new CsvMapper();
                mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN,true); //To ignore unknown columns.
                mapper.writerFor(JsonNode.class)
                        .with(csvSchema)
                        .writeValue(new File(CSVSaveLocationPath + (i+1) + "_" + location + ".csv"), jsonTree);
            }
        }
    }
}
