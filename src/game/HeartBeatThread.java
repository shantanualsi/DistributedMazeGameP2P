package game;
import java.net.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

public class HeartBeatThread extends Thread {
	int backupId = 0;
	GameImplementation gi;
	GameMethod gs;
	
	public HeartBeatThread(GameImplementation gi,GameMethod gs){
		this.gi = gi;;
		this.gs = gs;
	}
	
	public void run(){
		
		try {
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
		}
	}
}
