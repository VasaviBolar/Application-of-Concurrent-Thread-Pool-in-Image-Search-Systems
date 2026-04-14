import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int PORT = 8080;
    private static ThreadPool pool = new ThreadPool(4);

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server running on port " + PORT);

            while (true) {
                Socket client = server.accept();

                pool.submit(() -> {
                    RequestHandler.handle(client);
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}