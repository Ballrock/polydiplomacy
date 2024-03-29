package polybot;


import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import dip.daide.comm.*;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.*;

/**
 * @author Benjamin Antoine
 *
 */
public class PolyDiplomacy implements MessageListener{

	static final String VERSION = "0.1";
    private String name;
    private static Server serv;
    private static LinkedBlockingQueue<String[]> messageQueue = new LinkedBlockingQueue<String[]>();
    static boolean hasStarted = false;
    private Map map;
    private Power me; 
    private int passcode;
    private List<String[]> ordList = new ArrayList<String[]>();
    private int iteration = 0;
    
    String year; 
    private int turn = 0; 
    
	private final int SPR = 0; 
	private final int SUM = 1; 
	private final int FAL = 2; 
	private final int AUT = 3; 
	private final int WIN = 4;
    
    public PolyDiplomacy(InetAddress ip, int port, String name){
    	this.name = name;
    	try {
    	    serv = new Server(ip, port);
    	    serv.addMessageListener(this);
    	    serv.connect();
    	    String[] msg = new String[]{
    	    		"NME",
                    "(", "'" + this.name + "'", ")",
                    "(", "'" + VERSION + "'", ")"
                  };
    	    
    	    serv.send(msg);
    	 
    	    
    	} catch (IOException ioe){
    		ioe.printStackTrace();
    	} catch (DisconnectedException de){
    		System.out.println("Ok, we're disconnected. Exiting...");
    		System.exit(0);
    	} catch (UnknownTokenException ute){
    		System.err.println("Unknown token '" + ute.getToken() + "'");
    		System.exit(1);
    	} 

    }
	
	public static void main(String[] args) {
		try {
		    new PolyDiplomacy(InetAddress.getByName(args[0]),
			       Integer.parseInt(args[1]),
			       args[2]);

		} catch (ArrayIndexOutOfBoundsException be){
		    usage();
		} catch (UnknownHostException uhe){
		    System.err.println("Unknown host: " + uhe.getMessage());
		} catch (NumberFormatException nfe){
		    usage();
		}
	}
    
	public static void usage(){
    	System.err.println("Usage:\n" +
    			   "  Diplominator <ip> <port> <name>");
    }

	public void messageReceived(String[] message) {
		
		System.out.print("Server: ");
		printMessage(message);
		
		if (!hasStarted){
			handlePreGameMessage(message);
		} else if (message[0].equals("HLO")){
			handleHLO(message);
		} else if (message[0].equals("ORD")){
				if(!message[7].equals(me.getName())) ordList.add(message);
		} else if (message[0].equals("NOW")){
			
			
		    
			if (message[2].equals("SPR")) turn = 0;
			if (message[2].equals("SUM")) turn = 1;
			if (message[2].equals("FAL")) turn = 2;
			if (message[2].equals("AUT")) turn = 3;
			if (message[2].equals("WIN")) turn = 4;
			year = message[3];
			if(turn == SUM || turn == WIN || turn == AUT || turn == FAL) {
				map.handleORD(ordList, me);
			}
			ordList.removeAll(ordList);
			map.storeSeason(message[2]);
			map.updateUnits(message);
			map.linkUnits(me);
			List<String[]> orders;
			if (turn == FAL || turn == SPR) {
				map.calcDest(me);
				map.genMoveOrders(me);
				orders = map.submitOrders(me);
			}
			else {
				orders = map.processNOW(me);
			}
			for (int i = 0; i < orders.size(); i++){
				sendMessage(orders.get(i));
			}
			
			
		} else if (message[0].equals("SCO")){
			// Handle SCO
			map.handleChances(me);
			map.handleORD(ordList, me);
			ordList.removeAll(ordList);
			map.updateSCO(message);
		}else if (message[0].equals("YES")){ 
			// DO NOTHING
		}else if (message[0].equals("MIS")){ 
		
		} else if (message[0].equals("OUT")){
			map.listOfPowers.get(message[2]).setOut();
			//SOMEONE HAS BEEN BOOTED. THEY LOSE
		} else if (message[0].equals("HST")){
			// Handle HST
		} else if (message[0].equals("OFF")){
			System.exit(0);
		} else if (message[0].equals("THX")){
					
		} else if (message[0].equals("CCD")){
			 
		}
		else if (message[0].equals("SVE")) {
			System.out.println("Saving game : "+message[2]);
		}
		else if (message[0].equals("SLO")){
			
			if (message[2].equals(me.getName())){
				System.out.println("The game is over. We won.");
				System.exit(0);
			}
			else {
				System.out.println("The game is over. " + message[2] + " won.");
				System.exit(0);
			}
		}
		else {
			messageQueue.add(message);
		}
	}
	
	private void handleHLO(String[] message) {
		me = map.getPower(message[2]);
		passcode = Integer.parseInt(message[5]);
	}

	private void handlePreGameMessage(String[] message) {
		
		if(message[0].equalsIgnoreCase("MAP")){
			if(message[2].equalsIgnoreCase("'STANDARD'")){
				sendMessage(new String[] {"MDF"});
			}
		}
		if(message[0].equalsIgnoreCase("MDF")){
			map = new Map(message);
			sendMessage(new String[]{"YES", "(", "MAP", "(", "'STANDARD'", ")", ")"});	
			hasStarted = true; 
			
		}
	}

	static void printMessage(String [] msg){
		for(int i =0; i < msg.length; i++){
			System.out.print(msg[i]+" ");
		}
		System.out.println();
	}
	
    void sendMessage(String [] msg){
		try {
			serv.send(msg);
			System.out.print("PolyDiplomacy: ");
			printMessage(msg);
			
		} catch (DisconnectedException de){
    		System.out.println("No longer connected to server. Exiting.");
    		/* Try reconnecting */
    		System.out.println(this.me.getName());
    		sendMessage(new String[]{"IAM", "(", this.me.getName(), ")", "(", new Integer(this.passcode).toString(), ")" });
    		
    	} catch (UnknownTokenException ute){
    		System.err.println("Unknown token '" + ute.getToken() + "'");
    		System.exit(1);
    	} 
	}
}
