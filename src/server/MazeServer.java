package server;

import game.GameImplementation;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MazeServer extends Thread{
	
	
	private int gridsize;
	private int nTreasures;	
	private int serverPort;

	public MazeServer(int gridsize,int nTreasures){
		
		this.gridsize = gridsize;
		this.nTreasures = nTreasures;
		this.serverPort = 1099;
		
	}
	
	public void run(){
		
		
		Registry registry  = null;													
		
		while(true){
			try {
				registry = LocateRegistry.createRegistry(this.serverPort);
				break;
			} catch (RemoteException e) {
			
				System.out.println("Server cannot be started on port "+ this.serverPort);
				this.serverPort++;
				
			}				
		}
		
		try {
			
			GameImplementation gs = new GameImplementation(this.gridsize,this.nTreasures);			
			registry.bind("GameImplementation", gs);
			System.out.println("Server Started on "+InetAddress.getLocalHost().getHostAddress().toString()+":"+this.serverPort);			
			
		} catch (RemoteException re) {
			
			System.err.println("Server Cannot be Started");
			re.printStackTrace();
		} catch(AlreadyBoundException abe){
			
			System.err.println("Already Bounded");
			abe.printStackTrace();
		} catch (UnknownHostException uhe) {
			System.err.println("Unknown host cannot find the address");
			uhe.printStackTrace();
			
		}	    		
		 
	}
			
			
	
}
