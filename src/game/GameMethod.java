package game;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

public interface GameMethod extends Remote{
	
	public HashMap<String,Object> move(int id,int dir) throws RemoteException;
	public HashMap<String,Object> ConnectToGame(String clientIP,int clientPort) throws RemoteException;
	public HashMap<String,Object> GetInitialGameState(int id) throws RemoteException;	
	public void receiveBackUp(int[][] gameBoard,HashMap<Integer,Player> pList,int numberOfTreasures,int backUpServerID) throws RemoteException;	
	public void handleHeartBeat() throws RemoteException;
}
