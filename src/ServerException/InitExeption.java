package ServerException;

import java.io.IOException;

/**
 * Created by belldell on 17.10.16.
 */
public class InitExeption extends IOException {
    public InitExeption() {
    }

    public InitExeption(String message) {
        super(message);
    }

    public InitExeption(String message, Throwable cause) {
        super(message, cause);
    }

    public InitExeption(Throwable cause) {
        super(cause);
    }
}
