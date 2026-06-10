import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(8080)) {

            System.out.println("Server running on port 8080");

            ThreadPool pool = new ThreadPool(4);

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