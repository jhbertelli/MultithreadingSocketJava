import java.io.*;
import java.net.Socket;

public class ClientListener extends Thread {
    private final BufferedReader entrada;
    private final Object serverResponded;
    private final Socket socket;
    
//    public ClientListener(BufferedReader entrada, Object serverResponded) {
//        this.entrada = entrada;
//        this.serverResponded = serverResponded;
    
    public ClientListener(Socket socket, Object serverResponded) throws IOException {
        this.socket = socket;
        this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.serverResponded = serverResponded;
    }

    public void run() {
        try {
            while (true) {
                String response = entrada.readLine();

                if (response == null) {
                    notificarServidorRespondeu();
                    System.out.println("Conexão com o servidor perdida.");
                    break;
                }

                if (response.equals(ServerOperations.END_OF_OPERATION)) {
                    notificarServidorRespondeu();
                    continue;
                }

                //inicio da implemtação da lógica para receber arquivos
                if (response.startsWith(ServerOperations.RECIEVING_FILE)) {
                    String[] parts = response.split(" ");
                    String remetente = parts[1];
                    String nomeArquivo = parts[2];
                    
                    System.out.println("Recebendo arquivo de " + remetente + ": " + nomeArquivo);

                    try (FileOutputStream fileOut = new FileOutputStream(nomeArquivo)) {
                        byte[] buffer = new byte[8192];

                        var entradaRemetente = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                        int fileSize = entradaRemetente.readInt();

                        int totalRead = 0;

                        while (totalRead < fileSize) {
                            int bytesRead = entradaRemetente.read(buffer, 0, Math.min(buffer.length, fileSize - totalRead));
                            if (bytesRead == -1) break;
                            fileOut.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;
                        }

                        fileOut.flush();
                        System.out.println("Transferência de arquivo concluída! Arquivo salvo como: " + nomeArquivo);
                    }
                } else {
                    //se comporta como mensagem de texto
                    System.out.println(response);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private void notificarServidorRespondeu() {
        synchronized (serverResponded) {
            serverResponded.notify();
        }
    }
}
