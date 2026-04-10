import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int PORT = 8080;
    private static ThreadPool threadPool = new ThreadPool(4);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                // Submit task to thread pool
                threadPool.submit(() -> {
                    RequestHandler.handle(clientSocket);
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}