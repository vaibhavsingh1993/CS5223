import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerPeerNotifier extends Remote {
	boolean testIfPrimaryServerAlive() throws RemoteException;
    String joinSecondaryPeerIntoGame(ServerSynchronizer secondaryServerPeer) throws RemoteException;
    String joinNormalPeerIntoGame(NormalPeerController nomralPeerController) throws RemoteException;
    boolean isSecondaryPeerNull() throws RemoteException;
    String getBoardView() throws RemoteException; 
    String sendGameCommandAndGetResponse(String cmd, String playerId) throws RemoteException;
}
