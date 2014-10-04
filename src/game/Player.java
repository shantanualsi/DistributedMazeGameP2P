package game;

import java.io.Serializable;

public class Player implements Serializable{
	
	private int playerScore;
	private int xPos;
	private int yPos;
	private int id;
	private String ip;
	
	private static final long serialVersionUID = -4533268491603601249L;
	 
	public Player(int id){
		this.id = id;		
		this.playerScore = 0;
	}
	
	public int getPlayerScore() {
		return playerScore;
	}

	public void setPlayerScore(int playerScore) {
		this.playerScore = playerScore;
	}
	
	public void addPlayerScore(int addScore) {
		this.playerScore += addScore;
	}

	
	public int getId() {
		return id;
	}

	public int getScore(){
		
		return this.playerScore;
	}
	
	public int getyPos() {
		return yPos;
	}

	public void setyPos(int yPos) {
		this.yPos = yPos;
	}

	
	public int getxPos() {
		return xPos;
	}

	public void setxPos(int xPos) {
		this.xPos = xPos;
	}

	public void setIP(String ip) {

		this.ip = ip;
		
	}
	
	public String getIP(String ip) {
		
		return this.ip;
		
	}


}
