package polybot;

import java.util.*;

public class Power {
	
	private String name; 
	
	private Province[] ownedSC = new Province[34]; 
	private int countSC = 0;
	
	private Army[] armies = new Army[34];
	private int armyCount = 0;
	
	private Fleet[] fleets = new Fleet[34];
	private int fleetCount = 0;
	
	private int powerSize = 0;
	final private int M_SIZE_SQUARE_COEFFICIENT = 1;
	final private int M_SIZE_COEFFICIENT = 4;
	final private int M_SIZE_CONSTANT = 16;
	
	private int balance = 0 ; 
	
	private double enemyFactor;
	
	private boolean acceptedPeace = false;
	private boolean isOut = false;
	private boolean backstab = false; 
	
	private List<Province> homes = new ArrayList<Province>();
	
	
	public double getEnemyFactor(){
		return enemyFactor;
	}

	
	public void resetSCs(){
		countSC = 0;
	}
	
	public Power(String name){
			this.name = name;
	}
	
	public String getName(){
		return this.name;
	}
	
	public Army[] getArmies(){
		return armies;
	}
	
	public Fleet[] getFleets(){
		return fleets;
	}
	
	public int getFleetSize(){
		return fleetCount;
	}
	
	public int getArmySize(){
		return armyCount;
	}
	
	public int countSCs(){
		return countSC;
	}
	
	public void addUnit(Unit unit){
		if (unit instanceof Army){
			armies[armyCount] = (Army) unit;
			armyCount++;
		} else {
			fleets[fleetCount] = (Fleet) unit;
			fleetCount++;
		}
			
	}
	
	public void killAllUnits(){
		fleetCount = 0;
		armyCount = 0;
	}

	public void addSC(Province sc){
		ownedSC[countSC] = sc;
		countSC++;
	}
	
	public Province[] ownedSCs(){
		Province[] powerSC = new Province[countSC];
		for (int i = 0; i<countSC; i++){
			powerSC[i] = ownedSC[i];
		}
		return powerSC;
	}
	
	public void calcPower(){
		powerSize = M_SIZE_SQUARE_COEFFICIENT * countSC * countSC + M_SIZE_COEFFICIENT * countSC + M_SIZE_CONSTANT;
	}
	
	public int getPowerSize(){
		return powerSize;
	}
	
	public void addHome(Province province){
		homes.add(province);
	}
	
	
	public List<Province> getHomes(){
		return homes;
	}
	
	public List<Unit> getUnitList(){
		List<Unit> units = new ArrayList<Unit>();
		for (int unitCount = 0; unitCount < armyCount; unitCount++){
			units.add(armies[unitCount]);
		}
		for (int unitCount = 0; unitCount < fleetCount; unitCount++){
			units.add(fleets[unitCount]);
		}
		return units;
	}
	
	public List<Unit> getUnitListSortedASC() {
		List<Unit> units = getUnitList();
		for(int i = units.size(); --i>=0;){
			boolean flipped = false;
			for (int j = 0; j < i; j++){
				if (units.get(j).getLocation().getDestValue(this.name) > units.get(j+1).getLocation().getDestValue(this.name)){
					Unit panda = units.get(j); 
					units.remove(j);
					units.add(j+1, panda);
					flipped = true;
				}
			}
			if (!flipped){
				return units;
			}
		}

		return units;
	}
	
	public List<Unit> getUnitListSortedDESC() {
		List<Unit> units = getUnitList();
		for(int i = units.size(); --i>=0;){
			boolean flipped = false;
			for (int j = 0; j < i; j++){
				if (units.get(j).getLocation().getDestValue(this.name) < units.get(j+1).getLocation().getDestValue(this.name)){
					Unit panda = units.get(j); 
					units.remove(j);
					units.add(j+1, panda);
					flipped = true;
				}
			}
			if (!flipped){
				return units;
			}
		}

		return units;
	}
	
	// Retardbot's vulnerability to a stab from this power
	public int stabGain(Power retardbot){
		// iterate over this power's units getting average value of retardbot's provinces adjacent to this unit
		int stabVulVal = 0;
		
		List<Unit> units = getUnitList();
		
		while (!units.isEmpty()){
			List<Node> nodes = units.get(0).getLocation().getAdjacentNodesList();
			int unitStabability = 0, noOfAdjNodes = 0;
			Node multipleCoasts = null;
			
			while (!nodes.isEmpty()){
				if (nodes.get(0).getName().substring(4).compareTo("CS") == 0 && multipleCoasts != null){
					if (nodes.get(0).getDestValue(this.name) > multipleCoasts.getDestValue(this.name)){
						unitStabability += - multipleCoasts.getDestValue(this.name) + nodes.get(0).getDestValue(this.name);
					}
				}
				else {
					boolean maybeStabbed = false;
					if (nodes.get(0).getProvince().isSC()){
						if (nodes.get(0).getProvince().ownedBy().equals(retardbot.getName())){
							maybeStabbed = true;
						}
					}
					if (nodes.get(0).getProvince().isOccupied()){
						if (nodes.get(0).getUnit().getController().getName().equals(retardbot.getName())){
							maybeStabbed = true;
						}
					}
					if (maybeStabbed){
						unitStabability += nodes.get(0).getDestValue(this.name);
						noOfAdjNodes++;
						if (nodes.get(0).getName().substring(4).equals("CS")){
							multipleCoasts = nodes.get(0);
						}
					}
				}
				nodes.remove(0);
			}
			
			stabVulVal += unitStabability / noOfAdjNodes;
			units.remove(0);
		}
		
		return stabVulVal;
	}
	
	
	// This power's vulnerability to retard bots retaliation
	public int stabLoss(Power retardbot){
		int stabVulVal = 0;
		
		List<Unit> units = retardbot.getUnitList();
		
		while (!units.isEmpty()){
			List<Node> nodes = units.get(0).getLocation().getAdjacentNodesList();
			int unitStabability = 0, noOfAdjNodes = 0;
			Node multipleCoasts = null;
			
			while (!nodes.isEmpty()){
				if (nodes.get(0).getName().substring(4).compareTo("CS") == 0 && multipleCoasts != null){
					if (nodes.get(0).getDestValue(this.name) > multipleCoasts.getDestValue(this.name)){
						unitStabability += - multipleCoasts.getDestValue(this.name) + nodes.get(0).getDestValue(this.name);
					}
				}
				else {
					boolean maybeStabbed = false;
					if (nodes.get(0).getProvince().isSC()){
						if (nodes.get(0).getProvince().ownedBy().equals(this.name)){
							maybeStabbed = true;
						}
					}
					if (nodes.get(0).getProvince().isOccupied()){
						if (nodes.get(0).getUnit().getController().getName().equals(this.name)){
							maybeStabbed = true;
						}
					}
					if (maybeStabbed){
						unitStabability += nodes.get(0).getDestValue(this.name);
						noOfAdjNodes++;
						if (nodes.get(0).getName().substring(4).equals("CS")){
							multipleCoasts = nodes.get(0);
						}
					}
				}
				nodes.remove(0);
			}
			
			stabVulVal += unitStabability / noOfAdjNodes;
			units.remove(0);
		}
		
		return stabVulVal;		
	}
	
	public void setAcceptedPeace(){
		acceptedPeace = true;
	}
	
	public boolean acceptedPeace(){
		return acceptedPeace;
	}
	
	public void setOut(){
		isOut = true;
	}
	
	public boolean isOut(){
		return isOut;
	}
	
	public boolean wantToStab(){
		return backstab;
	}

	public void setBackstab(){
		acceptedPeace = false; 
		backstab = true;
	}
	
	public void unSetBackstab(){
		backstab = false;
	}
// SHOULDNT NEED THIS METHOD, JUST USING IT FOR DEBUGGING
	public int getBalance(){
		return balance; 
	}
	
	
}
