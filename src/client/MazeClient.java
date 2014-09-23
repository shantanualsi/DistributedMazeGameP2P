package client;

import game.Constants;
import game.Direction;
import game.GameMethod;
import game.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;

public class MazeClient {

	int clientID;
	int gameBoard[][];
	int boardSize;
	String host;
	
    public MazeClient(String host) {
    	
    	this.host = host;
    }

    public void startClient() {
		
		GameMethod gs = null;
		int msgType;
		HashMap<String,Object> res = null;
		
		//[TODO] Need a score object
		
		MazeClient mc = this;
		
		try {
		    Registry registry = LocateRegistry.getRegistry(mc.host);
		    gs = (GameMethod) registry.lookup("GameImplementation");
		    
		    
		} catch (Exception e) {
		    System.err.println("Client exception: " + e.toString());
		    e.printStackTrace();
		}
		
		
		try{			
			
			res = gs.ConnectToGame();			
			
		}catch(RemoteException re){
			
			System.out.println("Seems like server is not running or is throwing some exception.");			
			re.printStackTrace();
			System.exit(-1);
		}
		
		msgType = Integer.parseInt(res.get(Constants.MessageType).toString());
		
		switch(msgType){
		
			case MessageType.ConnectSuccess:
				mc.clientID = Integer.parseInt(res.get(Constants.MessageObject).toString());
				mc.boardSize = Integer.parseInt(res.get(Constants.BoardSize).toString());
				System.out.println("Connected with id "+ mc.clientID);
				break;
			case MessageType.ConnectError:
				String msg =res.get(Constants.MessageObject).toString(); 
				System.out.println(msg);
				System.exit(-1);
				break;
			default:
				System.out.println("Unknown message type!!!!");
				System.exit(-1);										
		
		}	    	    
	    	    			
    	
    	try{
    		
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	    	String text;

    		while(true){
    			
    			text = br.readLine();
    			
    			if(text.length() == 0){
    				continue;
    			}
    			
    			switch(text.toUpperCase().charAt(0)){
    				
    			case 'W': res = gs.move(mc.clientID,Direction.UP);
    				break;
    			case 'A': res = gs.move(mc.clientID,Direction.LEFT);
					break;
    			case 'S': res = gs.move(mc.clientID,Direction.DOWN);
					break;
    			case 'D': res = gs.move(mc.clientID,Direction.RIGHT);
    				break;
    			case 'E': res = gs.move(mc.clientID,Direction.STAY);
					break;
				default:
					System.out.println("Invalid Move!!!");
    			
    			}
    			
    			
    			msgType = Integer.parseInt(res.get(Constants.MessageType).toString());
        		String message = null;
        		
        		switch(msgType){
        			
        			case MessageType.Error:
						// Get the error message and print it
						message = res.get(Constants.MessageObject).toString();
						System.out.println(message);
						break;
        			case MessageType.MazeObject:    					
        				mc.gameBoard = (int[][]) res.get(Constants.MessageObject);
	    				mc.printGameBoard();
						break;
        			case MessageType.GameOver:
        				// [TODO] Take object of score and print that
	    				message = res.get(Constants.MessageObject).toString();
						System.out.println("Game Over. Thank you for playing...");									
						break;		
        			default :
        				System.out.println("Unknown response from the server");
        			
        		
        		}
        		
        		//[TODO] If game gets over client should move out of this while loop
    			
    			
    		}
    			
    				    		
    	}catch(RemoteException re){
			
			
		} catch (IOException e) {
			System.out.println("Cannot read from standard input");
			e.printStackTrace();
		}
    	
	    
    
    	
	
    }
    
    
    private void printGameBoard(){
    	
    	//[TODO] Add code to print the score
    	//[TODO] Screen should get cleared
    	for (int i = 0; i < this.boardSize; i++) {
			for (int j = 0; j < this.boardSize; j++) {
				System.out.print(this.gameBoard[i][j]+"\t");
			}
			System.out.println();
		}
		
		System.out.println();
    	
    }
}
