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
	private boolean waitOrders = false;
	private int stance = 0; 
	
	public void setStance(int stance){
		this.stance = stance; 
	}
	
	public boolean isWaitingOrder() {
		return this.waitOrders;
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
		for (Node n : retreatNodeList)
			System.out.println("retreat node : "+n.getName());
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
			Node currentNode = Map.getNodeRandom(this.retreatNodeList, this.controller);
			while (currentNode.isOccupied() && currentNode.getProvince().isBeingMovedTo()) {
				System.out.println(currentNode.getName());
				currentNode = Map.getNodeRandom(retreatNodeList, this.controller);
			}
			System.out.println("LAST : "+currentNode.getName());
			setRetreatTo(currentNode);
		}
	}
	
	public void makeOrder(Map m) {
		
		Node destNode;
		List<Node> listNodes = this.location.getAdjacentNodesList();
		listNodes.add(this.location);
		listNodes = this.location.getSortedAdjacentNodesListByDestValue(this.controller);
		destNode = Map.getNodeRandom(listNodes, this.controller);
		
		if (this.getOrderToken() == null) {
			/* We don't know what to do */
			
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
							if (!destNode.getProvince().getUnit().isWaitingOrder()) {
								this.waitOrders = true;
								destNode.getProvince().getUnit().makeOrder(m);
								if (destNode.getProvince().getUnit().getOrderToken().compareTo("MTO") == 0 || destNode.getProvince().getUnit().getOrderToken().compareTo("CTO") == 0)
									/* The unit is moving away, we take her place */
									this.setMove(destNode);
								else
									/* She's staying there so we support her */
									this.setSupportHold(destNode.getProvince().getUnit());
								this.waitOrders = false;
							}
							else
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
			this.setChanged();
			this.notifyObservers(m);
		}
		else {
			/* We've already choosen a place to go but we are told to do it again, so we'll compare the current spot with another */
			if (this.location.equals(destNode)) {
				this.setHold();
			}
			else {
				if (this.location.getDestValue(this.controller.getName()) < destNode.getDestValue(this.controller.getName())) {
					/* The current spot worthier than the randomed one so we move on the randomed */
					if(destNode.getProvince().isOccupied()) {
						if (destNode.getProvince().getUnit().getController().getName().compareTo(this.controller.getName()) == 0) {
							/* The current node is occupied by a unit owned by us */
							if (destNode.getProvince().getUnit().getOrderToken() == null) {
								/* The unit on the node don't know yet what to do, we ask for an action to figure out what we gonna do */
								if (!destNode.getProvince().getUnit().isWaitingOrder()) {
									this.waitOrders = true;
									destNode.getProvince().getUnit().makeOrder(m);
									if (destNode.getProvince().getUnit().getOrderToken().compareTo("MTO") == 0 || destNode.getProvince().getUnit().getOrderToken().compareTo("CTO") == 0)
										/* The unit is moving away, we take her place */
										this.setMove(destNode);
									else {
										/* She's staying there so we support her */
										System.out.println("ICI2");
										this.setSupportHold(destNode.getProvince().getUnit());
									}
									this.waitOrders = false;
								}
								else
									this.setHold();
							}
							else if (destNode.getProvince().getUnit().getOrderToken().compareTo("MTO") != 0 && destNode.getProvince().getUnit().getOrderToken().compareTo("CTO") != 0) {
								/* The unit is not moving */
								
								/* We support it if we can reach the province where she's moving to*/
								System.out.println("ICI3");
								this.setSupportHold(destNode.getProvince().getUnit());

							}
							else if (destNode.getProvince().getUnit().getOrderToken().compareTo("MTO") == 0) {
								/* The unit is moving out, we take her place */
								this.setMove(destNode);
							}
							else if (!(destNode.getProvince().getUnit().getOrderToken().compareTo("SUP") == 0 && destNode.getProvince().getUnit().orderSupportUnit.equals(this))) {
								if (destNode.getProvince().getUnit().getOrderLocation() == null) {
									System.out.println("ICI1");
									this.setHold();
								}
								else {
									if (destNode.getProvince().getUnit().getOrderLocation().getAdjacentNodesList().contains(this.location))
										this.setMove(destNode);
								}
							}
							else this.setHold();
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
							System.out.println("ICI4 - "+this.location.getName()+" , "+destNode.getName());
							this.setSupportMove(destNode.getProvince().getUnitBeingMovedTo(), destNode);
						}
						else {
							/* It's a free province where nobody wants to go, so we can quietly move this way */
							this.setMove(destNode);
						}
					}
				}
				else
					this.setHold();
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
	
	
	@Override
	public void update(Observable arg0, Object arg1) {
		Unit n = (Unit) arg0;
		if (this.getOrderToken() == null) {
			/* We don't got order yet */
			if (this.location.getAdjacentNodesList().contains(n.getOrderLocation())) {
				if (this.location.equals(n.getOrderLocation())) {
					this.setHold();
				}
				else {
					if (n.getOrderToken().compareTo("MTO") == 0 || n.getOrderToken().compareTo("CTO") == 0) {
						System.out.println("ICI5 - "+this.location.getName()+" , "+n.getOrderLocation().getName());
						this.setSupportMove(n, n.getOrderLocation());
					}
					else if (n.getOrderToken().compareTo("SUP") == 0)
						this.setHold();
					else {
						this.setSupportHold(n);
						System.out.println("ICI6");
					}
				}
			}
		}
		else {
			/* We already got orders, but we'll compare */
			if (this.location.getAdjacentNodesList().contains(n.getOrderLocation())) {
				if (this.location.equals(n.getOrderLocation())) {
					this.setHold();
				}
				else {
					if (this.location.getDestValue(this.controller.getName()) < n.getOrderLocation().getDestValue(this.controller.getName())) {
						if (!(n.getOrderToken().compareTo("MTO") == 0 || n.getOrderToken().compareTo("CTO") == 0)
								&& !(n.getOrderToken().compareTo("SUP") == 0)) {
							this.setSupportHold(n);
							System.out.println("ICI8");
						}
					}
				}
			}
		}
	}

	public abstract String[] getCompleteOrder();
    public abstract String getUnitType();
}
