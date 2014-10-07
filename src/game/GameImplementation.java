package game;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Random;

import server.WaitConnect;

public class GameImplementation extends UnicastRemoteObject implements GameMethod{

	private int boardSize;
	private int numberOfTreasures;
	private int[][] gameBoard;	
	private HashMap <Integer,Player> pList;
	private int maxPlayers;
	int lastId = 0;
	int backUpServerID;
	long startTime = 0; 
	int serverID = 1;
	GameMethod backgs;	
	
	
	GameInfo gameInfo = GameInfo.NotStarted;
	
	
	private static final long serialVersionUID = -4933868291603601249L;
			
	
	public GameImplementation(int bSize,int nTreasures,int clientID,int backUpServerID,HashMap<Integer,Player> pList) throws RemoteException {
		super();
 
		this.boardSize = bSize;		
		this.numberOfTreasures = nTreasures;
		this.gameBoard = new int[boardSize][boardSize];
		
		this.pList = pList;
		
		
		this.maxPlayers = bSize*bSize - 1;
		this.backUpServerID =backUpServerID;
		this.serverID = clientID;
		this.gameInfo = GameInfo.Started;
					
	}
	
	
	public GameImplementation(int bSize,int nTreasures) throws RemoteException {
		super();
 
		this.boardSize = bSize;
		this.numberOfTreasures = nTreasures;
		this.gameBoard = new int[boardSize][boardSize];	
		this.pList = new HashMap<Integer,Player>();
		this.maxPlayers = bSize*bSize - 1;
		this.backUpServerID = -1;
		this.serverID = 1;
		
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
		
	}
	
	
	public synchronized HashMap<String,Object> ConnectToGame(String clientIP,int clientPort){
		
		
		switch(this.gameInfo){
		
			case NotStarted:
					this.startTime = System.currentTimeMillis();
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
			p.setIP(clientIP);
			p.setPort(clientPort);
						
			this.pList.put(this.lastId, p);
			setRandomPlayerPosition(p);
			HashMap <String,Object> hm = createMessage(MessageType.ConnectSuccess,this.lastId);		
			hm.put(Constants.TimeLeft, this.startTime+20000 - System.currentTimeMillis());			
			//Set the second person to connect as backup server
			if(this.lastId == 2){
				this.backUpServerID  = 2;
			}						
			hm.put(Constants.BackUpServerID,this.backUpServerID);
			hm.put(Constants.BoardSize, this.boardSize);
			hm.put(Constants.Treasures, this.numberOfTreasures);		
			
			return hm;
			
		}else{
			
			return createMessage(MessageType.ConnectError,"Maximum players limit reached. Cannot connect now");
		}
			
	
		
	}
	
	
	//Connects a client to the game
	public synchronized HashMap<String,Object> GetInitialGameState(int id){
		
		HashMap <String,Object> hm;
		
		if(this.gameInfo == GameInfo.Waiting){
			
			hm = createMessage(MessageType.Error,"Still Waiting");
			
		}else{
			
			if(this.backUpServerID == id){
				this.updateBackUpObject();
				HeartBeatThread hbThread = new HeartBeatThread(this.backgs,this);
				hbThread.start();
			}
			
			hm = createMessage(MessageType.ConnectSuccess,this.gameBoard);
			hm.put(Constants.Players,this.pList);
		}					
		
		return hm;
		
	}
	
	//Creates a new message to be send over to the client
	public HashMap<String,Object> createMessage(Integer msgType, Object msgObj){	
		
		HashMap <String,Object> hm = new HashMap<String,Object>();
		hm.put(Constants.MessageType, msgType);
		hm.put(Constants.MessageObject, msgObj);
				
		return hm;
		
		
	}
	
	public void HeartBeat(){
		System.out.print(".");
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
		
		//this.printGameBoard();
		
		
	}
	
	
	
	//Makes the move given the is of player and direction
	public synchronized HashMap<String,Object> move(int id,int dir){			
		
		HashMap<String,Object> hm;
		
		//Check if this request is actually received by the backup server
		//This essentially means that our main server is down
		//We need to create a new backupserver here
		if(this.serverID == this.backUpServerID){				
			this.backUpServerID++;
			this.updateBackUpObject();
			//Also start the new heartbeat thread here
			HeartBeatThread hbThread = new HeartBeatThread(this.backgs,this);
			hbThread.start();
			
		}				
		
		if(this.gameInfo == GameInfo.Waiting){

			return createMessage(MessageType.Error,"Please wait for game to start..");
			
		}else if(this.gameInfo == GameInfo.GameOver){
 
			hm = createMessage(MessageType.GameOver,this.gameBoard);
			hm.put(Constants.Players, this.pList);
			return hm;
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
				hm = createMessage(MessageType.MazeObject,this.gameBoard);
				hm.put(Constants.BackUpServerID,this.backUpServerID);
				hm.put(Constants.Players,this.pList);
				hm.put(Constants.Treasures,this.numberOfTreasures);
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
				if(this.numberOfTreasures == 0){
					this.gameInfo = GameInfo.GameOver;
					 
					hm = createMessage(MessageType.GameOver,this.gameBoard);
					hm.put(Constants.Players, this.pList);
					return hm;
				}
				
				
			}
		}
		
		hm =  createMessage(MessageType.MazeObject,this.gameBoard);
		hm.put(Constants.BackUpServerID,this.backUpServerID);
		hm.put(Constants.Players,this.pList);
		hm.put(Constants.Treasures,this.numberOfTreasures);
							
		try {
			this.backgs.receiveBackUp(this.gameBoard, this.pList,this.numberOfTreasures,this.backUpServerID);
		} catch (RemoteException e) {
			
			System.out.println("BackUp is down. Ignoring it as this will be handled by server");
			
		}
		
			
		
		
		return hm;
	}
	
	
	public GameMethod updateBackUpObject(){
		
		Registry registry;
		
		//Keep finding and updating the backup
		while(true){

			Player backUpPlayer = this.pList.get(this.backUpServerID);
			try {
				registry = LocateRegistry.getRegistry(backUpPlayer.getIP(), backUpPlayer.getPort());
				this.backgs = (GameMethod) registry.lookup("GameImplementation");
				System.out.println("Updated BackUp Object");
				System.out.println("Player "+(char)(this.backUpServerID+64)+" is now backup.");
				//Send the initial updated state
				this.backgs.receiveBackUp(this.gameBoard, this.pList,this.numberOfTreasures,this.backUpServerID);
				break;
			} catch (RemoteException e) {
				System.out.println("Player "+(char)(this.backUpServerID+64)+" is down");
				this.backUpServerID++;
			} catch (NotBoundException e) {
				
				e.printStackTrace();
				break;
			}
			  
		}
		
		return this.backgs;
		
	}
		

	public void receiveBackUp(int[][] gameBoard,HashMap<Integer,Player> pList,int numberOfTreasures,int backUpServerID){	
		this.gameBoard = gameBoard;		
		this.pList = pList;
		this.numberOfTreasures = numberOfTreasures;
		this.backUpServerID = backUpServerID;
		
		System.out.print("*");
		
	}
}
