package polybot;

import java.util.ArrayList;
import java.util.List;

public abstract class Unit {
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
