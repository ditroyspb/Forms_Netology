public class Main {
    public static void main(String[] args) {

        Server server = new Server(9999, 64);

        // добавление handler'ов (обработчиков)

        server.addHandler("GET", "/messages", (request, out) -> {
            var response = "Hello from GET /message";
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Length: " + response.length() + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(response.getBytes());
            out.flush();
        });

        server.addHandler("POST", "/messages", (request, out) -> {
            var response = "Hello from POST /message";
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Length: " + response.length() + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(response.getBytes());
            out.flush();
        });

        server.listen();
    }
}