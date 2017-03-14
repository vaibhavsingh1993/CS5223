/*
 * Copyright 2004 Sun Microsystems, Inc. All  Rights Reserved.
 *  
 * Redistribution and use in source and binary forms, with or 
 * without modification, are permitted provided that the following 
 * conditions are met:
 * 
 * -Redistributions of source code must retain the above copyright  
 *  notice, this list of conditions and the following disclaimer.
 * 
 * -Redistribution in binary form must reproduce the above copyright 
 *  notice, this list of conditions and the following disclaimer in 
 *  the documentation and/or other materials provided with the 
 *  distribution.
 *  
 * Neither the name of Sun Microsystems, Inc. or the names of 
 * contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *  
 * You acknowledge that Software is not designed, licensed or 
 * intended for use in the design, construction, operation or 
 * maintenance of any nuclear facility.
 */

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

public class Peer implements ServerPeerNotifier,
                             ServerSynchronizer,
                             NormalPeerController,
                             AllPeerNotifier,
                             Runnable {

    private static final String BIND_NAME_PRIMARY_SERVER = "PrimaryServerPeer";

    private static final int CONST_JOING_PERIOD = 20000;

    private static final String PLAYER_COMMAND_MOVE_UP = "w";
    private static final String PLAYER_COMMAND_MOVE_DOWN = "s";
    private static final String PLAYER_COMMAND_MOVE_LEFT = "a";
    private static final String PLAYER_COMMAND_MOVE_RIGHT = "d";
    private static final String PLAYER_COMMAND_MOVE_STAY = "n";
    private static final String PLAYER_COMMAND_MOVE_QUIT = "q";

    private static final String THREAD_NAME_PRIMARY_SERVER_MONITOR = "PrimaryServerMonitor";
    private static final String THREAD_NAME_SECONDARY_SERVER_MONITOR = "SecondaryServerMonitor";
    private static final String THREAD_NAME_DELAY_STARTER = "DelayStarter";

    private static final String ERROR_IO_EXCEPTION = "IO_EXCEPTION";
    private static final String ERROR_INVALID_COMMAND = "INVALID_COMMAND";
    private static final String ERROR_PRIMARY_SERVER_BIND_EXCEPTION = "Primary server peer binding exception: ";
    private static final String ERROR_SECONDARY_OR_NORMAL_PEER_JOIN_EXCEPTION = "Secondary or normal peer joining exception: ";
    private static final String MSG_READING_IO_EXCEPTION = "IOException occurred when reading user input";
    private static final String MSG_INPUT_NOT_INTEGER = "Parameters provided should be integers";
    private static final String MSG_REMOTE_EXCEPTION = "RemoteException :";
    private static final String MSG_INTERRUPTED_EXCEPTION = "Delay was interrupted";

    private static final String MSG_NO_RESP_FROM_SERVER_TRY_AGAIN = "Fail to get response from server, please try again later...";
    private static final String MSG_REQUEST_USER_COMMAND = "Please enter command : ";
    private static final String MSG_ENTERED_USER_COMMAND = "Entered command : ";
    private static final String MSG_INVALID_COMMAND = "Command is invalid";
    private static final String MSG_SENDING_COMMAND_TO_SERVER = "Sending command to primary server...";
    private static final String MSG_RESPONSE_FROM_SERVER_MSG = "Response from primary Server: ";
    private static final String MSG_EXITING_GAME = "Exiting game now...";
    private static final String MSG_DISCONNECTED_FROM_SERVER = "Disconnected from the primary Server";

    private static final String MSG_SERVER_PARAM_FORMAT = "Required Format: java Server <BoardSize> <Num of Treasures>";

    private static final String MSG_GAME_JOIN_FAILURE_GAME_OVER = "GAME_JOIN_FAILURE: Sorry, game is over";
    private static final String MSG_GAME_JOIN_FAILURE_GAME_STARTED = "GAME_JOIN_FAILURE: Sorry, game has been started";
    private static final String MSG_WELCOME_NEW_PLAYER = "Hello, Player ";
    private static final String MSG_INDENTIFY_SECONDARY_SERVER = " [Secondary Server Peer]";
    private static final String MSG_INDENTIFY_NORMAL_PEER = " [Normal Peer]";
    private static final String MSG_GAME_NOW_START = "%ds passed, game now starts";
    private static final String MSG_COMMAND_RESPONSE_CODE_SUCCESS = "SUCCESS\n";
    private static final String MSG_COMMAND_RESPONSE_CODE_ERROR_NO_PLAYER_POS = "ERROR: Unable to get player position";
    private static final String MSG_COMMAND_RESPONSE_CODE_ERROR_OUT_OF_BOARD = "ERROR: Player cannot move out of maze board";
    private static final String MSG_COMMAND_RESPONSE_CODE_ERROR_NEXT_POS_OCCUPIED = "ERROR: Cannot move to a position occupied by others";
    private static final String MSG_COMMAND_RESPONSE_CODE_SUCCESS_NEW_TREASURE_FOUND = "Player %s found %d new treasure(s)!\n";
    private static final String MSG_COMMAND_RESPONSE_CODE_SUCCESS_NO_TREASURE_FOUND = "No new treasure found\n";
    private static final String MSG_GAME_IS_OVER = "Game is Over";
    private static final String MSG_GAME_NOT_YET_START = "Game is not started yet..";
    private static final String MSG_ALL_TREASURES_FOUND_USER = "All treasures found, game is over. SEE YA";
    private static final String MSG_PLAYER_QUIT_WHILE_GAME_ON = "What a pity, u r almost there... Goodbye";

    private static final String MSG_NEED_REBIND = "Duplicated Bind Found, trying to rebind..";
    private static final String MSG_GAME_ENVIRONMENT_SET_UP = "New Game Environment Has Been Set Up.";
    private static final String MSG_PRIMARY_SERVER_READY = "Primary server peer ready";
    private static final String MSG_NEW_PLAYER_JOINED = "New player joined : Player ";
    private static final String MSG_PLAYER_QUIT_TO_SERVER = "Player %s has quit the game";
    private static final String MSG_COMMAND_FROM_PLAYER = "Server recieved command from player %s : %s";
    private static final String MSG_ALL_TREASURES_FOUND_SERVER = "All treasures found, game is over.\n";

    private static final String LABEL_SCORE_BOARD_START = "\n================= Score Board =================\n";
    private static final String LABEL_PLAYER_SCORE = "Player %s: %d\n";
    private static final String LABEL_SCORE_BOARD_END = "===============================================\n\n";

    private static final String MSG_NORMAL_PEERS_SYNC_SUCCESS = "Normal peers synchronized";
    private static final String MSG_PLAYER_POS_SYNC_SUCCESS = "Player positions synchronized";  
    private static final String MSG_TREASURE_POS_SYNC_SUCCESS = "Treasure positions synchronized";
    private static final String MSG_PLAYER_SCORE_SYNC_SUCCESS = "Player scores synchronized";
    private static final String MSG_PEER_NOTIFIERS_SYNC_SUCCESS = "All peer notifiers synchronized";

    private static final String MSG_MAKING_SECONDARY_NEW_PRIMARY_SERVER = "Going to make secondary Server to be new Primary Server...";
    private static final String MSG_UPDATED_PRIMARY_SERVER_FOR_PLAYER = "Primary Server updated for player ";
    private static final String MSG_FAILED_TO_UPDATE_PRIMARY_SERVER_FOR_PLAYER = "Failed to update primary server for player ";
    private static final String MSG_SECONDARY_TO_PRIMARY_SERVER_SUCCESS = "Secondary server is now the new primary server";

    private static final String MSG_TO_SELECT_NEW_SECONDARY_SERVER = "Going to select new Secondary Server...";
    private static final String MSG_NEW_SECONDARY_SERVER_SELECTED = "Player %s is now the new Secondary Server";
    private static final String MSG_TRY_MAKE_NEXT_PEER_NEW_SECONDARY_SERVER = "Failed to make player %s the new secondary server, trying next player...";

    private static GameState gameState;
    private int mazeGameBoardSize;
    private int numOfTreasures;
    private int numberOfPlayers;

    private Map<String, String> playerPositions;
    private Map<String, Integer> treasurePositions;
    private Map<String, Integer> playerScores;

    private ServerPeerNotifier primaryServerPeer = null;
    private ServerSynchronizer secondaryServerPeer = null;
    private Map<String, NormalPeerController> normalPeers = null;
    private Map<String, AllPeerNotifier> allPeerNotifiers = null;

    private String playerId = null;


 	public Peer() throws RemoteException{}

    public Peer(ServerPeerNotifier primaryServerPeer) throws RemoteException{
    	this.primaryServerPeer = primaryServerPeer;
    }


    //Runnable Methods==============================================================================
    public void run(){

        String threadName =  Thread. currentThread(). getName();

        if(threadName.contentEquals(THREAD_NAME_PRIMARY_SERVER_MONITOR)){
            this.monitorPrimaryServer();
            //primary server is dead, make secondary peer new primary server
            if(this.gameState != GameState.OVER){
                this.makeSecondaryNewPrimary();
                this.selectNewSecondaryServer();
            } 
        } else if(threadName.contentEquals(THREAD_NAME_SECONDARY_SERVER_MONITOR)){
            this.monitorSecondaryServer();
            //secondary server is dead, select a new secondary server
            if(this.gameState != GameState.OVER){
                this.selectNewSecondaryServer();
            } 
        } else if(threadName.contentEquals(THREAD_NAME_DELAY_STARTER)){
            this.startGameAfterDelay();
        }
    }
    //==============================================================================================


    //Server Peer Notifier methods==================================================================

    public synchronized boolean testIfPrimaryServerAlive() throws RemoteException{
        return true;
    }

    public synchronized String joinSecondaryPeerIntoGame(ServerSynchronizer secondaryServerPeer) throws RemoteException{

        if(gameState == GameState.OVER){
            return MSG_GAME_JOIN_FAILURE_GAME_OVER;
        }

        if(gameState == GameState.WAITING) {
            gameState = GameState.JOINING;
            Thread newThread = new Thread(this);
            newThread.setName(THREAD_NAME_DELAY_STARTER);
            newThread.start();
        }

    	this.numberOfPlayers ++;
    	this.secondaryServerPeer = secondaryServerPeer;
        String newPlayerNumId = "" + this.numberOfPlayers;

        Coordinate newPlayerPos = getCleanStartCoordinate(newPlayerNumId);
        playerPositions.put(newPlayerNumId, newPlayerPos.getCoordinateString());
        playerScores.put(newPlayerNumId, 0);

        this.allPeerNotifiers.put(newPlayerNumId, (AllPeerNotifier) secondaryServerPeer);

        displayMsg(MSG_NEW_PLAYER_JOINED + newPlayerNumId);

        Thread newThread = new Thread(this);
        newThread.setName(THREAD_NAME_SECONDARY_SERVER_MONITOR);
        newThread.start();

    	return MSG_WELCOME_NEW_PLAYER + newPlayerNumId + MSG_INDENTIFY_SECONDARY_SERVER;
    }
	
    public synchronized String joinNormalPeerIntoGame(NormalPeerController nomralPeerController) throws RemoteException{
        if(gameState == GameState.OVER){
            return MSG_GAME_JOIN_FAILURE_GAME_OVER;
        }

        if(gameState == GameState.STARTED){
            return MSG_GAME_JOIN_FAILURE_GAME_STARTED;
        }

        this.numberOfPlayers ++;
        String newPlayerNumId = "" + this.numberOfPlayers;
        this.normalPeers.put(newPlayerNumId, nomralPeerController);

        Coordinate newPlayerPos = getCleanStartCoordinate(newPlayerNumId);
        playerPositions.put(newPlayerNumId, newPlayerPos.getCoordinateString());
        playerScores.put(newPlayerNumId, 0);
        this.allPeerNotifiers.put(newPlayerNumId, (AllPeerNotifier) nomralPeerController);

        displayMsg(MSG_NEW_PLAYER_JOINED + newPlayerNumId);

        this.secondaryServerPeer.synchronizeNormalPeers(this.normalPeers);
        return MSG_WELCOME_NEW_PLAYER + newPlayerNumId + MSG_INDENTIFY_NORMAL_PEER;
    }

    public synchronized boolean isSecondaryPeerNull() throws RemoteException{
        return this.secondaryServerPeer == null;
    }

    public synchronized String sendGameCommandAndGetResponse(String cmd, String playerId) throws RemoteException{

        displayMsg(String.format(MSG_COMMAND_FROM_PLAYER, playerId, cmd));
        String reponse = null;


        if(this.gameState == GameState.STARTED){
            String processReulst = processCommand(cmd, playerId);
            String scoreBoard = getScoreBoard(); 
            String curGameView = getBoardView();
            reponse = processReulst + scoreBoard + curGameView;
        } else if (this.gameState == GameState.OVER){
            String processReulst = MSG_GAME_IS_OVER;
            String scoreBoard = getScoreBoard(); 
            String curGameView = getBoardView();
            reponse = processReulst + scoreBoard + curGameView;
            System.exit(0);
        } else if(this.gameState == GameState.JOINING && 
                  cmd.contentEquals(PLAYER_COMMAND_MOVE_QUIT)){
            String processReulst = processCommand(cmd, playerId);
            reponse = processReulst;
        } else {
            String processReulst = MSG_GAME_NOT_YET_START;
            reponse = processReulst;
        }

        if(this.secondaryServerPeer != null){
           synchronizeAll();
        }

        if(this.gameState == GameState.STARTED && areAllTreasuresFound()){
            this.gameState = GameState.OVER;
            this.notifyGameOver();
            try{
                Thread.sleep(5000);
            } catch (InterruptedException e){
                System.exit(1);
            }
            System.exit(1);
        }
    
        return reponse;
        
    } 

    public synchronized String getBoardView(){
        return getBoardViewFromServer();
    }

    //=============================================================================================


    //Noraml Peer Controller Methods=================================================================

    public void updatePrimaryServer(ServerPeerNotifier newPrimaryServerPeer) throws RemoteException{
        this.primaryServerPeer = newPrimaryServerPeer;
    }

    public void makeSelfSecondaryServer() throws RemoteException{
        this.secondaryServerPeer = null;
        this.normalPeers = new HashMap<String, NormalPeerController>();

        Thread newThread = new Thread(this);
        newThread.setName(THREAD_NAME_PRIMARY_SERVER_MONITOR);
        newThread.start();

    }
    //=============================================================================================




    //Server Synchronizer methods==================================================================

    public boolean testIfSecondaryServerAlive() throws RemoteException{
        return true;
    }

    public String getSencondaryServerPeerPlayerId() throws RemoteException{
        return this.playerId;
    }

    public void synchronizeNumberOfPlayers(int number) throws RemoteException{
        this.numberOfPlayers = number;
    }

    public void synchronizeNormalPeers(Map<String, NormalPeerController> normalPeers) throws RemoteException{

        this.normalPeers = new HashMap<String, NormalPeerController>();

        for(String key : normalPeers.keySet()){
            this.normalPeers.put(key, normalPeers.get(key));
        }
        displayMsg(MSG_NORMAL_PEERS_SYNC_SUCCESS);
    }

    public void synchronizeGameState(GameState gameState) throws RemoteException{
        this.gameState = gameState;
    }

    public void synchronizeMazeGameBoardSize(int mazeGameBoardSize) throws RemoteException{
        this.mazeGameBoardSize = mazeGameBoardSize;
    }

    public void synchronizeNumOfTreasures(int numOfTreasures) throws RemoteException{
        this.numOfTreasures = numOfTreasures; 
    }

    public void synchronizePlayerPositions(Map<String, String> playerPositions) throws RemoteException{
        
        this.playerPositions = new HashMap<String, String>();

        for(String key : playerPositions.keySet()){
            this.playerPositions.put(key, playerPositions.get(key));
        }
        displayMsg(MSG_PLAYER_POS_SYNC_SUCCESS);
    }

    public void synchronizeTreasurePositions(Map<String, Integer> treasurePositions) throws RemoteException{

        this.treasurePositions = new HashMap<String, Integer>();

        for(String key : treasurePositions.keySet()){
            this.treasurePositions.put(key, treasurePositions.get(key));
        }

        displayMsg(MSG_TREASURE_POS_SYNC_SUCCESS);
    }

    public void synchronizePlayerScores(Map<String, Integer> playerScores) throws RemoteException{
    
        this.playerScores = new HashMap<String, Integer>();

        for(String key : playerScores.keySet()){
            this.playerScores.put(key, playerScores.get(key));
        }

        displayMsg(MSG_PLAYER_SCORE_SYNC_SUCCESS);
    }

    public void synchronizeAllPeerNotifiers(Map<String, AllPeerNotifier> allPeerNotifiers) throws RemoteException{
        this.allPeerNotifiers = new HashMap<String, AllPeerNotifier>();

        for(String key : allPeerNotifiers.keySet()){
            this.allPeerNotifiers.put(key, allPeerNotifiers.get(key));
        }

        displayMsg(MSG_PEER_NOTIFIERS_SYNC_SUCCESS);
    }
    //=============================================================================================


    //All Peer Notifier Methods====================================================================
    public void sendResponse(String response) throws RemoteException{
        displayMsgToPlayer(response);
    }
    public void closeClient() throws RemoteException{
        System.exit(0);
    }
    //=============================================================================================


    private static void startPeerAsPrimaryServer(String[] args){
        int mazeGameBoardSize = 0;
        int numOfTreasures = 0;

        try{
            mazeGameBoardSize = Integer.parseInt(args[0]);
            numOfTreasures = Integer.parseInt(args[1]);
        } catch (Exception e) {
            displayMsg(MSG_INPUT_NOT_INTEGER);
            System.exit(1);
        }

    	ServerPeerNotifier stub = null;
		Registry registry = null;
		Peer primaryPeer = null;

    	try {
		    primaryPeer = new Peer();
		    stub = (ServerPeerNotifier) UnicastRemoteObject.exportObject(primaryPeer, 0);
		   
		    registry = LocateRegistry.getRegistry();
		    registry.bind(BIND_NAME_PRIMARY_SERVER, stub);
		    System.err.println(MSG_PRIMARY_SERVER_READY);
		} catch (Exception e) {
		    try{
				registry.unbind(BIND_NAME_PRIMARY_SERVER);
				registry.bind(BIND_NAME_PRIMARY_SERVER, stub);
			    System.err.println(MSG_PRIMARY_SERVER_READY);
			}catch(Exception ee){
				System.err.println(ERROR_PRIMARY_SERVER_BIND_EXCEPTION + ee.toString());
			    	ee.printStackTrace();
                    System.exit(1);
			}
		}


		primaryPeer.setUpNewGame(mazeGameBoardSize, numOfTreasures);
        displayMsg(primaryPeer.getBoardViewFromServer());
        primaryPeer.playGame();
        System.exit(0);
    }

    private static void startPeerAsSecondaryOrNormalPeer(String host){
        Peer newPeer = null;

    	try {
		    Registry registry = LocateRegistry.getRegistry(host);
		    ServerPeerNotifier stub = (ServerPeerNotifier) registry.lookup(BIND_NAME_PRIMARY_SERVER);

		    newPeer = new Peer(stub);

            try{
                if(stub.isSecondaryPeerNull()){
                    ServerSynchronizer secondaryServerPeer = (ServerSynchronizer) UnicastRemoteObject
                                                                                  .exportObject(newPeer, 0);
                   
                    String response = stub.joinSecondaryPeerIntoGame(secondaryServerPeer);
                    displayMsg(response);
                    if(response.contains("GAME_JOIN_FAILURE")){
                        System.exit(1);
                    }
                    newPeer.extractAndSavePlayerId(response);

                    Thread newThread = new Thread(newPeer);
                    newThread.setName(THREAD_NAME_PRIMARY_SERVER_MONITOR);
                    newThread.start();
                } else {
                    NormalPeerController normalPeer = (NormalPeerController) UnicastRemoteObject
                                                                             .exportObject(newPeer, 0);

                    String response = stub.joinNormalPeerIntoGame(normalPeer);
                    displayMsg(response);
                    if(response.contains("GAME_JOIN_FAILURE")){
                        System.exit(1);
                    }
                    newPeer.extractAndSavePlayerId(response); 
                }
            } catch (RemoteException e) {
                displayMsg(MSG_REMOTE_EXCEPTION + e.toString());
                System.exit(1);
            }

		   	
		    
		} catch (Exception e) {
		    System.err.println(ERROR_SECONDARY_OR_NORMAL_PEER_JOIN_EXCEPTION + e.toString());
		    e.printStackTrace();
            System.exit(1);
		}

        newPeer.playGame();
        System.exit(0);
    }


    //Common Methods===================================================================================

    public void extractAndSavePlayerId(String rsp){
        this.playerId = rsp.split(" ")[2];
    }
    //=================================================================================================

    //Primary Server Methods===========================================================================

    private void setUpNewGame(int mazeGameBoardSize, int numOfTreasures){
    
        this.normalPeers = new HashMap<String, NormalPeerController>();
        this.allPeerNotifiers = new HashMap<String, AllPeerNotifier>();
        this.primaryServerPeer = (ServerPeerNotifier)this;
        this.gameState = GameState.WAITING;
        this.numberOfPlayers = 1;
        this.playerId = "" + this.numberOfPlayers;

        this.mazeGameBoardSize = mazeGameBoardSize;
        this.numOfTreasures = numOfTreasures;
        this.playerPositions = new HashMap<String, String>();
        this.treasurePositions = new HashMap<String, Integer>();
        this.playerScores = new HashMap<String, Integer>();

        shuffleAndPlaceTreasures();
        displayMsg(MSG_GAME_ENVIRONMENT_SET_UP);

        Coordinate newPlayerPos = getCleanStartCoordinate(this.playerId);
        this.playerPositions.put(this.playerId, newPlayerPos.getCoordinateString());
        this.playerScores.put(this.playerId, 0);

        this.allPeerNotifiers.put(this.playerId, (AllPeerNotifier)this);

        displayMsg(MSG_NEW_PLAYER_JOINED + this.numberOfPlayers);
    }

    private void synchronizeAll() throws RemoteException{
        this.secondaryServerPeer.synchronizeNumberOfPlayers(this.numberOfPlayers);
        this.secondaryServerPeer.synchronizeNormalPeers(this.normalPeers);
        this.secondaryServerPeer.synchronizeGameState(this.gameState);
        this.secondaryServerPeer.synchronizeMazeGameBoardSize(this.mazeGameBoardSize);
        this.secondaryServerPeer.synchronizeNumOfTreasures(this.numOfTreasures);
        this.secondaryServerPeer.synchronizePlayerPositions(this.playerPositions);
        this.secondaryServerPeer.synchronizeTreasurePositions(this.treasurePositions);
        this.secondaryServerPeer.synchronizePlayerScores(this.playerScores);
        this.secondaryServerPeer.synchronizeAllPeerNotifiers(this.allPeerNotifiers);
    }

    private void monitorSecondaryServer(){
        boolean shouldStop = false;
        while(!shouldStop && this.gameState != GameState.OVER){
            try{
                Thread.sleep(2000);
            } catch (InterruptedException e){
                displayMsg(MSG_INTERRUPTED_EXCEPTION);
            }
            boolean isAlive = this.isSecondaryServerAlive();
            displayMsg("Secondary Server is " + (isAlive? "Alive" : "Dead"));
            shouldStop = isAlive == false;
        }
    }

    private boolean isSecondaryServerAlive(){
        boolean isAlive = false;
        try {
            isAlive = this.secondaryServerPeer.testIfSecondaryServerAlive();
        } catch (RemoteException e) {
            isAlive = false;
        }
        return isAlive;
    }


    public String getBoardViewFromServer(){
        String viewResultString = ""; 
        for(int y = mazeGameBoardSize - 1 ; y >= 0; y --){
            for(int x = 0; x < mazeGameBoardSize; x ++){

                Coordinate curC = new Coordinate(x, y);
                Integer treasureCount = treasurePositions.get(curC.getCoordinateString());

                if(treasureCount == null){
                    viewResultString += "[   ]";
                } else {
                    viewResultString += "[Tx"+ treasureCount +"]";
                }

                if(playerPositions.containsValue(curC.getCoordinateString())){
                    String playerId = getPlayerAtCoordinate(curC.getCoordinateString());
                    viewResultString +="P" + playerId;
                } else {
                    viewResultString += "  ";
                }
            }

            viewResultString += "\n";
        }

        return viewResultString;
    }

    private void shuffleAndPlaceTreasures(){

        for(int i = 0; i < this.numOfTreasures ; i ++){
            
            Coordinate newCoordinate = getRandomCoordinate();
            Integer curCount = treasurePositions.get(newCoordinate.getCoordinateString());
            if(curCount == null){
                treasurePositions.put(newCoordinate.getCoordinateString(), 1);
            } else {
                treasurePositions.put(newCoordinate.getCoordinateString(), (curCount + 1));
            }
        }
    }

    private void notifyGameOver(){

        this.gameState = GameState.OVER;
        //this will stop the monitoring thread

        //need to tell secondary server game is over
        try{
            this.secondaryServerPeer.synchronizeGameState(this.gameState);
        } catch (RemoteException e) {
            displayMsg(MSG_REMOTE_EXCEPTION + e.toString());
        }

        //kill normal peers first
        for(String playerId : this.normalPeers.keySet()){
            AllPeerNotifier notifier = this.allPeerNotifiers.get(playerId);
            try{
                notifier.sendResponse(getBoardViewFromServer() + 
                                      getScoreBoard() + 
                                      MSG_ALL_TREASURES_FOUND_USER);
                notifier.closeClient();
                
            } catch (RemoteException e) {
                displayMsg(String.format(MSG_PLAYER_QUIT_TO_SERVER, playerId));
            }
            
        }

        try{
            ((AllPeerNotifier)this.secondaryServerPeer).sendResponse(getBoardViewFromServer() + 
                                                                     getScoreBoard() + 
                                                                     MSG_ALL_TREASURES_FOUND_USER);
            ((AllPeerNotifier)this.secondaryServerPeer).closeClient();
        } catch (RemoteException e){
            displayMsg(String.format(MSG_PLAYER_QUIT_TO_SERVER, playerId));
        }
        
        displayMsgToPlayer(getBoardViewFromServer() + 
                           getScoreBoard() + 
                           MSG_ALL_TREASURES_FOUND_SERVER);
    }

    private void startGameAfterDelay(){
        try{
            Thread.sleep(CONST_JOING_PERIOD);
        } catch (InterruptedException e){
            displayMsg(MSG_INTERRUPTED_EXCEPTION);
        }
        
        for(String playerId : this.allPeerNotifiers.keySet()){
            AllPeerNotifier notifier = this.allPeerNotifiers.get(playerId);
            try{
                notifier.sendResponse(String.format(MSG_GAME_NOW_START, CONST_JOING_PERIOD/1000));
                
                this.gameState = GameState.STARTED;
            } catch (RemoteException e) {
                displayMsg(MSG_REMOTE_EXCEPTION + e.toString());
            }
            
        }
    }

    private boolean areAllTreasuresFound(){
        return treasurePositions.keySet().size() == 0;
    }

    private String getScoreBoard(){
        String scoreBoardAsString = LABEL_SCORE_BOARD_START;
        for(String key : playerScores.keySet()){
            scoreBoardAsString += String.format(LABEL_PLAYER_SCORE, key, playerScores.get(key));
        }
        scoreBoardAsString += LABEL_SCORE_BOARD_END;

        return scoreBoardAsString;
    }

    private String processCommand(String cmd, String playerId) throws RemoteException{
        String resp = MSG_COMMAND_RESPONSE_CODE_SUCCESS;
        Coordinate curCoordinate = null;

        

        if(cmd.contentEquals(PLAYER_COMMAND_MOVE_QUIT)){

            //second server chose to quit
            if(!playerId.contentEquals(this.secondaryServerPeer.getSencondaryServerPeerPlayerId()) &&
               !playerId.contentEquals(this.playerId)){
                this.normalPeers.remove(playerId);
            }
            this.allPeerNotifiers.remove(playerId);
            this.playerPositions.remove(playerId);
            this.playerScores.remove(playerId);
            displayMsg(String.format(MSG_PLAYER_QUIT_TO_SERVER, playerId));
            return resp + MSG_PLAYER_QUIT_WHILE_GAME_ON;
        }

        try{
            curCoordinate = new Coordinate(this.playerPositions.get(playerId));
        } catch(Exception e) {
            resp = MSG_COMMAND_RESPONSE_CODE_ERROR_NO_PLAYER_POS;
            return resp;
        }

        Coordinate nextCoordinate = getNextPos(curCoordinate, cmd);
        if(!isCoordinateWithinBoard(nextCoordinate)){
            resp = MSG_COMMAND_RESPONSE_CODE_ERROR_OUT_OF_BOARD;
            return resp;
        }

        if(isCoordinateOccupiedByOthers(nextCoordinate, playerId)){
            resp = MSG_COMMAND_RESPONSE_CODE_ERROR_NEXT_POS_OCCUPIED;
            return resp;
        }

        this.playerPositions.put(playerId, nextCoordinate.getCoordinateString());

        return resp + getUpdatedScoreStatus(nextCoordinate, playerId);
    }

    private String getUpdatedScoreStatus(Coordinate newCoordinate, String playerId){
        String scoreStatus = null;

        Integer numOfTreasuresHere = treasurePositions.get(newCoordinate.getCoordinateString());
        if(numOfTreasuresHere != null){
            int curScore = playerScores.get(playerId);
            playerScores.put(playerId, curScore + numOfTreasuresHere);
            scoreStatus = String.format(MSG_COMMAND_RESPONSE_CODE_SUCCESS_NEW_TREASURE_FOUND
                                        , playerId
                                        , numOfTreasures);
            treasurePositions.remove(newCoordinate.getCoordinateString());
            return scoreStatus;
        }
        scoreStatus = MSG_COMMAND_RESPONSE_CODE_SUCCESS_NO_TREASURE_FOUND;
        return scoreStatus;
    }

    private boolean isCoordinateOccupiedByOthers(Coordinate coordinate, String myId){
        String playerId = getPlayerAtCoordinate(coordinate.getCoordinateString());
        return playerId != null && !playerId.contentEquals(myId);
    }

    private boolean isCoordinateWithinBoard(Coordinate coordinate){
        return coordinate.x < this.mazeGameBoardSize &&
               coordinate.x >= 0 &&
               coordinate.y < this.mazeGameBoardSize &&
               coordinate.y >= 0;
    }

    private Coordinate getNextPos(Coordinate curCoordinate, String cmd){
        Coordinate newCoordinate = curCoordinate;
        switch(cmd){
            case PLAYER_COMMAND_MOVE_UP:
                newCoordinate.y ++;
                break;
            case PLAYER_COMMAND_MOVE_LEFT:
                newCoordinate.x --;
                break;
            case PLAYER_COMMAND_MOVE_DOWN:
                newCoordinate.y --;
                break;
            case PLAYER_COMMAND_MOVE_RIGHT:
                newCoordinate.x ++;
                break;
            case PLAYER_COMMAND_MOVE_STAY:
                break;
            default :
                break;
        }

        return newCoordinate;
    }

    private String getPlayerAtCoordinate(String coorStr){
        for(String key : playerPositions.keySet()){
            if(playerPositions.get(key).contentEquals(coorStr)){
                return key;
            }
        }

        return null;
    }


    private int getRandomInt(int min, int max){
        Random rand = new Random();

        int randomNum = rand.nextInt((max - min) + 1) + min;
        
        return randomNum;
    } 

    private Coordinate getRandomCoordinate(){
        int x = getRandomInt(0, this.mazeGameBoardSize -1);
        int y = getRandomInt(0, this.mazeGameBoardSize -1);
        Coordinate newCoordinate = new Coordinate(x, y);
        return newCoordinate;
    }

    private Coordinate getCleanStartCoordinate(String playerId){

        boolean isCleanStartPos = false;
        Coordinate newCoordinate = null;

        while(!isCleanStartPos){
            newCoordinate = getRandomCoordinate();
            if(treasurePositions.get(newCoordinate.getCoordinateString()) == null){
                if(!isCoordinateOccupiedByOthers(newCoordinate, playerId)){
                    isCleanStartPos = true;
                }       
            }
        }

        return newCoordinate;
    }

    //=================================================================================================




    //Secondary and Noraml Peer Methods================================================================

    private void monitorPrimaryServer(){
        boolean shouldStop = false;
        while(!shouldStop && this.gameState != GameState.OVER){
            try{
                Thread.sleep(2000);
            } catch (InterruptedException e){
                displayMsg(MSG_INTERRUPTED_EXCEPTION);
            }
            boolean isAlive = this.isPrimaryServerAlive();
            displayMsg("Primary Server is " + (isAlive? "Alive" : "Dead"));
            shouldStop = isAlive == false;
        }
    }

    private boolean isPrimaryServerAlive(){
        boolean isAlive = false;

        try {
            isAlive = this.primaryServerPeer.testIfPrimaryServerAlive();
        } catch (RemoteException e) {
            isAlive = false;
        }
        return isAlive;
    }

    private void makeSecondaryNewPrimary(){
        displayMsg(MSG_MAKING_SECONDARY_NEW_PRIMARY_SERVER);

        this.primaryServerPeer = this;
        this.secondaryServerPeer = null;
        ServerPeerNotifier stub = (ServerPeerNotifier)this; 

        for(String playerId : this.normalPeers.keySet()){
            NormalPeerController peerController = this.normalPeers.get(playerId);

            try{
                peerController.updatePrimaryServer(stub);
                displayMsg(MSG_UPDATED_PRIMARY_SERVER_FOR_PLAYER + playerId);
            } catch (RemoteException e) {
                displayMsg(MSG_FAILED_TO_UPDATE_PRIMARY_SERVER_FOR_PLAYER + playerId);
            }
            
        }

        displayMsg(MSG_SECONDARY_TO_PRIMARY_SERVER_SUCCESS);
    }


    private void selectNewSecondaryServer(){
        displayMsg(MSG_TO_SELECT_NEW_SECONDARY_SERVER);
        ServerSynchronizer stub = null;

        for(String playerId : this.normalPeers.keySet()){
            NormalPeerController peerController = this.normalPeers.get(playerId);

            try{
                peerController.makeSelfSecondaryServer();
                stub = (ServerSynchronizer) peerController;
                this.secondaryServerPeer = stub;
                this.normalPeers.remove(playerId);
                this.synchronizeAll();
                displayMsg(String.format(MSG_NEW_SECONDARY_SERVER_SELECTED, playerId));

                Thread newThread = new Thread(this);
                newThread.setName(THREAD_NAME_SECONDARY_SERVER_MONITOR);
                newThread.start();
                break;
            } catch (RemoteException e) {
                displayMsg(String.format(MSG_TRY_MAKE_NEXT_PEER_NEW_SECONDARY_SERVER, playerId));
            }
        }
    }
    

    public void playGame(){
        boolean ifPlayerChooseToQuit = false;

        while(!ifPlayerChooseToQuit){

            
            String cmd = getPlayerCommand();
            if(cmd.contentEquals(ERROR_IO_EXCEPTION)){
                displayMsg(MSG_READING_IO_EXCEPTION);
                continue;
            }

            displayMsg(MSG_ENTERED_USER_COMMAND + cmd);

            if(!isCommandValid(cmd)){
                displayMsg(MSG_INVALID_COMMAND);
                continue;
            }

            displayMsg(MSG_SENDING_COMMAND_TO_SERVER);

            try{
                String rsp = this.primaryServerPeer.sendGameCommandAndGetResponse(cmd, playerId);
                displayMsgToPlayer(MSG_RESPONSE_FROM_SERVER_MSG + rsp);

                if(cmd.contentEquals(PLAYER_COMMAND_MOVE_QUIT)){
                    displayMsg(MSG_EXITING_GAME);
                    ifPlayerChooseToQuit = true;
                }
            } catch (RemoteException e){
                //Failed to get response from primary server, 
                //primary server may have crashed
                //let the player wait for a while to retry
                //when primary server is being restored
                displayMsg(MSG_NO_RESP_FROM_SERVER_TRY_AGAIN);
                continue;
            }
            
        }

        displayMsg(MSG_DISCONNECTED_FROM_SERVER);

    }


    private String getPlayerCommand(){
        displayMsg(MSG_REQUEST_USER_COMMAND);

        String playerCommand = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        try{
            playerCommand = br.readLine();
        } catch (IOException e){
            playerCommand = ERROR_IO_EXCEPTION;
        } 
        
        return playerCommand;
    }

    private boolean isCommandValid(String cmd){

        boolean isValid = false;
        cmd = cmd.toLowerCase();
        if(cmd.contentEquals(PLAYER_COMMAND_MOVE_UP)   ||
           cmd.contentEquals(PLAYER_COMMAND_MOVE_LEFT) ||
           cmd.contentEquals(PLAYER_COMMAND_MOVE_DOWN) ||
           cmd.contentEquals(PLAYER_COMMAND_MOVE_RIGHT)||
           cmd.contentEquals(PLAYER_COMMAND_MOVE_QUIT) ||
           cmd.contentEquals(PLAYER_COMMAND_MOVE_STAY)){
           
           isValid = true;
        }

        return isValid;
    }

    //=================================================================================================


    public static void main(String args[]) {
		       
        if(args.length != 2 && args.length != 1){
            displayMsg(MSG_SERVER_PARAM_FORMAT);
            System.exit(1);
        }

		if(args.length == 2){
			startPeerAsPrimaryServer(args);
		} else {
			startPeerAsSecondaryOrNormalPeer(args[0]);
		}  	 
	}


    public void displayMsgToPlayer(String msg){
        String myPlayerIdString = "Player " + this.playerId;
        msg = msg.replace(myPlayerIdString, myPlayerIdString + " (You) ");
    	System.out.println(msg);
    }

    public static void displayMsg(String msg){
        System.out.println(msg);
    }

}
