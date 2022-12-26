import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;


public class Server {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers = new ConcurrentHashMap<>();

    int portNumber;
    int poolSize;
    private final ExecutorService threadPool;

    public Server(int portNumber, int poolSize) {
        this.poolSize = poolSize;
        this.portNumber = portNumber;
        this.threadPool = Executors.newFixedThreadPool(poolSize);
    }

    public void handleClient(Socket socket) {
        ClientHandler clientHandler = new ClientHandler(socket, handlers);
        threadPool.execute(clientHandler);
    }

    public void listen() {
        System.out.println("Server started");

        try (final var serverSocket = new ServerSocket(portNumber)) {
            while (true) {
                final var socket = serverSocket.accept();
                handleClient(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String method, String path, Handler handler) {
        if (!handlers.containsKey(method)) {
            handlers.put(method, new ConcurrentHashMap<>());
        }
        handlers.get(method).put(path, handler);
    }
}
