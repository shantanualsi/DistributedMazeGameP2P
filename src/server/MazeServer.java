package server;

import game.GameImplementation;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MazeServer extends Thread{
	
	
	private int gridsize;
	private int nTreasures;

	public MazeServer(int gridsize,int nTreasures){
		
		this.gridsize = gridsize;
		this.nTreasures = nTreasures;
			
		
	}
	
	public void run(){
		
		
		Registry registry  = null;								
		
		try {
			
			GameImplementation gs = new GameImplementation(this.gridsize,this.nTreasures);
			registry = LocateRegistry.getRegistry();
			registry.bind("GameImplementation", gs);
			System.out.println("Server Started");			
			
		} catch (RemoteException re) {
			
			System.err.println("Server Cannot be Started");
			re.printStackTrace();
		} catch(AlreadyBoundException abe){
			
			System.err.println("Already Bounded");
			abe.printStackTrace();
		}	    		
		 
	}
			
			
	
}
