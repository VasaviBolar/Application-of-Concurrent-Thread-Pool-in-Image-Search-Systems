import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;

public class RequestHandler {

    public static void handle(Socket socket) {
        try (
                InputStream input = socket.getInputStream();
                OutputStream output = socket.getOutputStream();
        ) {

            StringBuilder headers = new StringBuilder();
            int prev = 0, curr;

            while ((curr = input.read()) != -1) {
                headers.append((char) curr);

                if (prev == '\r' && curr == '\n') {
                    int len = headers.length();
                    if (len >= 4 && headers.substring(len - 4).equals("\r\n\r\n")) {
                        break;
                    }
                }
                prev = curr;
            }

            String headerStr = headers.toString();
            System.out.println(headerStr);

            if (headerStr.startsWith("OPTIONS")) {
                sendCors(output);
                return;
            }

            if (headerStr.startsWith("POST /upload")) {
                handleUpload(headerStr, input, output);
            } else if (headerStr.startsWith("GET /search")) {
                handleSearch(headerStr, output);
            } else if (headerStr.startsWith("GET /images/")) {
                serveImage(headerStr, output);
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

        byte[] data = new byte[contentLength];

        int read = 0;
        while (read < contentLength) {
            int r = input.read(data, read, contentLength - read);
            if (r == -1) break;
            read += r;
        }

        String filePath = ImageService.saveImage(data);

        // 🔥 FIX WINDOWS PATH
        String safePath = filePath.replace("\\", "\\\\");

        URL url = new URL("http://localhost:5000/store");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String json = "{\"image_path\":\"" + safePath + "\"}";

        OutputStream os = conn.getOutputStream();
        os.write(json.getBytes());
        os.close();

        conn.getInputStream().close();

        sendResponse(output, "Image stored and indexed");
    }

    private static void handleSearch(String headers, OutputStream output) throws Exception {

        String query = "cat";

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

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response = br.readLine();

        sendResponse(output, response);
    }

    private static void serveImage(String headers, OutputStream output) throws Exception {

        String path = headers.split(" ")[1].substring(1);

        File file = new File(path);

        if (!file.exists()) {
            sendResponse(output, "Not found");
            return;
        }

        byte[] data = Files.readAllBytes(file.toPath());

        String res =
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: image/jpeg\r\n" +
                "Content-Length: " + data.length + "\r\n" +
                "Access-Control-Allow-Origin: *\r\n\r\n";

        output.write(res.getBytes());
        output.write(data);
    }

    private static void sendCors(OutputStream output) throws IOException {
        String res =
                "HTTP/1.1 200 OK\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type\r\n\r\n";

        output.write(res.getBytes());
    }

    private static void sendResponse(OutputStream output, String msg) throws IOException {
        String res =
                "HTTP/1.1 200 OK\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Type: application/json\r\n\r\n" +
                msg;

        output.write(res.getBytes());
    }
}