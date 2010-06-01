package polybot;

import java.util.*;

public class Node {

	private String name; 
	private Unit unit;
	private Province province; 
	private Node[] adjacentNodes = new Node[20]; 
	private Hashtable<String, Integer> destValue = new Hashtable<String, Integer>();		//Destination value: final value for that province of how much we want to move there.private int[] proximityMap = new int[10];
	private int[] proximityMap = new int[10];
	private int adjNodeCount = 0;
	private boolean buildHere = false;
	
	
	public Node(String name){
		this.name = name; 
	}
	
	public Province getProvince(){
		return this.province;
	}
	
	public String toString(){
		return this.name;
	}
	
	public void occupy(Unit unit){
		this.unit = unit;
	}
	
	public Node[] getAdjacentNodes(){
		return adjacentNodes;
	}
	
	public int getAdjNodesCount(){
		return adjNodeCount;
	}
	
	public List<Node> getAdjacentNodesList(){
		List<Node> nodes = new ArrayList<Node>();
		for (int nodeCount = 0; nodeCount < adjNodeCount; nodeCount++){
			nodes.add(adjacentNodes[nodeCount]);
		}
		return nodes;
	}
	
	public List<Node> getSortedAdjacentNodesListByDestValue(Power me) {
		List<Node> nodes = getAdjacentNodesList();
		for(int i = nodes.size(); --i>=0;){
			boolean flipped = false;
			for (int j = 0; j < i; j++){
				if (nodes.get(j).destValue.get(me.getName()) < nodes.get(j+1).getDestValue(me.getName())){
					Node panda = nodes.get(j); 
					nodes.remove(j);
					nodes.add(j+1, panda);
					flipped = true;
				}
			}
			if (!flipped){
				return nodes;
			}
		}

		return nodes;
	}
	
	public void setProvince(Province province){
		this.province = province;
	}
	
	public boolean isOccupied() {
		return unit != null;
	}
		
	public void unoccupy(){
		this.unit = null;
	}
	
	void addAdjacentNode(Node adjNode){
		adjacentNodes[adjNodeCount] = adjNode;
		adjNodeCount++;
	}

	public String getName() {
		return this.name;
	}
	
	public int getAdjNodeCount(){
		return adjNodeCount;
	}
	
	public Unit getUnit(){
		return unit;
	}
	
	public void setProximityMap(int prox, int val){
		proximityMap[prox] = val;
	}
	
	public int getProximity(int prox){
		return proximityMap[prox];
	}

	public void setDestValue(String power, int value){
		destValue.put(power, value);
	}
	
	public boolean isAdjacentTo(Node n) {
		for (int i = 0; i < this.adjacentNodes.length; i++) {
			if (this.adjacentNodes[i].equals(n))
				return true;
		}
		return false;
	}
	
	public int getDestValue(String power){
		return destValue.get(power);
	}

	public boolean buildHere(){		
		if (buildHere){
			buildHere = false;
			return true;
		}
		return false;
	}
	
	public boolean isBuildingHere() {
		return this.buildHere;
	}
	
	public void setBuildHere(){
		buildHere = true;
	}
}
