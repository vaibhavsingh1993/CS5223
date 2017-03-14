import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AllPeerNotifier extends Remote {
    void sendResponse(String response) throws RemoteException;
    void closeClient() throws RemoteException;
}