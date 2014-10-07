package play;

import client.MazeClient;
import server.MazeServer;

public class MazeGame {

	
	public static void main(String[] args){
				
		String serverip = "localhost";
		int port        = 1099;
		
		switch(args.length){		
		
		//Acting as Client now
		case 2: 				
				serverip = args[0];
				port     = Integer.parseInt(args[1]);
				break;
		//Acting as Server now
		case 3:
			int gridsize = Integer.parseInt(args[1]);
			int nTreasures = Integer.parseInt(args[2]);
			
			if(nTreasures == 0){
				
				System.out.println("Number of treasures cannot be zero");
				System.exit(-1);
			}
			
			if(gridsize < 2){
				
				System.out.println("GridSize should be atleast 2");
				System.exit(-1);
				
			}
			
			startMazeServer(gridsize,nTreasures);															
			break;
		default:
				System.out.println("Usage");
				System.out.println("play.MazeGame server <gridsize> <numberoftreasures>");
				System.out.println("play.MazeGame <IPAddress> <port>");
				System.exit(-1);
			
		}
				
		startGameClient(serverip,port);
		
		
	}
	
	
	private static void startMazeServer(int gridsize,int nTreasures){
		
		MazeServer ms = new MazeServer(gridsize,nTreasures);
		ms.start();
		try {
			ms.join();
		} catch (InterruptedException e) {
			
			System.out.println("Exception in waiting for the thread");
			
		}
		
		
	}
	
	private static void startGameClient(String serverip,int port){
		
		
		MazeClient mc = new MazeClient(serverip,port);
		mc.start();
		
	}
	
}
