import java.io.*;
import java.util.concurrent.locks.ReentrantLock;

class OperationHandler implements Runnable {
    private final DataInputStream in;
    private final DataOutputStream out;
    private final String tipo;
    private final String comandoTipo;
    private final BufferedReader teclado;
    private ReentrantLock lockIn = new ReentrantLock();
    private ReentrantLock lockOut = new ReentrantLock();

    public OperationHandler(DataInputStream in, DataOutputStream out, String tipo, String comandoTipo, BufferedReader teclado) {
        this.in = in;
        this.out = out;
        this.tipo = tipo;
        this.comandoTipo = comandoTipo;
        this.teclado = teclado;
    }

    @Override
    public void run() {
        try {
            lockOut.lock();
            try{
                out.writeUTF(tipo);
                out.writeUTF(comandoTipo);
            } finally {
                lockOut.unlock();
            }

            if (tipo.equals("1")) {
                if (comandoTipo.equals("1")) {
                    System.out.println("Digite a chave:");
                    String chaveTexto = teclado.readLine();
                    lockOut.lock();
                    try{
                        out.writeUTF(chaveTexto);

                        System.out.println("Digite o texto:");
                        String valorTexto = teclado.readLine();
                        byte[] valorBytes = valorTexto.getBytes();
                        out.writeInt(valorBytes.length);
                        out.write(valorBytes);
                    } finally {
                        lockOut.unlock();
                    }

                } else if (comandoTipo.equals("2")) {
                    System.out.println("Digite a quantidade de mensagens:");
                    int repeticoes = Integer.parseInt(teclado.readLine());
                    lockOut.lock();
                    try{
                        out.writeInt(repeticoes);

                        for (int i = 0; i < repeticoes; i++) {
                            System.out.println("Digite a chave:");
                            String chave = teclado.readLine();
                            out.writeUTF(chave);

                            System.out.println("Digite o valor:");
                            String valor = teclado.readLine();
                            byte[] valorBytes = valor.getBytes();
                            out.writeInt(valorBytes.length);
                            out.write(valorBytes);
                        }
                    } finally {
                        lockOut.unlock();
                    }
                }
            } else if (tipo.equals("2")) {
                if (comandoTipo.equals("1")) {
                    System.out.println("Digite a chave:");
                    String chave = teclado.readLine();
                    lockOut.lock();
                    try {
                        out.writeUTF(chave);
                    } finally {
                        lockOut.unlock();
                    }

                    lockIn.lock(); 
                    try {
                        int respostaTamanho = in.readInt();
                        if (respostaTamanho > 0) {
                            byte[] respostaBytes = new byte[respostaTamanho];
                            in.readFully(respostaBytes);
                            System.out.println("Resposta: " + new String(respostaBytes));
                        } else {
                            System.out.println("Chave não encontrada.");
                        }
                    } finally {
                        lockIn.unlock();
                    }

                } else if (comandoTipo.equals("2")) {
                    System.out.println("Quantas chaves deseja consultar?");
                    int quantidade = Integer.parseInt(teclado.readLine());
                    lockOut.lock(); 
                    try{
                        out.writeInt(quantidade);

                        for (int i = 0; i < quantidade; i++) {
                            System.out.println("Digite a chave:");
                            String chave = teclado.readLine();
                            out.writeUTF(chave);
                        }
                    } finally {
                        lockOut.unlock();
                    }

                    lockIn.lock();
                    try {
                        int tamanhoResposta = in.readInt();
                        System.out.println("Número de pares encontrados: " + tamanhoResposta);
                        for (int i = 0; i < tamanhoResposta; i++) {
                            String chave = in.readUTF();
                            int valorTamanho = in.readInt();
                            byte[] valorBytes = new byte[valorTamanho];
                            in.readFully(valorBytes);
                            System.out.println("Chave: " + chave + ", Valor: " + new String(valorBytes));
                        }
                    } finally {
                        lockIn.unlock();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}