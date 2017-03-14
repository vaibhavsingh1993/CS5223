import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface ServerSynchronizer extends Remote {
	boolean testIfSecondaryServerAlive() throws RemoteException;
	String getSencondaryServerPeerPlayerId() throws RemoteException;
    void synchronizeNumberOfPlayers(int number) throws RemoteException;
    void synchronizeNormalPeers(Map<String, NormalPeerController> normalPeers) throws RemoteException;
    void synchronizeGameState(GameState gameState) throws RemoteException;
    void synchronizeMazeGameBoardSize(int mazeGameBoardSize) throws RemoteException;
    void synchronizeNumOfTreasures(int numOfTreasures) throws RemoteException;
    void synchronizePlayerPositions(Map<String, String> playerPositions) throws RemoteException;
    void synchronizeTreasurePositions(Map<String, Integer> treasurePositions) throws RemoteException;
    void synchronizePlayerScores(Map<String, Integer> playerScores) throws RemoteException;
    void synchronizeAllPeerNotifiers(Map<String, AllPeerNotifier> allPeerNotifiers) throws RemoteException;
}	
