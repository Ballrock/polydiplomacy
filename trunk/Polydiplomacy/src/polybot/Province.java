package polybot;

import java.util.*;

public class Province {
	public static final double PEACE_COEFFICIENT = 0.4;
	private Node[] containsNodes = new Node[3];
	private String name;
	private boolean isSC = false;
	private Power ownedBy; 
	private boolean beingMovedTo = false;
	private Unit beingMovedToBy = null;
	private int defValue = 0;
	private int attValue = 0;
	private int strValue = 0;		//Amount of units that we own that can move into this province.
	private int compValue = 0;		//Competition value: the most enemy units from a single power that can move into this province.
	private Hashtable<String, Integer> adjUnitCount = new Hashtable<String, Integer>();

	private List<Power> demilitarisedBy = new ArrayList<Power>();

	
	public Province(String name){
		this.name = name;
	}
	
	public void setSupplyCentre(){
		isSC = true;
	}
	
	public boolean isSC(){
		return isSC;
	}
	
	public String getName(){
		return this.name;
	}
	
	public void capture(Power newOwner){
		ownedBy = newOwner;
	}
	
	public void setNode0(Node node){
		containsNodes[0]= node;
	}
	
	public void setNode1(Node node){
		containsNodes[1]= node;
	}
	
	public void setNode2(Node node){
		containsNodes[2]= node;
	}
	
	public Node[] getNodeArray(){
		return containsNodes;
	}
	
	public List<Node> getNodeList(){
		List<Node> nodes = new ArrayList<Node>();
		for (int nodeCount = 0; nodeCount < 3; nodeCount++){
			if (containsNodes[nodeCount] != null) nodes.add(containsNodes[nodeCount]);
		}
		return nodes;
	}
	
	public Power ownedBy(){
		return ownedBy; 
	}
	
	public boolean isOccupied(){ 
		for(int i =0; i < 3; i++){
			if(containsNodes[i]!=null) {
				if(containsNodes[i].isOccupied()) return true;
			}
		}
		return false;
	}

	// Works out defence value: Size of the largest power that is adjacent to this province.
	public void calcDefVal(Power me){
 		int defVal = 0;
 		
 		for (int currentNode = 0; currentNode < 2; currentNode++){
 			if (containsNodes[currentNode] != null){
 				for (int i = 0; i < containsNodes[currentNode].getAdjNodeCount(); i++){
 					if (containsNodes[currentNode].getAdjacentNodes()[i].isOccupied()){
 						Power adjPower = containsNodes[currentNode].getAdjacentNodes()[i].getUnit().getController();
 						if (adjPower.getName().compareTo(me.getName()) != 0 && adjPower.getPowerSize() > defVal){
 							defVal = adjPower.getPowerSize();
 						}
 					}
 				}
 			}
 		}
 		defValue = defVal;
 	}
	
	public void resetAttVal(){
		attValue = 0;
	}
	
	public void resetDefVal(){
		defValue = 0;
	}
	
	public void setAttVal(int ownersPowerSize){
		attValue = ownersPowerSize;
		if (ownedBy.acceptedPeace()) attValue *= PEACE_COEFFICIENT;
		defValue = 0;
	}
	
	public List<Node> getAdjacentProvinces(){
//		 RETURNS NODE NOT PROVINCE NAMES
		List<Node> adjProvinces = new ArrayList<Node>();
		
		for (int nodeCount = 0; nodeCount <3; nodeCount++){
			if (containsNodes[nodeCount] != null){
				for (int i = 0; i < containsNodes[nodeCount].getAdjNodeCount(); i++)
					if (!adjProvinces.contains((containsNodes[nodeCount].getAdjacentNodes()[i])))
						adjProvinces.add(containsNodes[nodeCount].getAdjacentNodes()[i]);
			}
		}
		return adjProvinces;
	}

	public void setStrengthValue(int units){
		strValue = units;	
	}

	public void setCompValue(int units){
		compValue = units;	
	}
	
	public int getCompValue(){
		return compValue;	
	}

	public int getStrengthValue(){
		return strValue;	
	}
	
	public Unit getUnit(){
		
		for(int i = 0; i       <3        ; i++) {//LOL, it's a heart <3!
			if(containsNodes[i]!=null) {
				if(containsNodes[i].isOccupied()){
					return containsNodes[i].getUnit();
				}
			}
		}
		
		return null;
		
	}
	
	public void setBeingMovedTo(Unit movingUnit){
		beingMovedToBy = movingUnit;
		beingMovedTo = true;
	}
	
	public boolean isBeingMovedTo(){
		return beingMovedTo;
	}
	
	public Unit getUnitBeingMovedTo(){
		return beingMovedToBy;
	}
	
	public void resetBeingMovedTo(){
		beingMovedTo = false;
		beingMovedToBy = null;
	}
		
	public int getDefVal(){
		return defValue;
	}
	
	public int getAttVal(){
		return attValue;
	}
	
	public void setAdjUnitCount(Hashtable<String, Integer> newAdjUnitCount){
		adjUnitCount = newAdjUnitCount;
	}
	
	public int getAdjUnitCount(String power){
		return adjUnitCount.get(power);
	}
	
	public Unit getOccupyingUnit(){
		for(int i = 0; i < 3; i++){
			if(containsNodes[i]!=null) {
				if(containsNodes[i].isOccupied()) return containsNodes[i].getUnit();
			}
		}
		return null;
	}
	
	public void demilitarise(Power DMZpower){
		demilitarisedBy.add(DMZpower);
	}
	
	public void remilitarise(Power NOTDMZpower){
		demilitarisedBy.remove(NOTDMZpower);
	}
	
	public List<Power> getDemilitariseList(){
		return demilitarisedBy;
	}
	
	
}
