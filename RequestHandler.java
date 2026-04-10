import java.io.*;
import java.net.Socket;

public class RequestHandler {

   public static void handle(Socket socket) {
    try (
        InputStream input = socket.getInputStream();
        OutputStream output = socket.getOutputStream();
    ) {

        StringBuilder requestBuilder = new StringBuilder();
        int prev = 0, curr;

        // Read headers manually (until \r\n\r\n)
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

        // Extract Content-Length
        int contentLength = 0;
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
        }

        if (headers.startsWith("POST /upload")) {

            if (contentLength <= 0) {
                sendResponse(output, "Invalid Content-Length");
                return;
            }

            byte[] imageBytes = new byte[contentLength];

            int totalRead = 0;
            while (totalRead < contentLength) {
                int bytesRead = input.read(imageBytes, totalRead, contentLength - totalRead);
                if (bytesRead == -1) break;
                totalRead += bytesRead;
            }

            System.out.println("Bytes read: " + totalRead);

            String filePath = ImageService.saveImage(imageBytes);

            sendResponse(output, "Image saved at: " + filePath);
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}

    private static void handleUpload(BufferedReader reader, InputStream input, OutputStream output) throws Exception {

    int contentLength = 0;
    String line;

    // Read headers
    while (!(line = reader.readLine()).isEmpty()) {
        System.out.println(line);

        if (line.toLowerCase().startsWith("content-length:")) {
            contentLength = Integer.parseInt(line.split(":")[1].trim());
        }
    }

    if (contentLength == 0) {
        sendResponse(output, "No Content-Length found");
        return;
    }

    byte[] imageBytes = new byte[contentLength];
    int totalRead = 0;

    while (totalRead < contentLength) {
        int bytesRead = input.read(imageBytes, totalRead, contentLength - totalRead);
        if (bytesRead == -1) break;
        totalRead += bytesRead;
    }

    String filePath = ImageService.saveImage(imageBytes);

    sendResponse(output, "Image saved at: " + filePath);
}

    private static void handleSearch(OutputStream output) throws IOException {
        // dummy response for now
        sendResponse(output, "Search result: (implement later)");
    }

    private static void sendResponse(OutputStream output, String message) throws IOException {
        String response =
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                message;

        output.write(response.getBytes());
        output.flush();
    }
}