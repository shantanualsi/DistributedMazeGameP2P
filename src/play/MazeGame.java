package play;

import game.GameImplementation;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MazeGame {
	
	
	public static void main(String[] args){
		
		switch(args.length){
		
		
		//Acting as Client now
		case 1:
				break;
		//Acting as Server now
		case 2:
			
			if(args.length != 2){
				
				System.out.println("Usage java MazeServer <Size of grid> <Number of treasures>");
				System.exit(-1);
			}
			
			int gridsize = Integer.parseInt(args[0]);
			int nTreasures = Integer.parseInt(args[1]);
			
			if(nTreasures == 0){
				
				System.out.println("Number of treasures cannot be zero");
				System.exit(-1);
			}
			
			if(gridsize < 2){
				
				System.out.println("GridSize should be atleast 2");
				System.exit(-1);
				
			}
			
			Registry registry  = null;								
			
			try {
				
				GameImplementation gs = new GameImplementation(gridsize,nTreasures);
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
			
				break;
		default:
				System.out.println("Usage");
				System.out.println("play.ClientServer <gridsize> <numberoftreasures>");
				System.out.println("play.ClientServer IPAddress");
				System.exit(-1);
			
		}
		
		
		
	}
	
}
