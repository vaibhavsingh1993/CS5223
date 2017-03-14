import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NormalPeerController extends Remote {
   void updatePrimaryServer(ServerPeerNotifier newPrimaryServerPeer) throws RemoteException; 
   void makeSelfSecondaryServer() throws RemoteException;
}
