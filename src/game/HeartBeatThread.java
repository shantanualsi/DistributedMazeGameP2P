package game;

import java.rmi.RemoteException;


public class HeartBeatThread extends Thread {
	
	GameMethod backObj;
	GameImplementation gi;
	
	public HeartBeatThread(GameMethod backObj,GameImplementation gi){
		
		this.backObj = backObj;		
		this.gi = gi;
		
	}
	
	public void run(){				
		
		System.out.println("Heartbeat thread started");
		while(true){
			
			try{
				
				this.backObj.HeartBeat();
				
			}catch(RemoteException e){
				
				this.backObj = gi.updateBackUpObject();
			}
			
			try{
				
				Thread.sleep(2000);
				
			}catch(Exception e){
				
				System.out.println("Some exception occured while sleeping heartbeat thread");
			}
			
			
		}
		
		/*try {
			System.out.println("Starting heartbeat"); 
			while(true){
				//Send request to Backup
				gs.handleHeartBeat();
				System.out.println("-");
				Thread.sleep(1000);
				Thread.yield();
			}
		} catch (RemoteException re){
			//Exception received => Backup is down.
			System.out.println("Backup down! Now starting getting new backup");
			gi.updateBackUpObject();
		}catch (InterruptedException ie) {		
			// TODO Auto-generated catch block
			ie.printStackTrace();
		}*/
	}
}
