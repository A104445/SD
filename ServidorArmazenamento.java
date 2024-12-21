import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServidorArmazenamento {
    private static final Map<String, byte[]> armazenamento = new HashMap<>();
    private static final Lock lock = new ReentrantLock(); // Lock para sincronização

    public static Map<String, byte[]> getArmazenamento() {
        return armazenamento;
    }

    public static Lock getLock() {
        return lock;
    }
}
