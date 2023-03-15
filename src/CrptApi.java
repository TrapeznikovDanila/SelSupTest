import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private static final String API_PATH = "https://ismp.crpt.ru/api";
    private String token;
    private final Instant timeLimit;
    private final int requestLimit;
    private int requestCount;
    private Gson gson = new Gson();
    private Instant start;
    private HttpClient client = HttpClient.newHttpClient();
    private HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        timeLimit = getTimeLimit(timeUnit);
    }

    private Instant getTimeLimit(TimeUnit timeUnit) {
        switch (timeUnit) {
            case SECONDS:
                return Instant.ofEpochSecond(1);
            case MINUTES:
                return Instant.ofEpochSecond(60);
            case HOURS:
                return Instant.ofEpochSecond(60 * 60);
            default:
                return Instant.ofEpochMilli(1);
        }
    }

    // Создание документа для ввода в оборот товара, произведенного в РФ
    public synchronized String createDocumentToIntroduceGoodsFromRF(Document doc, String signature) {
        doc.setSignature(signature);
        String json = documentToJson(doc);
        String path = API_PATH + "/v3/lk/documents/commissioning/contract/create";
        try {
            return (String) makeRequest(HttpMethods.POST, path, json);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Object makeRequest(HttpMethods method, String path, String json) throws IOException, InterruptedException {
        switch (method) {
            case POST:
                return post(path, json);
            case GET:
                return get(path, json);
            case PUT:
                return put(path, json);
            default:
                return null;
        }
    }

    private String post(String path, String json) throws InterruptedException, IOException {
        final HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(json);
        HttpRequest request = requestBuilder
                .uri(URI.create(path))
                .version(HttpClient.Version.HTTP_1_1)
                .header("content-type", "application/json")
                .header("Authorization", token)
                .POST(body)
                .build();

        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        if (!requestFilter()) {
            Thread.sleep(timeLimit.toEpochMilli());
        }
        HttpResponse<String> response = client.send(request, handler);
        requestCount++;
        return response.body();
    }

    // Другие типы запросов в рамках задания не разрабатывались
    private Object get(String path, String json) {
        return null;
    }

    // Другие типы запросов в рамках задания не разрабатывались
    private Object put(String path, String json) {
        return null;
    }

    private String documentToJson(Object obj) {
        return gson.toJson(obj);

    }

    // Фильтрация запросов к API
    private synchronized boolean requestFilter() throws InterruptedException {
        boolean isTimeLeft = true;
        if (start != null) {
            isTimeLeft = timeLimit.toEpochMilli() > Instant.now().minusMillis(start.toEpochMilli())
                    .toEpochMilli();
        } else {
            start = Instant.now();
        }
        if (isTimeLeft) {
            if (requestCount < requestLimit) {
                return true;
            } else {
                return false;
            }
        } else {
            start = Instant.now();
            return true;
        }
    }

    // Получение идентификационного токена не было в задании
    public void setToken(String token) {
        this.token = token;
    }
}

class Document {
    private String documentFormat;
    private String productDocument;
    private String productGroup;
    private String signature;
    private String type;

    public Document() {
    }

    public Document(String documentFormat, String productDocument, String productGroup, String type) {
        this.documentFormat = documentFormat;
        this.productDocument = productDocument;
        this.productGroup = productGroup;
        this.type = type;
    }

    @Override
    public String toString() {
        return "Documen{" +
                "documentFormat='" + documentFormat + '\'' +
                ", productDocument='" + productDocument + '\'' +
                ", productGroup='" + productGroup + '\'' +
                ", signature='" + signature + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    public String getDocumentFormat() {
        return documentFormat;
    }

    public void setDocumentFormat(String documentFormat) {
        this.documentFormat = documentFormat;
    }

    public String getProductDocument() {
        return productDocument;
    }

    public void setProductDocument(String productDocument) {
        this.productDocument = productDocument;
    }

    public String getProductGroup() {
        return productGroup;
    }

    public void setProductGroup(String productGroup) {
        this.productGroup = productGroup;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

// в документации по работе с API нашёл только 3 метода
enum HttpMethods {
    POST,
    GET,
    PUT
}
