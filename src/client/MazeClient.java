package client;

import game.Constants;
import game.Direction;
import game.GameImplementation;
import game.GameMethod;
import game.MessageType;
import game.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map.Entry;

public class MazeClient extends Thread{

	int clientID;
	int gameBoard[][];
	int boardSize;
	int nTreasures;
	int backUpServerID;
	HashMap <Integer,Player> pList;
	String host;
	String myIP;
	int port;
	Registry registry;
	Registry serverRegistry;
	
    public MazeClient(String host) {
    	
    	this.host = host;
    	this.port = 1099;
    	try {
			this.myIP = InetAddress.getLocalHost().getHostAddress();			
		} catch (UnknownHostException e) {
			System.out.println("Unknown Host");
		}
    	    	
    }
    
	@SuppressWarnings("unchecked")
	public void run() {
		
		GameMethod gs = null;		
		int msgType;
		HashMap<String,Object> res = null;
				
		
		MazeClient mc = this;
		
		//Start the client registry
		startClientRegistry();
		
		//Get the server registry Object
		gs = getServerRegistryObject();
		
		//Connect to the server
		connectToServer(gs);
	    	    			
    	
    	try{
    		
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	    	String text;

    		while(true){
    			
    			text = br.readLine();
    			
    			if(text.length() == 0){
    				continue;
    			}
    			
    			
    			res = this.makeAMove(text, gs);
    			
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
        				mc.pList = (HashMap<Integer,Player>)res.get(Constants.Players);
        				int backServerID = Integer.parseInt(res.get(Constants.BackUpServerID).toString());
        				
        				//BackUpServer Changed
        				if(backServerID != mc.backUpServerID){
        					
        					//[TODO] Handle Backup server change here
        					
        				}
	    				mc.printGameBoard();
						break;
        			case MessageType.GameOver:        				
	    				message = res.get(Constants.MessageObject).toString();
						System.out.println("Game Over. Thank you for playing...");	
						//If game gets over client should move out of this while loop
						break;
        			default :
        				System.out.println("Unknown response from the server");
        			
        		
        		}
        		
        		
    			
    			
    		}
    			
    				    		
    	}catch(RemoteException re){
			System.out.println("Server Down");
												
			
		} catch (IOException e) {
			System.out.println("Cannot read from standard input");
			e.printStackTrace();
		}    		    
    	
	
    }
    
    private GameMethod getServerRegistryObject(){
    	
    	GameMethod gs = null;
    	MazeClient mc = this;
    	
    	try {						
			//Locate server object
		    this.serverRegistry = LocateRegistry.getRegistry(mc.host);
		    gs = (GameMethod) serverRegistry.lookup("GameImplementation");
		    
		    
		} catch (Exception e) {
		    System.err.println("Client exception: " + e.toString());
		    e.printStackTrace();
		}
    	
    	return gs;
    }
    
    
    @SuppressWarnings("unchecked")
	private void connectToServer(GameMethod gs){
    	    	
		int msgType;
		HashMap<String,Object> res = null;
		MazeClient mc = this; 

		try{			
			
			res = gs.ConnectToGame(this.myIP,this.port);			
			
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
				mc.backUpServerID = Integer.parseInt(res.get(Constants.BackUpServerID).toString());
				System.out.println("Connected with id "+ mc.clientID);
				
				//If I am the backup server 
				//Register my object for RMI
				if(mc.clientID == mc.backUpServerID){
					
					startBackUp(gs);
					
				}
				long waitTime = Long.parseLong(res.get(Constants.TimeLeft).toString());
				System.out.println("Please wait for "+(int)waitTime/1000+" seconds for game to begin.");
				try {
					
					Thread.sleep(waitTime);
					
				} catch (InterruptedException e1) {
					
					System.out.println("Cannot sleep the thread");
				}
				
				//Get the Initial Game state 
				//Loop till you get the Connect Success
				while(true){
					
					try {
						res = gs.GetInitialGameState();
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					msgType = Integer.parseInt(res.get(Constants.MessageType).toString());
					
					if(msgType == MessageType.ConnectSuccess){
						
						
						System.out.println("Game Started");
						mc.gameBoard = (int[][]) res.get(Constants.MessageObject);						
        				mc.pList = (HashMap<Integer,Player>)res.get(Constants.Players);
						this.printGameBoard();
						break;
						
					}
					
					
				}
				
				
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
    	
    }
    
    private void startBackUp(GameMethod gs){
    	
    	GameImplementation mygs = null;
    	
    	try {
			mygs = new GameImplementation(this.boardSize,this.nTreasures,this.clientID);						
			this.registry.bind("GameImplementation",mygs);
			gs.startBackUpService();
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (AlreadyBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
    }
    
    private void startClientRegistry(){
    	
		//Start client registry		
		while(true){
			
			try {
				this.registry = LocateRegistry.createRegistry(this.port);
				System.out.println("registry started on "+this.port);
				break;
			} catch (RemoteException e) {
				System.out.println("Cannot start registry on port "+this.port);
				this.port++;
				
			}
			
		}
			
    	
    }
    
    private HashMap<String,Object> makeAMove(String text,GameMethod gs) throws RemoteException{
    	
    	MazeClient mc = this;
    	HashMap<String,Object> res = null;
    	
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
    	
    	return res;
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
		printScores();
    	
    } 
    
    private void printScores(){
    	
    	for(Entry<Integer, Player> p: this.pList.entrySet()){
    		
    		System.out.println("Player "+p.getKey()+" : "+p.getValue().getPlayerScore());
    		
    	}
    	
    }
    
}
