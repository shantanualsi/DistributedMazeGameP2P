package game;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Random;

import server.WaitConnect;

//import server.WaitConnect;

public class GameImplementation extends UnicastRemoteObject implements GameMethod{

	private int boardSize;
	private int numberOfTreasures;
	private int[][] gameBoard;	
	private HashMap <Integer,Player> pList;
	private int maxPlayers;
	int lastId = 0;
	String backUpServerIP;
	GameMethod backgs;
	
	
	GameInfo gameInfo = GameInfo.NotStarted;
	
	private static final long serialVersionUID = -4933868291603601249L;
	
	String[] msg = new String[2];
	
	
	public GameImplementation(int bSize) throws RemoteException {
		super();
 
		this.boardSize = bSize;		
		this.gameBoard = new int[boardSize][boardSize];	
		this.pList = new HashMap<Integer,Player>();
		this.maxPlayers = bSize*bSize - 1;
		this.backUpServerIP = "none";
					
	}
	
	
	public GameImplementation(int bSize,int nTreasures) throws RemoteException {
		super();
 
		this.boardSize = bSize;
		this.numberOfTreasures = nTreasures;
		this.gameBoard = new int[boardSize][boardSize];	
		this.pList = new HashMap<Integer,Player>();
		this.maxPlayers = bSize*bSize - 1;
		this.backUpServerIP = "none";
		
		//Initialize GameBoard with all zeros
		for(int i=0;i<boardSize;i++){
			for(int j=0;j<boardSize;j++){
				this.gameBoard[i][j] = 0;
			}
		}
	}
	
	

	//Place treasures on the board while checking it should not
	//coincide with the player position
	public void setRandomTreasures(){
		
		HashMap<Integer,HashMap<String,Integer>> hm = new HashMap<Integer,HashMap<String,Integer>>();
		HashMap<String,Integer> pos;
		int count = 0;
		
		//Put all the empty places inside the hashmap
		for(int i=0;i<this.boardSize;i++){
			
			for(int j=0;j<this.boardSize;j++){
				
				if(!this.isOccupiedByPlayer(i,j)){
					
					pos = new HashMap<String,Integer>();
					pos.put("X",i);
					pos.put("Y",j);
					hm.put(count, pos);
					count++;
				}
				
			}
			
		}
		
		Random rm = new Random();
		int hmpos,tresx,tresy;
		
		for(int i=0;i<this.numberOfTreasures;i++){
						
			hmpos = rm.nextInt(count);
			tresx = hm.get(hmpos).get("X");
			tresy = hm.get(hmpos).get("Y");
			this.gameBoard[tresx][tresy] -= 1;
			
			
		}
		
		this.printGameBoard();
		
	}
	
	//Prints the gameBoard on server
	private void printGameBoard(){
		
		for (int i = 0; i < this.boardSize; i++) {
			for (int j = 0; j < this.boardSize; j++) {
				System.out.print(this.gameBoard[i][j]+"\t");
			}
			System.out.println();
		}
		
		System.out.println();
	}
	
	//Checks if position x,y is occupied by the player or not
	private Boolean isOccupiedByPlayer(int x,int y){
		
		if(this.gameBoard[x][y] > 0){
			return true;
		}else{
			
			return false;
		}
		
	}
	
	//Sets the player position to random, unoccupied position
	//I feel this can be done using some other algo
	//It will go on looping until it finds an empty position
	//Since the Position is random, theoretically it might loop forever
	//-Naman
	private void setRandomPlayerPosition(Player p){
		
		int x = 0;
		int y = 0;
		
		Random rm;
				
		while(true){
			
			rm = new Random();
			x = rm.nextInt(this.boardSize);
			y = rm.nextInt(this.boardSize);
			
			if(!this.isOccupiedByPlayer(x, y)){
				
				p.setxPos(x);
				p.setyPos(y);				
				this.gameBoard[x][y] = p.getId();				
				break;
				
			}
			
		}
		
		
	}
	
	
	//Starts the game by setting the gameinfo variable to started
	public void startGame(){
		
		this.gameInfo = GameInfo.Started;
		//[TODO] All connected clients should get the info that game has started 
		
	}
	
	//Connects a client to the game
	public HashMap<String,Object> ConnectToGame(){
		
		 
		switch(this.gameInfo){
		
			case NotStarted:
					this.gameInfo = GameInfo.Waiting;
					WaitConnect wc = new WaitConnect(this);
					wc.start();					
					break;
			case Started:
					String msg = "Game has already started !!! Cannot join now.";
					return createMessage(MessageType.ConnectError,msg);
			default:
				break;													
			
		}
		
		this.lastId++;
		
		if(this.lastId<=this.maxPlayers){
			
			Player p = new Player(this.lastId);
			this.pList.put(this.lastId, p);
			setRandomPlayerPosition(p);
			HashMap <String,Object> hm = createMessage(MessageType.ConnectSuccess,this.lastId);
			hm.put(Constants.BoardSize, this.boardSize);
			
			//Set the second person to connect as backup server
			if(this.lastId == 2){
				
				try{
					this.backUpServerIP = RemoteServer.getClientHost();
				} catch (ServerNotActiveException e) {
					
					System.out.println("Error Setting up remote server ip");
				}						
			}
			
			
			hm.put(Constants.BackUpServerIP,this.backUpServerIP);
			return hm;
			
		}else{
			
			return createMessage(MessageType.ConnectError,"Maximum players limit reached. Cannot connect now");
		}
					
		
	}
	
	//Creates a new message to be send over to the client
	public HashMap<String,Object> createMessage(Integer msgType, Object msgObj){	
		
		HashMap <String,Object> hm = new HashMap<String,Object>();
		hm.put(Constants.MessageType, msgType);
		hm.put(Constants.MessageObject, msgObj);
				
		return hm;
		
		
	}
	
	//Makes the actual move
	//Updates the player score
	private void makeMove(Player p,int newX, int newY){
		
		int curX = p.getxPos();
		int curY = p.getyPos();
		
		int treasure = Math.abs(this.gameBoard[newX][newY]);
		numberOfTreasures -= treasure;		
		p.addPlayerScore(treasure);
		this.gameBoard[newX][newY] = p.getId();
		p.setxPos(newX);
		p.setyPos(newY);
		this.gameBoard[curX][curY] = 0;
		
		this.printGameBoard();
		
		
	}
	
	//Makes the move given the is of player and direction
	public HashMap<String,Object> move(int id,int dir){
		
		if(this.gameInfo == GameInfo.Waiting){
			//[TODO] It should return the time, how long should the client wait
			return createMessage(MessageType.Error,"Please wait for game to start..");
			
		}else if(this.gameInfo == GameInfo.GameOver){
			//[TODO] Return the final score to print who won the game 
			return createMessage(MessageType.GameOver,this.gameBoard);
		}
				
		Player p = this.pList.get(id);
		int curX = p.getxPos();
		int curY = p.getyPos();
		int newX = curX;
		int newY = curY;
		
		switch(dir){
		
			case Direction.UP:    newX -= 1;
				break;
			case Direction.DOWN:  newX += 1;
				break;
			case Direction.LEFT:  newY -= 1;
				break;
			case Direction.RIGHT: newY += 1;
				break;
			case Direction.STAY:
				HashMap<String,Object> hm = createMessage(MessageType.MazeObject,this.gameBoard);
				hm.put(Constants.BackUpServerIP,this.backUpServerIP);
				return hm;
			default:
				return createMessage(MessageType.Error,"Unknown move");
		}
		
		//Do the bounds checking		
		if(newX>=0 && newX < this.boardSize && newY>=0 && newY <this.boardSize){
			
			//Check if its not occupied
			if(!isOccupiedByPlayer(newX,newY)){
				
				makeMove(p,newX,newY);
				
				//Check game over condition here
				if(numberOfTreasures == 0){
					this.gameInfo = GameInfo.GameOver;					
					return createMessage(MessageType.GameOver,this.gameBoard);
				}
				
				
			}
		}
		
		HashMap<String,Object> hm =  createMessage(MessageType.MazeObject,this.gameBoard);
		hm.put(Constants.BackUpServerIP,this.backUpServerIP);
		return hm;
	}
	
	//Initializes the BackUpService Object
	public boolean startBackUpService(){
		
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry(this.backUpServerIP);
			this.backgs = (GameMethod) registry.lookup("BackUp");
			System.out.println("Done Handshaking");			
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  
		 
		
		return true;
		
	}
	
	//[TODO] Recreate the current GameImplementation Object here
	public void receivedBackUp(){
		
		System.out.println("Received Some info");
		
	}
}
