package polybot;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public abstract class Unit extends Observable implements Observer{
	protected Node location;
	protected Power controller; 
	protected String order = null;
	protected Node orderLocation = null;
	protected Unit orderSupportUnit = null;
	protected boolean mustRetreat = false, beingRemoved = false;
	private List<Node> retreatNodeList;
	private int stance = 0; 
	
	public void setStance(int stance){
		this.stance = stance; 
	}
	
	public int getStance(){
		return stance; 
	}
	
	public Unit(Power controller, Node location){
		this.controller = controller;
		this.location = location; 
	}
	
	public Power getController(){
		return controller;
	}
	
	public Node getLocation(){
		return location;
	}
	
	public void setLocation(Node location){
		this.location = location;
	}
	
	public void setHold(){
		order = "HLD";
	}
	
	public void setMove(Node MTOProvNode){
		order = "MTO";
		orderLocation = MTOProvNode;
        MTOProvNode.getProvince().setBeingMovedTo(this);
	}
	
	public void setSupportHold(Unit SUPUnit){
		order = "SUP";
		orderSupportUnit = SUPUnit;
		orderLocation = null;
	}
	
	public void setSupportMove(Unit SUPUnit, Node SUPNode){
		order = "SUP";
		orderSupportUnit = SUPUnit;
		orderLocation = SUPNode;
	}
	
	//DumbBot doesn't implement convoy yet.
	/*public void setConvoy(Unit CVYUnit, Node CTOProvNode){
		order = "CVY";
		orderSupportUnit = CVYUnit;
		orderLocation = CTOProvNode;
	}
	
	public void setMoveByConvoy(Unit CVYUnit, Node ){
		order = "MTO";
		orderLocation = MTOProvNode;
		
	}*/
	
	public String getOrderToken(){
		return order;
	}
	
	public Node getOrderLocation(){
		return orderLocation;
	}
	
	public void setMRT(List<Node> retreatNodeList){
//		System.out.println("I HAVE BEEN SET TO BE RETREATED.." );
		mustRetreat = true;
		this.retreatNodeList = retreatNodeList;
	}
	
	public List<Node> getMRTList(){
		return retreatNodeList;
	}
	
	public boolean mustRetreat(){
		return mustRetreat;
	}
	
	public void setDisband(){
		order = "DSB";
	}
	
	public void setRetreatTo(Node MRTProv){
		order = "RTO";
		orderLocation = MRTProv;
        MRTProv.getProvince().setBeingMovedTo(this);	
	}
	
	public void setRemoval(){
		order = "REM";
		beingRemoved = true;
	}
	
	public void retreat() {
		if (this.retreatNodeList.isEmpty()) {
			setDisband();
		}
		else {
			//There is some nodes available
			List<Node> sortedRetreatNodeList = this.location.getSortedAdjacentNodesListByDestValue(this.controller);
			Node currentNode = Map.getNodeRandom(sortedRetreatNodeList, this.controller);
			while (currentNode.isOccupied() && currentNode.getProvince().isBeingMovedTo())
				currentNode = Map.getNodeRandom(sortedRetreatNodeList, this.controller);
			setRetreatTo(currentNode);
		}
	}
	
	public void makeOrder(Map m) {
		if (this.getOrderToken() == null) {
			/* We don't know what to do */
			Node destNode;
			List<Node> listNodes = this.location.getAdjacentNodesList();
			listNodes.add(this.location);
			listNodes = this.location.getSortedAdjacentNodesListByDestValue(this.controller);
			destNode = Map.getNodeRandom(listNodes, this.controller);
			
			/* Dest node == current Node -> Hold */
			if (destNode.equals(this.location)) {
				this.setHold();
			}
			else {
				if(destNode.getProvince().isOccupied()) {
					if (destNode.getProvince().getUnit().getController().getName().compareTo(this.controller.getName()) == 0) {
						/* The current node is occupied by a unit owned by us */
						if (destNode.getProvince().getUnit().getOrderToken() == null) {
							/* The unit on the node don't know yet what to do, we ask for an action to figure out what we gonna do */
							//destNode.getProvince().getUnit().makeOrder(m);
							this.setHold();
						}
						else if (destNode.getProvince().getUnit().getOrderToken().compareTo("MTO") != 0 && destNode.getProvince().getUnit().getOrderToken().compareTo("CTO") != 0) {
							/* The unit is not moving */
							
							/* We support it if we can reach the province where she's moving to*/
							this.setSupportHold(destNode.getProvince().getUnit());

						}
						else if (destNode.getProvince().getUnit().getOrderToken().compareTo("MTO") == 0) {
							/* The unit is moving out, we take her place */
							this.setMove(destNode);
						}
					}
					else {
						/* This province is occupied by another power, so if we've chosen this one, we attack it */
						this.setMove(destNode);
					}
				}
				/* Empty spot */
				else {
					if (destNode.getProvince().isBeingMovedTo()) {
						/* A unit wants to move to this province, maybe we can support it */
						this.setSupportMove(destNode.getProvince().getUnitBeingMovedTo(), destNode);
					}
					else {
						/* It's a free province where nobody wants to go, so we can quietly move this way */
						this.setMove(destNode);
					}
				}
			}
		}
	}
	
	public boolean beingRemoved(){
		if (beingRemoved) {
			beingRemoved = false;
			return true;
		}
		return false;
	}
	
	public abstract String[] getCompleteOrder();
    public abstract String getUnitType();
}
