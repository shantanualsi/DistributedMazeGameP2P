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
	
	
    public MazeClient(String host,int port) {    	
    	this.host = host;
    	this.port = port;
    	try {
			this.myIP = InetAddress.getLocalHost().getHostAddress();			
		} catch (UnknownHostException e) {
			System.out.println("Unknown Host");
		}

    	this.pList = new HashMap<Integer,Player>();
    	    	
    }
    
	
	public void run() {
		
		GameMethod gs = null;						
		
		//Start the client registry
		this.startClientRegistry();
		
		try{
			//Get the server registry Object
			gs = this.getServerRegistryObject(this.host,1099);
		}catch(Exception e){
			
			System.out.println("Error connecting to server");
			
		}
		
		//Connect to the server
		this.joinGame(gs);				
		
		
		//Take user input
		this.getUserInput(gs);
				
    }
	
	
	private void getUserInput(GameMethod gs){
		
		HashMap<String,Object> res = null;
		String text = "";
    	Boolean act = true;
    	while(true){
    			    			
    		try{
        		
    			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    			
    			text = br.readLine();
    			
    			if(text.length() == 0){
    				continue;
    			}
    			
    			
    			res = this.makeAMove(text, gs);
    			//If result is not null
    			if(!(res == null)){
    				
    				act = takeActionOnResult(res,gs);
    				
    				if(!act){
    					break;
    				}
    			}
    			
    				    		
	    	}catch(RemoteException re){
	    		
				System.out.println("Server Down");							
				while(true){				
					try {
						System.out.println("Connecting to BackUp Server at "+(char)(this.backUpServerID+64));
						Player p = this.pList.get(this.backUpServerID);
						System.out.println("IP is "+p.getIP()+" port is "+p.getPort());
						gs = this.getServerRegistryObject(p.getIP(), p.getPort());
						res = this.makeAMove(text, gs);
						break;
					}catch (RemoteException exc) {

						System.out.println("Player "+(char)(this.backUpServerID+64)+" is down");
						this.backUpServerID++;						
						
					}										
					catch (Exception e) {

						System.out.println("Player "+(char)(this.backUpServerID+64)+" threw an exception");
						e.printStackTrace();
						break;
					}    			

					
				}
				
				act = takeActionOnResult(res,gs);
				
				if(!act){
					break;
				}
				
			} catch (IOException e) {
				System.out.println("Cannot read from standard input");
				e.printStackTrace();
			}
    	}
	    	
		
	}
    
    private GameMethod getServerRegistryObject(String host,int port) throws Exception{
    	
    	GameMethod gs = null;    
    	
    							
		//Locate server object
	    this.serverRegistry = LocateRegistry.getRegistry(host,port);
	    gs = (GameMethod) serverRegistry.lookup("GameImplementation");
	    		   	
    	return gs;
    }
    
    
    @SuppressWarnings("unchecked")
	private void joinGame(GameMethod gs){
    	    	
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
				System.out.println("You are Player "+ (char)(mc.clientID+64));

				
				//Register My GameImplementation 
				this.registerClientGI(gs);
				
				//Get the time left to start the game and then sleep for that amount of time
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
						res = gs.GetInitialGameState(this.clientID);
					} catch (RemoteException e) {
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
    
    
    private void registerClientGI(GameMethod gs){
    	
    	GameImplementation mygs = null;
    	
    	try {
			mygs = new GameImplementation(this.boardSize,this.nTreasures,this.clientID,this.backUpServerID,this.pList);						
			this.registry.bind("GameImplementation",mygs);
    	}catch (RemoteException e1) {
    		
			e1.printStackTrace();
		} catch (AlreadyBoundException e) {

			e.printStackTrace();
		}
		
    	
    }
       
    
    @SuppressWarnings("unchecked")
	private Boolean takeActionOnResult(HashMap<String,Object> res,GameMethod gs){
    	
    	int msgType;
    	MazeClient mc = this;
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
				mc.nTreasures = Integer.parseInt(res.get(Constants.Treasures).toString());												
				mc.printGameBoard();
				mc.backUpServerID = Integer.parseInt(res.get(Constants.BackUpServerID).toString());
				break;
			case MessageType.GameOver:        				
				message = res.get(Constants.MessageObject).toString();
				mc.pList = (HashMap<Integer,Player>)res.get(Constants.Players);
				System.out.println("Game Over. Thank you for playing...");
				System.out.println("----------- Final Scores ---------------");
				this.printFinalScores();
				return false;
			default :
				System.out.println("Unknown response from the server");
			
		
		}
		
		return true;
	
    	
    }
    
    
    private void printFinalScores(){
    	
    	char win='0';
    	int max = 0;
    	int score = 0;
    	for(Entry<Integer, Player> p: this.pList.entrySet()){
    		score = p.getValue().getPlayerScore();    		
    		System.out.println("Player "+(char)(Integer.parseInt(p.getKey().toString())+64)+" : "+score);
    		if(score > max){
    			max = score;
    			win = (char)(Integer.parseInt(p.getKey().toString())+64);
    		}
    		
    	}
    	
    	System.out.println("Player "+win+" wins with score "+max);
    	
    	
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
    
    
    private synchronized void printGameBoard(){
    	
    	for (int i = 0; i < this.boardSize; i++) {
			for (int j = 0; j < this.boardSize; j++) {
				if(this.gameBoard[i][j] <= 0){
					
					System.out.print(Math.abs(this.gameBoard[i][j])+"\t");
				}else{
					System.out.print((char)(this.gameBoard[i][j]+64)+"\t");
					
				}
				
				
				
			}
			System.out.println();
		}
		
		System.out.println();
		printScores();
    	
    } 
    
    private void printScores(){
    	
    	for(Entry<Integer, Player> p: this.pList.entrySet()){
    		
    		System.out.println("Player "+(char)(Integer.parseInt(p.getKey().toString())+64)+" : "+p.getValue().getPlayerScore());
    		
    	}
    	
    }
    
}
