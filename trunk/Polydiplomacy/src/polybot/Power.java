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
	
	private final double INITIAL_CREDIT_RATING = 0.8;
	private final double INTEREST = 1.08;
	private final int TURNS_TO_REMEMBER_HATRED = 20;
	private final int MIN_BALANCE = -80000000; // 8-15 million roughly equates to a move. depends on season 
	
	private int dealsBroken = 0;
	private int dealsKept = 0;
	private int totalDeals = 0;
	
	private int balance = 0 ; 
	private int moneyToGain; // this is set on the turn before they accept to do a move for us. The money they would expect to gain, if they do the deal for us 
							// which is discovered in the ORD phase. 
	
	private double enemyFactor;
	
	private boolean acceptedPeace = false;
	private boolean isOut = false;
	private boolean noPressBot = false; 
	private boolean backstab = false; 
	
	private List<Province> homes = new ArrayList<Province>();
	
	public void setMoneyToGain(int mtg){
		moneyToGain = mtg; 
	}
	
	public double getEnemyFactor(){
		return enemyFactor;
	}

	public boolean hasEnoughCredit(int cost){
		// 1 over the credit rating, multiplied by the cost. if this is <= the balance plus the min balance allocated.
		return (   (1/   ( (dealsKept + INITIAL_CREDIT_RATING) / (dealsBroken + dealsKept + 1) )  )*cost <= (balance+(-MIN_BALANCE)));
	}
	
	public void makeDeposit(int amount){
		//amount * credit rating. 
		balance += amount*( (dealsKept + INITIAL_CREDIT_RATING) / (dealsBroken + dealsKept + 1) );
	}
	
	public void makeWithdrawal(int amount){
		balance -= amount*INTEREST;
	}
	
	public void dealKept(){
		dealsKept++;
		dealsBroken = totalDeals - dealsKept; 
	}
	
	public void increaseDealsMade(){
		totalDeals++;
		dealsBroken = totalDeals - dealsKept;
	}
	
	public double getCreditRating(){
		return ( (dealsKept + INITIAL_CREDIT_RATING) / (dealsBroken + dealsKept + 1) );
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
		if (unit.getClass().getName().equals("dip.daide.us.Army")){
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
	
	public boolean isNoPress(){
		return noPressBot;
	}
	
	public void setNoPress(){
		noPressBot = true;
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
