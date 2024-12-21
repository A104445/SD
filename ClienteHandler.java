import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.Lock;

public class ClienteHandler implements Runnable {
    private final Socket socket;

    public ClienteHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            // Autenticação simples
            String username = in.readUTF();
            String password = in.readUTF();

            if (autenticar(username, password)) {
                out.writeUTF("Autenticação bem-sucedida. Bem-vindo!");
                System.out.println("Cliente autenticado: " + username);

                // Após autenticação, processa comandos do cliente
                while(true){
                    processarComandos(in, out);
                }
            } else {
                out.writeUTF("Autenticação falhou. Conexão encerrada.");
                System.out.println("Falha na autenticação para: " + username);
            }
        } catch (IOException e) {
            System.out.println("Erro no cliente: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Erro ao fechar o socket: " + e.getMessage());
            }
        }
    }

    private void processarComandos(DataInputStream in, DataOutputStream out) throws IOException {
        while (true) {
            String comando = in.readUTF();
            String comandoTipo = in.readUTF();

            if (comando.equals("1")) {
                String chave = in.readUTF(); // Lê a chave
                int tamanho = in.readInt(); // Lê o tamanho do array de bytes
                byte[] valor = new byte[tamanho];
                in.readFully(valor); // Lê os bytes do cliente

                // Lock para garantir a segurança do acesso ao armazenamento
                Lock lock = ServidorArmazenamento.getLock();
                lock.lock(); // Adquire o lock
                try {
                    ServidorArmazenamento.getArmazenamento().put(chave, valor);
                } finally {
                    lock.unlock(); // Libera o lock
                }

                //out.writeUTF("Par chave-valor armazenado com sucesso.");
                System.out.println("Escrita: [" + chave + "] = " + new String(valor));
            }
            if (comando.equals("2")) {
                String chave = in.readUTF(); // Lê a chave

                // Lock para garantir segurança no acesso ao armazenamento
                Lock lock = ServidorArmazenamento.getLock();
                lock.lock(); // Adquire o lock
                try {
                    byte[] valor = ServidorArmazenamento.getArmazenamento().get(chave);
                    if (valor == null) {
                        out.writeInt(0); // Envia tamanho 0 para indicar que a chave não existe
                        System.out.println("Leitura: Chave não encontrada - " + chave);
                    } else {
                        out.writeInt(valor.length); // Envia o tamanho do valor
                        out.write(valor); // Envia o valor como byte[]
                        System.out.println("Leitura: [" + chave + "] = " + new String(valor));
                    }
                } finally {
                    lock.unlock(); // Libera o lock
                }
            }
            if (comando.equals("0")) {
                break;
            }
        }
    }

    private boolean autenticar(String username, String password) {
        return "admin".equals(username) && "1234".equals(password);
    }
}
