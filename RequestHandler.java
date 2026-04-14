import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

public class RequestHandler {

    public static void handle(Socket socket) {
        try (
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();
        ) {

            StringBuilder requestBuilder = new StringBuilder();
            int prev = 0, curr;

            // Read headers manually
            while ((curr = input.read()) != -1) {
                requestBuilder.append((char) curr);

                if (prev == '\r' && curr == '\n') {
                    int len = requestBuilder.length();
                    if (len >= 4 &&
                        requestBuilder.substring(len - 4).equals("\r\n\r\n")) {
                        break;
                    }
                }
                prev = curr;
            }

            String headers = requestBuilder.toString();
            System.out.println(headers);

            if (headers.startsWith("POST /upload")) {
                handleUpload(headers, input, output);
            } else if (headers.startsWith("GET /search")) {
                handleSearch(headers, output);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleUpload(String headers, InputStream input, OutputStream output) throws Exception {

        int contentLength = 0;

        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }

        byte[] imageBytes = new byte[contentLength];

        int totalRead = 0;
        while (totalRead < contentLength) {
            int read = input.read(imageBytes, totalRead, contentLength - totalRead);
            if (read == -1) break;
            totalRead += read;
        }

        System.out.println("Bytes read: " + totalRead);

        String filePath = ImageService.saveImage(imageBytes);

        // 🔥 Send to Python FAISS
        URL url = new URL("http://localhost:5000/store");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String json = "{\"image_path\":\"" + filePath + "\"}";

        OutputStream os = conn.getOutputStream();
        os.write(json.getBytes());
        os.close();

        conn.getInputStream().close();

        sendResponse(output, "Image stored and indexed");
    }

    private static void handleSearch(String headers, OutputStream output) throws Exception {

        String query = "dog"; // default

        if (headers.contains("?q=")) {
            query = headers.split("q=")[1].split(" ")[0];
        }

        URL url = new URL("http://localhost:5000/search");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String json = "{\"query\":\"" + query + "\"}";

        OutputStream os = conn.getOutputStream();
        os.write(json.getBytes());
        os.close();

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));

        String response = br.readLine();

        sendResponse(output, response);
    }

    private static void sendResponse(OutputStream output, String message) throws IOException {
        String response =
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json\r\n" +
                "\r\n" +
                message;

        output.write(response.getBytes());
        output.flush();
    }
}