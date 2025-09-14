import java.io.BufferedReader;
import java.io.IOException;

public class ClientListener extends Thread {
    private final BufferedReader entrada;
    private final Object serverResponded;

    public ClientListener(BufferedReader entrada, Object serverResponded) {
        this.entrada = entrada;
        this.serverResponded = serverResponded;
    }

    public void run() {
        try {
            while (true) {
                String response = entrada.readLine();

                if (response == null || response.isBlank()) {
                    synchronized (serverResponded) {
                        serverResponded.notify();
                    }
                }

                if (response == null) break;

                System.out.println(response);
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
