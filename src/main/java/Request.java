import java.util.List;

public class Request {

    private final String method;
    private final String path;
    private final List<String> headers;
    private final byte[] body;

    public Request(String method, String path, List<String> headers, byte[] body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getHeader() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }
}

