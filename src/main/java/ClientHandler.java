import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {

    public static final String GET = "GET";
    public static final String POST = "POST";

    Socket socket;
    ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers;

    public ClientHandler(Socket socket, ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers) {
        this.socket = socket;
        this.handlers = handlers;
    }

    @Override
    public void run() {

        try (
                final var in = new BufferedInputStream(socket.getInputStream());
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {

            final var allowedMethods = List.of(GET, POST);


            // лимит на request line + заголовки
            final var limit = 4096;

            in.mark(limit);
            final var buffer = new byte[limit];
            final var read = in.read(buffer);

            // ищем request line
            final var requestLineDelimiter = new byte[]{'\r', '\n'};
            final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
            if (requestLineEnd == -1) {
                badRequest(out);
                return;
            }

            // читаем request line
            final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
            if (requestLine.length != 3) {
                badRequest(out);
                return;
            }

            final var method = requestLine[0];
            if (!allowedMethods.contains(method)) {
                badRequest(out);
                return;
            }
            System.out.println(method);

            var path = requestLine[1];
            if (!path.startsWith("/")) {
                badRequest(out);
                return;
            }

            //выполняется чтение query, если есть
            if (path.contains("?")) {
                String[] parts = path.split("\\?");
                String query = parts[1];
                getQueryParams(query);
                getQueryParam("key", query);

                path = parts[0];
            }

            System.out.println(path);

            // ищем заголовки
            final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
            final var headersStart = requestLineEnd + requestLineDelimiter.length;
            final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
            if (headersEnd == -1) {
                badRequest(out);
                return;
            }

            // отматываем на начало буфера
            in.reset();
            // пропускаем requestLine
            in.skip(headersStart);

            final var headersBytes = in.readNBytes(headersEnd - headersStart);
            final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
            System.out.println(headers);

            // для GET тела нет
            byte[] bodyResult = null;
            if (!method.equals(GET)) {
                in.skip(headersDelimiter.length);
                // вычитываем Content-Length, чтобы прочитать body
                final var contentLength = extractHeader(headers, "Content-Length");
                if (contentLength.isPresent()) {
                    final var length = Integer.parseInt(contentLength.get());
                    final var bodyBytes = in.readNBytes(length);

                    final var body = new String(bodyBytes);
                    System.out.println(body);
                    bodyResult = body.getBytes();
                }
            }

            Request request = new Request(requestLine[0], path, headers, bodyResult);
            if (!handlers.containsKey(request.getMethod())) {
                send404(out);
                return;
            }

            var pathHandlers = handlers.get((request.getMethod()));

            if (!pathHandlers.containsKey(request.getPath())) {
                send404(out);
                return;
            }

            var handler = pathHandlers.get(request.getPath());
            try {
                handler.handle(request, out);
            } catch (Exception e) {
                System.out.println(e);
            }

            send200(out);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void send200(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public void send404(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public static void getQueryParam(String name, String query) {
        String[] queryParts = query.split("&");
        int count = 0;
        for (String part : queryParts) {
            String[] keyAndValues = part.split("=");
            if (keyAndValues[0].equals(name)) {
                String valueDecoded = URLDecoder.decode(keyAndValues[1], StandardCharsets.UTF_8);
                System.out.println("Для искомого " + name + " значение равно: " + valueDecoded);
                count++;
            }
        }
        if (count == 0) {
            System.out.println("Нет такого значения для key.");
        }
    }

    public static void getQueryParams(String query) {
        String[] queryParts = query.split("&");
        for (String part : queryParts) {
            String[] keyAndValues = part.split("=");
            String key = keyAndValues[0];
            String value = keyAndValues[1];
            String keyDecoded = URLDecoder.decode(key, StandardCharsets.UTF_8);
            String valueDecoded = URLDecoder.decode(value, StandardCharsets.UTF_8);
            System.out.println(keyDecoded + " = " + valueDecoded);
        }
    }
}
