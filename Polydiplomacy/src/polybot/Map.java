package polybot;

import java.util.*;



public class Map {
	
	
	
	// The key for the collection of nodes will always be six letters
	// In the case of army nodes they will be called <PROVICE><"AMY">
	// In the case of fleet nodes on seas or single coastal province they will be called <PROVINCE><"FLT">
	// In the case of a fleet on coast nodes of bicoastal provinces they will be called <PROVINCE><COAST>
	
	Hashtable<String, Province> listOfProvinces= new Hashtable<String, Province>();
	Hashtable<String, Power> listOfPowers = new Hashtable<String, Power>(); 
	Hashtable<String, Node> listOfNodes = new Hashtable<String, Node>();
	private boolean firstTurn = true;
	
	private List<String[]> sentProposals = new ArrayList<String[]>();
	private List<Unit> movesWeAgreed = new ArrayList<Unit>();
    private List<Unit> listOfNegUnits = new ArrayList<Unit>();
	
	private int season; 
	private final int SPR = 0; 
	private final int SUM = 1; 
	private final int FAL = 2; 
	private final int AUT = 3; 
	private final int WIN = 4;
	
	private final int PCE = 1;
	private final int ALY = 2;
	private final int DMZ = 3;
	private final int XDO = 4;
	private final int DRW = 5;
	private final int SLO = 6;
	
	public static final int m_prox_spr_att_weight = 700;	//Importance of attacking centres we don't own in spring
	public static final int m_prox_spr_def_weight = 200;	//300 //Importance of defending our centres in spring 300
	public static final int m_prox_fall_att_weight = 600;	//Importance of attacking centres we don't own in fall
	public static final int m_prox_fall_def_weight = 300;	//400 //Importance of defending our centres in fall    400 orig
	public static final int m_spr_str_weight = 1000;		//Importance of our attack strength on a province in spring
	public static final int m_spr_comp_weight = 1000;		//Importance of our lack of competition for the province in spring
	public static final int[] m_spr_prox_weight = {100, 1000, 30, 10, 6, 5, 4, 3, 2, 1}; //Importance of proximity[n] in spring
	public static final int m_fall_str_weight = 1000;		//Importance of our attack strength on a province in fall
	public static final int m_fall_comp_weight = 1000;	//Importance of our lack of competition for the province in fall
	public static final int[] m_fall_prox_weight = {1000, 100, 30, 10, 6, 5, 4, 3, 2, 1}; //Importance of proximity[n] in fall
	public static final int PROXIMITY_DEPTH = 10;			//How deep we should take other provinces into account when evaluating a province.
	public static final int m_rem_def_weight = 1000;		//Importance of removing in provinces we don't need to defend
	public static final int m_build_def_weight = 1000;	//Importance of building in provinces we need to defend
	public static final int[] m_rem_prox_weight = {1000, 100, 30, 10, 6, 5, 4, 3, 2, 1}; //Importance of proximity[n] when removing
	public static final int[] m_build_prox_weight = {1000, 100, 30, 10, 6, 5, 4, 3, 2, 1}; //Importance of proximity[n] when building
	public static final int m_alternative_difference_modifier = 500; //500
	public static final int m_play_alternative = 30; //50
    
	public static final double PCE_THRESHOLD = 0.4;
	
	private final int ANGRY = 2;
	private final int NEUTRAL = 1;
	
    List<Unit> globalSupUnit;
    private int waives = 0;

	public Map(String[] mdf){
		processMDF(takeRight(mdf, 1));
	}
	
	public Power getPower(String powerString){
		return (Power) listOfPowers.get(powerString);
	}
	
	public void updateSCO(String[] sco){
		// This method updates which supply centres are owned each turn. 
		killAllSCs();
		
		
		sco = takeRight(sco, 1);
		int closingBracket;
		String[] singlePower;
		
		while (sco.length > 0){
			closingBracket = findClose(sco);
			singlePower = takeLeft(sco, closingBracket);
			Power pandaPower = (Power) listOfPowers.get(singlePower[1]);
			int i; 
			
			for(i=2;i < singlePower.length-1; i++){
				Province owned = (Province) listOfProvinces.get(singlePower[i]);
				pandaPower.addSC(owned);
				owned.capture(pandaPower);
				if (firstTurn){
					pandaPower.addHome(owned);
				}
			}
			pandaPower.calcPower();
			sco = takeRight(sco, closingBracket);						
		}
		firstTurn = false;
	}

	private void killAllSCs() {
		for (Iterator i = listOfPowers.values().iterator(); i.hasNext(); ) {
			  Power current = (Power) i.next();
			  current.resetSCs();
			}
	
		for (Iterator i = listOfProvinces.values().iterator(); i.hasNext(); ) {
			  Province currentProvince = (Province) i.next();
			  currentProvince.capture((Power)listOfPowers.get("UNO"));
			}
		
	}
	
	private void handleUnit(String[] chunk) {
		Power currentPower = listOfPowers.get(chunk[1]);
		
		if(chunk[2].equals("AMY")) {
			Node[] tempNodeArray = (listOfProvinces.get(chunk[3])).getNodeArray();
			Unit currentUnit = new Army(currentPower, tempNodeArray[0]);
			tempNodeArray[0].occupy(currentUnit);
			currentPower.addUnit(currentUnit);

			if (chunk[4].equals("MRT")){
				currentUnit.setMRT(updatePossibleRetreats(chunk, currentUnit));
			}
		} else {
			
			if(!chunk[3].equals("(")) {
//				NO COASTS 
				Node[] tempNodeArray = (listOfProvinces.get(chunk[3])).getNodeArray();
				
 
				Unit currentUnit = new Fleet(currentPower, tempNodeArray[1]);
				tempNodeArray[1].occupy(currentUnit);
				currentPower.addUnit(currentUnit);
				if (chunk[4].equals("MRT")){
					currentUnit.setMRT(updatePossibleRetreats(chunk, currentUnit));
				}
			} else {
				// PANIC - COASTS, fucksocks
				Node currentNode = (Node) listOfNodes.get(chunk[4] + chunk[5]);
				Unit currentUnit = new Fleet(currentPower, currentNode);
				
				currentNode.occupy(currentUnit);
				currentPower.addUnit(currentUnit);
				if (chunk[7].equals("MRT")){
					currentUnit.setMRT(updatePossibleRetreats(chunk, currentUnit));
				}
			}
		}
		
	}

	private List<Node> updatePossibleRetreats(String[] chunk, Unit currentUnit) {
		// TODO Auto-generated method stub
		
		List<Node> retreatNodes = new ArrayList<Node>();
				
		if(currentUnit.getUnitType().equals("AMY")){
			chunk = takeRight(chunk, 5);
			int closingBracket = findClose(chunk);
			if(closingBracket!=1){
				// THIS MEANS THE UNIT HAS PLACES TO RETREAT TO AND DOESN'T HAVE TO DISBAND
				for(int i =1; i < closingBracket-1; i++){
//					System.out.println("Province is called: " + chunk[i]);
					retreatNodes.add(listOfProvinces.get(chunk[i]).getNodeArray()[0]);
				}
			}
		} else {
			if(chunk[3].equals("(")) {
				// THIS UNIT IS ON A BI COASTAL PROVINCE
				chunk = takeRight(chunk, 8);
				int closingBracket = findClose(chunk);
				if(closingBracket!=1){
					// THIS MEANS THE UNIT HAS PLACES TO RETREAT TO AND DOESN'T HAVE TO DISBAND
					for(int i =1; i < closingBracket-1; i++){
						if(chunk[i].equals("(")){
							// THE UNIT CAN RETREAT TO A COAST HERE
							retreatNodes.add(listOfNodes.get(chunk[i+1] + chunk[i+2]));
							i+=3;
						} else retreatNodes.add(listOfProvinces.get(chunk[i]).getNodeArray()[1]);
					}
				}
			} else {
				// THIS UNIT HAS TO RETREAT, BUT IT IS NOT ON A BI COASTAL PROVINCE. AND IT IS A FLEET.
				chunk = takeRight(chunk, 5);
				int closingBracket = findClose(chunk);
				if(closingBracket!=1){
					// THIS MEANS THE UNIT HAS PLACES TO RETREAT TO AND DOESN'T HAVE TO DISBAND
					for(int i =1; i < closingBracket-1; i++){
						if(chunk[i].equals("(")){
							// THE UNIT CAN RETREAT TO A COAST HERE
							retreatNodes.add(listOfNodes.get(chunk[i+1] + chunk[i+2]));
							i+=3;
						} else retreatNodes.add(listOfProvinces.get(chunk[i]).getNodeArray()[1]);
					}
				}
			}
		}
		
		
		return retreatNodes;
	}

	private void createPowers(String[] powers){
		
		for (int i = 1; !powers[i].equals(")"); i++ ){
			listOfPowers.put(powers[i], new Power(powers[i]));
		}
		
		listOfPowers.put("UNO", new Power("UNO"));
	}
	
	private void processMDF(String[] tokens) {
		
		int powerEnds = findClose(tokens);
		
		String[] powers = takeLeft(tokens, powerEnds);
		String[] provAndAdj = takeRight(tokens, powerEnds);
		
		int provinceEnds = findClose(provAndAdj);
		
		String[] provinces = takeLeft(provAndAdj, provinceEnds);
		String[] adjacencies = takeRight(provAndAdj, provinceEnds);
			
		createPowers(powers);
		createProvinces(provinces);
		createNodes(adjacencies);
		processAdjacencies(adjacencies);
		
	}
	
	private void processAdjacencies(String[] adjacencies){
		
		int provinceClosingBracket;
		
		adjacencies = takeRight(adjacencies, 1);
		provinceClosingBracket = findClose(adjacencies);
		
		while (provinceClosingBracket != -1){
			
			processProvinceAdjacencies(takeLeft(adjacencies, provinceClosingBracket));
			
			adjacencies = takeRight(adjacencies, provinceClosingBracket);
			provinceClosingBracket = findClose(adjacencies);
		}
	
		
	}
	
	private void processProvinceAdjacencies(String[] provinceAdjacencies) {
		
		Province province = listOfProvinces.get(provinceAdjacencies[1]);
//		System.out.println();
//		System.out.println("THE PROVINCE IN QUESTION IS: " + province.getName());
		provinceAdjacencies = takeRight(provinceAdjacencies, 2);
		int movementClosingBracket	= findClose(provinceAdjacencies);
		
		while (movementClosingBracket != -1){
			
			Node workingNode; 
			
//			System.out.println();
			
			int i = 2; 
			if (provinceAdjacencies[1].equals("AMY")){
				workingNode = listOfNodes.get(province.getName() + "AMY");
			} else if (provinceAdjacencies[1].equals("FLT")){
				workingNode = listOfNodes.get(province.getName() + "FLT");
			} else {
				workingNode = listOfNodes.get(province.getName() + provinceAdjacencies[3]);
				i = 5;  
			}
//			System.out.println("i = " + i + ", and movementClosingBr =  " + movementClosingBracket);
//			System.out.println(workingNode.getName());
			for(; i < movementClosingBracket-1; i++){
				if (provinceAdjacencies[i].equals("(")){
					workingNode.addAdjacentNode((Node) listOfNodes.get(provinceAdjacencies[i+1] + provinceAdjacencies[i+2]));
//					System.out.println(provinceAdjacencies[i+1] + provinceAdjacencies[i+2]);
					i += 3;
				} else {
					String nodeName = provinceAdjacencies[1].equals("AMY") ? provinceAdjacencies[i] + "AMY" : provinceAdjacencies[i] + "FLT";
//					System.out.println(nodeName);
					workingNode.addAdjacentNode((Node) listOfNodes.get(nodeName));
				}
			}
			 
			provinceAdjacencies = takeRight(provinceAdjacencies, movementClosingBracket);		 
			movementClosingBracket = findClose(provinceAdjacencies);
			
		}
		
	}

	private void createProvinces(String[] provinces){
		// AND CREATE SCs
		provinces = takeRight(provinces,1);
		int closingBracket = findClose(provinces);

		String[] supplyCentres = takeLeft(provinces,closingBracket);
		String[] nonSupplyCentres = takeRight(provinces, closingBracket);
		
		createSupplyCentres(supplyCentres);
		createNonSupplyCentres(nonSupplyCentres);
		
	}
	
	private void createNonSupplyCentres(String[] nonSupplyCentres) {
		// TODO Auto-generated method stub
		
		int closingBracket = findClose(nonSupplyCentres);
		Province newProvince;
		
		
		// I ADDED A MINUS 1 HERE BECAUSE I'M AWESOME ---- ADAM 
		for (int i = 1; i < closingBracket-1; i++){
			newProvince = new Province(nonSupplyCentres[i]);
			listOfProvinces.put(nonSupplyCentres[i], newProvince);
		}
		
	}

	private void createSupplyCentres(String[] supplyCentres) {
		// TODO Auto-generated method stub
		int closingBracket;
		
		supplyCentres = takeRight(supplyCentres, 1);
		closingBracket = findClose(supplyCentres);
		String[] singlePower;
		
		while(closingBracket != -1){
			singlePower = takeLeft(supplyCentres, closingBracket);
			Power tempPower = (Power) listOfPowers.get(singlePower[1]);
	
			for(int i=2;i < singlePower.length-1; i++){
				// Province owned = (Province) listOfProvinces.get(singlePower[i]);
				Province owned = new Province(singlePower[i]);
				tempPower.addSC(owned);
				owned.capture(tempPower);
				owned.setSupplyCentre();
				listOfProvinces.put(singlePower[i], owned);
			}
			supplyCentres = takeRight(supplyCentres, closingBracket);
			closingBracket = findClose(supplyCentres);
		}
		
	}

	private void createNodes(String[] adjacencies){
		
		
		String[] inputRemaining = takeRight(adjacencies, 1);
		int closingBracket = findClose(inputRemaining);
		
		while(closingBracket != -1 ){
//			if(inputRemaining[1].equals(")")) System.out.println("-----------------------------------------------------------SUPER ERROR_________________________________________________");
			Province workingProvince = (Province) listOfProvinces.get(inputRemaining[1]);
			
			String[] twoCoasts = new String[2];
			twoCoasts = hasCoasts(inputRemaining);

			if (twoCoasts[0] == null) {
				if(isCoastal(inputRemaining)){
					String node0str = inputRemaining[1] + "AMY";
					String node1str = inputRemaining[1] + "FLT";
					
					Node node0 = new Node(node0str);
	 				Node node1 = new Node(node1str);
	 				
	 				listOfNodes.put(node0str, node0);
	 				listOfNodes.put(node1str, node1);
	 				
	 				node0.setProvince(workingProvince);
	 				node1.setProvince(workingProvince);
	 				
	 				workingProvince.setNode0(node0);
					workingProvince.setNode1(node1);
					
				} else {
					if(inputRemaining[3].equals("AMY")){
						Node node0 = new Node(inputRemaining[1] + "AMY");
						listOfNodes.put(inputRemaining[1] + "AMY", node0);
						node0.setProvince(workingProvince);
						workingProvince.setNode0(node0);
					} else {
						Node node1 = new Node(inputRemaining[1] + "FLT");
						listOfNodes.put(inputRemaining[1] + "FLT", node1);
						node1.setProvince(workingProvince);
						workingProvince.setNode1(node1);
					}
				}
				
				
			} else {
				// In this case there is an army and 2 fleet nodes to be added. 
				String node0str = inputRemaining[1] + "AMY";
				String node1str = inputRemaining[1] + twoCoasts[0];
 				String node2str = inputRemaining[1] + twoCoasts[1];
				
 				Node node0 = new Node(node0str);
 				Node node1 = new Node(node1str);
 				Node node2 = new Node(node2str);

 				listOfNodes.put(node0str, node0);
 				listOfNodes.put(node1str, node1);
 				listOfNodes.put(node2str, node2);
 				
 				node0.setProvince(workingProvince);
 				node1.setProvince(workingProvince);
 				node2.setProvince(workingProvince);
 				
 				workingProvince.setNode0(node0);
				workingProvince.setNode1(node1);
				workingProvince.setNode2(node2);
				
			}

			inputRemaining = takeRight(inputRemaining,closingBracket);
			closingBracket = findClose(inputRemaining);
		
		}
		
		
	}

	private boolean isCoastal(String[] inputRemaining){
		int closingBracket = findClose(inputRemaining);
		String[] testProvince = takeLeft(inputRemaining, closingBracket);
		int i = 0;
				
		for (i=0; i< testProvince.length-1; i++){
			if(testProvince[i].equals(")") && testProvince[i+1].equals("(")){
				return true;
			}
		}
		
		return false;
	}
	
	private String[] hasCoasts(String[] inputRemaining) {
		int closingBracket = findClose(inputRemaining);
		String[] testProvince = takeLeft(inputRemaining, closingBracket);
		int i = 0;
		String[] coasts = new String[2];
		
		for (i=0; i< testProvince.length-2; i++){
			if(testProvince[i].equals("(") && testProvince[i+1].equals("(") && testProvince[i+2].equals("FLT")){
				if (coasts[0] == null) coasts[0] = testProvince[i+3]; 
				else coasts[1] = testProvince[i+3];
			}
		}
		
		
		return coasts;
	}

	private void printMessage(String[] message){
		for(int i =0; i<message.length;i++){
			System.out.print(message[i] + " ");
		}
		System.out.println();
	}
	
	private String[] takeLeft(String[] tokens, int end) {
		int i = 0;
		String[] left = new String[end];
		for(i=0; i< end; i++){
			left[i] = tokens[i];
		}
		
		return left;
		
		
	}
	
	// TAKE RIGHT INCLUDES THE STRING AT THE TOKEN YOU PROVIDE
	private String[] takeRight(String[] tokens, int start){
		int length = tokens.length;
		String[] right = new String[length-start];
		
		for(int i=0; i < (length - start); i++){
			right[i] = tokens[start + i];
		}
		
		return right;
	}
		
	private int findClose(String[] tokens){
		int bracketCount = 0; 
		int i = 0;
		
		if (!tokens[0].equals("(")){ return -1; }		
		
		for(i = 0; i<tokens.length; i++){
			
			if (tokens[i].equals( "(" ))  bracketCount++;
			if (tokens[i].equals( ")" )) bracketCount--;
			if (bracketCount == 0) return (i+1);
		}
		
		return -1;
	}

	private void killAllUnits() {
		for (Iterator i = listOfNodes.values().iterator(); i.hasNext(); ) {
			  Node current = (Node) i.next();
			  current.unoccupy();
			}
		for (Iterator i = listOfPowers.values().iterator(); i.hasNext(); ) {
			  Power current = (Power) i.next();
			  current.killAllUnits();
		}
		
	}
	
	public void updateUnits(String[] message) {
		
		
		
		killAllUnits();
		
		message = takeRight(message, 5);
		
		int closingBracket;
		
		while(message.length > 0){
			closingBracket = findClose(message);
			handleUnit(takeLeft(message, closingBracket));
			message = takeRight(message, closingBracket);
		}
		
	}

	public void storeSeason(String curSeason){
		if (curSeason.compareTo("SPR") == 0) season = SPR;
		else if (curSeason.compareTo("SUM") == 0) season = SUM;
		else if (curSeason.compareTo("FAL") == 0) season = FAL;
		else if (curSeason.compareTo("AUT") == 0) season = AUT;
		else season = WIN;
	}
	
	public int getSeason(){
		return season;
	}
	
	public List<String[]> processNOW(Power me){
		if (season == SUM){
			// Spring Moves/Retreats
			calcFactors(me, m_prox_spr_att_weight, m_prox_spr_def_weight);
			initStrCompValues(me);
			calcDestValue(me, m_spr_prox_weight, m_spr_str_weight, m_spr_comp_weight);
		}
		else if (season == AUT){
			// Fall Moves/Retreats
			calcFactors(me, m_prox_fall_att_weight, m_prox_fall_def_weight);
			initStrCompValues(me);
			calcDestValue(me, m_fall_prox_weight, m_fall_str_weight, m_fall_comp_weight);
		}
		else{
			// Winter Adjustments
			calcFactors(me, m_prox_spr_att_weight, m_prox_spr_def_weight);
			initStrCompValues(me);
			if (me.getArmySize() + me.getFleetSize() > me.countSCs()){
				// Disbanding...
				calcWinDestVal(me, m_rem_prox_weight, m_rem_def_weight);
			}
			else{
				// Building...
				calcWinDestVal(me, m_build_prox_weight, m_build_def_weight);
			}
		}
		
		if (season == SUM || season == AUT){
			genRetreatOrders(me);
		}
		else{
			if (me.getArmySize() + me.getFleetSize() > me.countSCs()){
				genRemoveOrders(me, me.getArmySize() + me.getFleetSize() - me.countSCs());
			}
			else{
				genBuildOrders(me, me.countSCs() - me.getArmySize() - me.getFleetSize());
			}
		}
		
		return submitOrders(me);
	}
	
	public void processFactors(Power me){
		if (season == SPR){
			// Spring Moves/Retreats
			calcFactors(me, m_prox_spr_att_weight, m_prox_spr_def_weight);
		}
		else {
			// Fall Moves/Retreats
			calcFactors(me, m_prox_fall_att_weight, m_prox_fall_def_weight);
		}
	}
	
	
	//Works out destination value for each province and generates orders. Takes into account strength value, competition value.
	//Goes through each province and if..
	//      -it's a supply centre and owned by us, it works out a defence value
	//		-it's a supply centre and not owned by us then it works out the attack value (the amount of units of the owning power)
	//		-else it's zero
	//Each province has a proximity_map array of 10 values. prox_map[0] = attack_value*att_weight + defence_value*def_weight)
	//It then spreads this number around to all surrounding prox_map such that for each province, it has a
	// prox_map[n] = sum of adjacent prox_this[n-1]/5
	//It then calculates the strength value and competition value for each province. This is...
	//		Strength value: Amount of units that we own that can move into this province.
	//		Competition value: the most enemy units from a single power that can move into this province.
	// All of these factors are taken into account in the final destination value for each province.
	// If you were to add alliances, then you can for instance, change the strength value or defence value such that we take
	// alliances into account. you could also change the competition value so that it doesn't include the alliances.
	public void calcFactors( Power me, int prox_att_weight, int prox_def_weight){
		Enumeration provinceIterator = this.listOfProvinces.elements();
	    Province province;
		
	    //Reset's attack and defence values for all provinces.
	    while (provinceIterator.hasMoreElements ()){
	        province = (Province) provinceIterator.nextElement();
	        province.resetAttVal();
	        province.resetDefVal();
		}
	    
	    //Go through each province
	    provinceIterator = this.listOfProvinces.elements();
	    while (provinceIterator.hasMoreElements ()){
			province = (Province) provinceIterator.nextElement();
			// Reset variable being moved to and being moved set by previous turns
			province.resetBeingMovedTo();
			//If it is a supply centre
			if (province.isSC()){
				//If it is owned by us then work out defence value (size of largest power adjacent to us)
				if (province.ownedBy() == me){
					province.calcDefVal(me);
				}
//				If it is not owned by us then work out attack value (size of the power who owns the province)
				else{
					province.setAttVal(province.ownedBy().getPowerSize());
				}
			}
		}
		
	    //Go through each province
	    provinceIterator = this.listOfProvinces.elements();
	    while (provinceIterator.hasMoreElements()){
			province = (Province) provinceIterator.nextElement();
			List<Node> nodes = province.getNodeList();
			//Go through each node in province
			while (!nodes.isEmpty()){
				//Set prox_map[0] to province.attackval*weight + province.defval*weight
				nodes.get(0).setProximityMap(0,province.getAttVal()*prox_att_weight + province.getDefVal() *prox_def_weight);
				for (int proxCount = 1; proxCount < PROXIMITY_DEPTH; proxCount++){
					//Reset rest of prox_map to 0 for node
					nodes.get(0).setProximityMap(proxCount, 0);
				}
				nodes.remove(0);
			}
	    }
	    
	    //Blurring algorithm
	    //go through each proximity depth
	    //go through each province
	    //go through each adjacent province and take proximity[n-1] from it, adding them
	    //  to form current province's proximity[n]
	    //then proximity[n]/5
	    for (int proxCount = 1; proxCount < PROXIMITY_DEPTH; proxCount++){
		    //Go through each province
	    	provinceIterator = this.listOfProvinces.elements();
		    while (provinceIterator.hasMoreElements()){
		    	province = (Province) provinceIterator.nextElement();	
				List<Node> nodes = province.getNodeList();
				//Go through each node
				while (!nodes.isEmpty()){
					nodes.get(0).setProximityMap(proxCount, nodes.get(0).getProximity(proxCount-1));
					List<Node> adjacentNodes = nodes.get(0).getAdjacentNodesList();
					Node multipleCoasts = null;
					//Go through each adjacent node
					while (!adjacentNodes.isEmpty()){
						//If multiple coast and have been through that province before (ie SPA NCS after SCS)
						if (adjacentNodes.get(0).getName().substring(4).compareTo("CS") == 0 && multipleCoasts != null){
							// If the new node has a greater prox-1 then the other node then replace
							if (adjacentNodes.get(0).getProximity(proxCount-1) > multipleCoasts.getProximity(proxCount-1)){
								nodes.get(0).setProximityMap(proxCount, nodes.get(0).getProximity(proxCount) - multipleCoasts.getProximity(proxCount-1) + adjacentNodes.get(0).getProximity(proxCount-1));								
							}
						}
						else{
							//Add to adjacent node prox-1 to proxmap
							nodes.get(0).setProximityMap(proxCount, nodes.get(0).getProximity(proxCount) + adjacentNodes.get(0).getProximity(proxCount-1));
							//If a coast then remember coast for next time
							if (adjacentNodes.get(0).getName().substring(4).compareTo("CS") == 0){
								multipleCoasts = adjacentNodes.get(0);
							}							
						}
						adjacentNodes.remove(0);
					}
					nodes.get(0).setProximityMap(proxCount, nodes.get(0).getProximity(proxCount)/5);
					//System.out.println(nodes.get(0).getName() + "'s proxmap["+proxCount+": "+ nodes.get(0).getProximity(proxCount));
					nodes.remove(0);
				}
		    }
	    }
	}
	
	
	public void initStrCompValues(Power me){
	    //Calculate number of units each power has next to each province
	    Enumeration provinceIterator = this.listOfProvinces.elements();
	    while (provinceIterator.hasMoreElements()){
			Province province = (Province) provinceIterator.nextElement();
			province.setStrengthValue(0);
			province.setCompValue(0);
	        
			//Store a temp hash table of every power and the amount of units they have adjacent to this province
		    Hashtable<String,Integer> adjUnitCount = new Hashtable<String,Integer>();
		    Enumeration powerIterator = this.listOfPowers.elements();			
		    while (powerIterator.hasMoreElements()){
				Power power = (Power) powerIterator.nextElement();
				adjUnitCount.put(power.getName(), 0);
		    }
		    
		    List<Node> adjProvinces = province.getAdjacentProvinces();
		    
		    //DELETE ME? Adds current nodes to adj list
		    List<Node> nodes = province.getNodeList();
		    while (!nodes.isEmpty()){
		    	adjProvinces.add(nodes.get(0));
		    	nodes.remove(0);
		    }
		    
		    while (!adjProvinces.isEmpty()){
				if (adjProvinces.get(0).isOccupied()){
					adjUnitCount.put(adjProvinces.get(0).getUnit().getController().getName(),adjUnitCount.get(adjProvinces.get(0).getUnit().getController().getName()) + 1);
				}
				adjProvinces.remove(0);
			}

		    powerIterator = this.listOfPowers.elements();			
		    while (powerIterator.hasMoreElements()){
				Power power = (Power) powerIterator.nextElement();
				if (power == me){
					province.setStrengthValue(adjUnitCount.get(me.getName()));
				}
				else if (adjUnitCount.get(power.getName()) > province.getCompValue()){
					province.setCompValue(adjUnitCount.get(power.getName()));
				}
		    }
		    province.setAdjUnitCount(adjUnitCount);
	    }   
	}
	
	public void calcDest(Power me){
		if (season == SPR){
			// Spring Moves/Retreats
			calcDestValue(me, m_spr_prox_weight, m_spr_str_weight, m_spr_comp_weight);
		}
		else {
			// Fall Moves/Retreats
			calcDestValue(me, m_fall_prox_weight, m_fall_str_weight, m_fall_comp_weight);
		}
	}
	
	// Works out final destination value for each province. This is worked out by adding all the values in the proximity map for
	// that province, and then adds the strength value, and minuses the competition value (meaning we're more likely to move into
	// it if we have strong forces next to it, and less likely if a power has large forces that can move into it.
	private void calcDestValue(Power me, int[] proxWeight, int strWeight, int compWeight){
		//Go through each node
		Enumeration nodeIterator = this.listOfNodes.elements();
		while (nodeIterator.hasMoreElements()){
			Node node = (Node) nodeIterator.nextElement();
			int destWeight = 0;
			//Add up all ints in prox_map*weight
			for (int proxCount = 0; proxCount < PROXIMITY_DEPTH; proxCount++){
				destWeight += node.getProximity(proxCount)*proxWeight[proxCount];
			}
			//Add a strength value & remove a comp value
			destWeight += strWeight * node.getProvince().getStrengthValue();
			destWeight -= compWeight * node.getProvince().getCompValue();

			if (node.isOccupied()){
				if(node.getUnit().getController()!=me) destWeight *= (node.getUnit().getController().acceptedPeace()) ? Province.PEACE_COEFFICIENT : 1;
			}

			
			node.setDestValue(me.getName(), (destWeight == 0) ? 1 : destWeight);
		}
	}
	
	public void calcWinDestVal(Power me, int[] proxWeight, int defWeight){
		//Go through eacg bide
		Enumeration nodeIterator = this.listOfNodes.elements();
		while (nodeIterator.hasMoreElements()){
			Node node = (Node) nodeIterator.nextElement();
			int destWeight = 0;
			//Add up all ints in prox_map*weight
			for (int proxCount = 0; proxCount < PROXIMITY_DEPTH; proxCount++){
				destWeight += node.getProximity(proxCount)*proxWeight[proxCount];
			}
			//Add a def value
			destWeight += defWeight * node.getProvince().getDefVal();
			node.setDestValue(me.getName(), (destWeight == 0) ? 1 : destWeight);
		}
	}
		
	public void genMoveOrders(Power me){
		List<Unit> units = genRandomUnitList(me);
		boolean selectionIsOK;
		boolean orderUnitToMove;
		List<Node> destNodes;
		
		for (Unit n : units) {
			n.makeOrder(this);
		}
		
		/*//Go through each unit (which is in a random order)
		while(!units.isEmpty()){
			Unit currentUnit = units.get(0);

			if(movesWeAgreed.contains(currentUnit)) {
				units.remove(0);
			}
			else {
				
				//Get list of possible nodes which it can move to, including it's current location and sort by destval
				destNodes = currentUnit.getLocation().getAdjacentNodesList();
				destNodes.add(currentUnit.getLocation());
				destNodes = sortNodeListByDest(me, destNodes);
				
				
				do{
					Node currentNode = destNodes.get(0);
					boolean tryNextNode = true;
					int provCount = 1, nextNodeChance;
					while (tryNextNode){
						if (provCount < destNodes.size()){
							Node nextNode = destNodes.get(provCount++);
							if (currentNode.getDestValue(me.getName()) == 0){
								nextNodeChance = 0;
							}
							else{
								nextNodeChance = ((currentNode.getDestValue(me.getName()) - nextNode.getDestValue(me.getName())) * m_alternative_difference_modifier) / currentNode.getDestValue(me.getName());
							}
							if (randNo(100) < m_play_alternative && randNo(100) >= nextNodeChance){
								currentNode = nextNode;
							}
							else{
								tryNextNode = false;
							}
						}
						else{
							tryNextNode = false;
						}
					}
					
					
					selectionIsOK = true;
					orderUnitToMove = true;
					
					//if hold order then make it so
					if (currentUnit.getLocation().getName().compareTo(currentNode.getName()) == 0){
						currentUnit.setHold();
					}
					else{
						//if there one of our units in the provice already
						if(currentNode.getProvince().isOccupied()){
							if (currentNode.getProvince().getUnit().getController().getName().compareTo(me.getName()) == 0){
								// if that unit does not yet have an order
								if (currentNode.getProvince().getUnit().getOrderToken() == null){
									//find unit in the random unit list and move current unit to after it as we cant decide whether to
									// move there or not
									units.add(units.indexOf(currentNode.getProvince().getUnit())+1,currentUnit);
									orderUnitToMove = false;
								}
								// if it is not moving
								else if(currentNode.getProvince().getUnit().getOrderToken().compareTo("MTO") != 0 && currentNode.getProvince().getUnit().getOrderToken().compareTo("CTO") != 0){
									// if it needs supporting
									if (currentNode.getProvince().getCompValue() > 0){// ADAM CHANGED THIS - IF WE SUCK HARD, ITS CAUSE OF THIS
										//support it
										currentUnit.setSupportHold(currentNode.getProvince().getUnit());
										orderUnitToMove = false;
									}
									else{
										// Selection isn't ok, delete node from destination list so that it isn't selected again
										selectionIsOK = false;
										destNodes.remove(currentNode);
									}
								}
							}
						}
						// if there is a unit moving to this province already
						if (currentNode.getProvince().isBeingMovedTo()){
							//if it may need our support then support it
							if (currentNode.getProvince().getCompValue() > 0){
								currentUnit.setSupportMove(currentNode.getProvince().getUnitBeingMovedTo(), currentNode);
								orderUnitToMove = false;
							}
							else{
								//Selection is not ok, make sure this province isn't selected again
								selectionIsOK = false;
								destNodes.remove(currentNode);
							}
						}
						//if selection is ok and unit is ordered to move then make it so
						if (selectionIsOK && orderUnitToMove){
							currentUnit.setMove(currentNode);
						}
					}
				}while(!selectionIsOK);
				units.remove(0);
			}
		}
		checkForWastedHolds(me);*/
	}
	
	private void checkForWastedHolds(Power me){
		List<Unit> units = genRandomUnitList(me);
		while(!units.isEmpty()){
			Unit currentUnit = units.get(0);
			if(movesWeAgreed.contains(currentUnit)) {
				units.remove(0);
			}
			else {
			Node destination = null;
			Unit unitSupported = null;
			//If unit is ordered to hold
			if (currentUnit.getOrderToken().compareTo("HLD") == 0){
				//consider every province we can move to
				int maxDestValue = 0;
				List<Node> destNodes = currentUnit.getLocation().getAdjacentNodesList();
				destNodes = sortNodeListByDest(me, destNodes);

				while(!destNodes.isEmpty()){
					Node currentNodes = destNodes.get(0);
					//if a unit is moving there
					if (currentNodes.getProvince().isBeingMovedTo()){
						//if unit needs support
						if (currentNodes.getProvince().getCompValue() > 0){
							//if this is the best option so far then choose this one
							if (currentNodes.getDestValue(me.getName()) > maxDestValue){
								maxDestValue = currentNodes.getDestValue(me.getName());
								destination = currentNodes;
								unitSupported = currentNodes.getProvince().getUnitBeingMovedTo();
							}
						}
					}
					else{
						//if there is a unit holding there
						if (currentNodes.isOccupied()){
							if (currentNodes.getUnit().getController().getName().compareTo(me.getName()) == 0){
								if(currentNodes.getUnit().getOrderToken().compareTo("MTO")!=0 && currentNodes.getUnit().getOrderToken().compareTo("CTO")!=0 ){
									// if unit needs support
									if(currentNodes.getProvince().getCompValue() > 1){
										//if this is the best option so far then choose this one
										if(currentNodes.getDestValue(me.getName()) > maxDestValue){
											maxDestValue = currentNodes.getDestValue(me.getName());
											destination = currentNodes;
											unitSupported = currentNodes.getProvince().getUnit();				
										}
									}
								}
							}
						}
					}
					//if something worth supporting was found
					if (maxDestValue > 0){
						if (unitSupported.getOrderToken().compareTo("MTO") != 0 && unitSupported.getOrderToken().compareTo("CTO") != 0 ){
							currentUnit.setSupportHold(unitSupported);
						}
						else{
							currentUnit.setSupportMove(unitSupported, destination);
						}
					}
					destNodes.remove(0);
				}
			}
			units.remove(0);
		}
		}
	}
	
	
	private void genRetreatOrders(Power me){

		
		List<Unit> units = genRandomRETUnitList(me);
		for (Unit n : units) {
			n.retreat();
		}
	}
	
	
	private void genRemoveOrders(Power me, int removeCount){
		List<Unit> units = me.getUnitListSortedASC();
		for (int i = 0; i < removeCount; i++) {
			units.get(i).setRemoval();
		}
	}
	

	private synchronized void genBuildOrders(Power me, int buildCount){
		List<Node> homes = sortNodeListByDest(me, getBuildHomeList(me));
		System.out.println("homes.size() : " + homes.size());
		Node prevNode;
		if (homes.size() > 0) {
			prevNode = homes.get(0);
			for (Node n : homes) {
				if (!n.getProvince().isBuildingHere()) {
					if (!(Map.randNo(100) < Map.m_play_alternative 
							&&
							Map.randNo(100) >= ((prevNode.getDestValue(me.getName()) - n.getDestValue(me.getName()))
							* m_alternative_difference_modifier / n.getDestValue(me.getName())))
							&&
							!n.isOccupied()
							&& 
							!n.getProvince().isBeingMovedTo()) { 
						n.setBuildHere();
						
						if (buildCount-- == 0)
							break;
					}
				}
			}
		}
		if(buildCount>0){
			System.out.println("waive");
			waives = buildCount;
		}
	}			
	 

	private List<Unit> genRandomUnitList(Power me ){
		List<Unit> units = new ArrayList<Unit>(me.getArmySize() + me.getFleetSize());
		for (int x = 0; x < me.getArmySize(); x++){
			units.add(me.getArmies()[x]);
		}
		for (int x = me.getArmySize(); x < me.getArmySize() + me.getFleetSize(); x++){
			units.add(me.getFleets()[x - me.getArmySize()]);
		}
		Collections.shuffle(units);		
		return units;
	} 
	
	
	private List<Unit> genRandomRETUnitList(Power me ){
		List<Unit> units = new ArrayList<Unit>(me.getArmySize() + me.getFleetSize());
		for (int x = 0; x < me.getArmySize(); x++){
//			System.out.println("Army x = "+ me.getArmies()[x].getLocation().getName() + ", Must I retreat? "+ me.getArmies()[x].mustRetreat());
			if (me.getArmies()[x].mustRetreat()){ 
				units.add(me.getArmies()[x]);
			}
		}
//			for (int x = me.getArmySize(); x < me.getArmySize() + me.getFleetSize(); x++){
		for (int x = 0; x < me.getFleetSize(); x++){
			if (me.getFleets()[x].mustRetreat()) {
				units.add(me.getFleets()[x]);
			}
		}
		Collections.shuffle(units);		
		return units;
	}
	
	private List<Node> sortNodeListByDest(Power me, List<Node> nodes){

		
		for(int i = nodes.size(); --i>=0;){
			boolean flipped = false;
			for (int j = 0; j < i; j++){
				if (nodes.get(j).getDestValue(me.getName()) < nodes.get(j+1).getDestValue(me.getName())){
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
	
	public static Node getNodeRandom(List<Node> listNodes, Power me) {
		Node prevNode = listNodes.get(0);
		Node currentNode = prevNode;
		for (Node n : listNodes) {
			if (!(Map.randNo(100) > Map.m_play_alternative 
					&&
					Map.randNo(100) >= (((prevNode.getDestValue(me.getName()) - n.getDestValue(me.getName()))
					* Map.m_alternative_difference_modifier) / n.getDestValue(me.getName())))) { 
				currentNode = n;
				return currentNode;
			}
		}
		return currentNode;
	}
	
	private List<Node> getBuildHomeList(Power me){
		List<Province> orighomes = me.getHomes();
		List<Node> homes = new ArrayList<Node>(); 

		for(int i = 0; i < orighomes.size(); i++){
			if(!orighomes.get(i).isOccupied() && orighomes.get(i).ownedBy().equals(me)){
				for (int j = 0; j < orighomes.get(i).getNodeList().size(); j++){
					homes.add(orighomes.get(i).getNodeList().get(j));
				}
			}
		}
		return homes;
	}
	
	
	public static int randNo(int max){
		Random r = new Random();
		int no = r.nextInt(max);
		return no;
	}
	
	public List<String[]> submitOrders(Power me){
		List<String[]> listOfOrders = new ArrayList<String[]>();
		String [] order = {};
		Army[] myArmies = me.getArmies();
		int armyCount = me.getArmySize();
		Fleet[] myFleets = me.getFleets();
		int fleetCount = me.getFleetSize();
		List<Unit> supUnits = new ArrayList<Unit>(me.getArmySize() + me.getFleetSize());
		
		System.out.println("Size army : "+armyCount+" - Size Fleet : "+fleetCount);
		
		if (season == SPR || season == FAL){
			for (int i = 0; i < armyCount; i++){
				if (myArmies[i].getOrderToken().compareTo("SUP") == 0 ){
					supUnits.add(myArmies[i]);
				}
				else {
					order = new String[myArmies[i].getCompleteOrder().length + 1];
					order[0] = "SUB";
					for (int j = 0; j < myArmies[i].getCompleteOrder().length; j++){
						order[j+1] = myArmies[i].getCompleteOrder()[j];
					}
					listOfOrders.add(order);
//					sendMessage(msg);
				}
			}
			for (int i = 0; i < fleetCount; i++){
				if (myFleets[i].getOrderToken().compareTo("SUP") == 0 ){
					supUnits.add(myFleets[i]);
				}
				else {
					order = new String[myFleets[i].getCompleteOrder().length + 1];
					order[0] = "SUB";
					for (int j = 0; j < myFleets[i].getCompleteOrder().length; j++){
						order[j+1] = myFleets[i].getCompleteOrder()[j];
					}
					listOfOrders.add(order);
//						sendMessage(msg);
				}
			}
			for (int i = 0; i < supUnits.size(); i++){
				order = new String[supUnits.get(i).getCompleteOrder().length + 1];
				order[0] = "SUB";
				for (int j = 0; j < supUnits.get(i).getCompleteOrder().length; j++){
					order[j+1] = supUnits.get(i).getCompleteOrder()[j];
				}
				listOfOrders.add(order);
//					sendMessage(msg);
			}
			globalSupUnit = supUnits;
		}
		else if (season == SUM || season == AUT){
			for (int i = 0; i < armyCount; i++){
				if(myArmies[i].mustRetreat()){
					order = new String[myArmies[i].getCompleteOrder().length + 1];
					order[0] = "SUB";
					for (int j = 0; j < myArmies[i].getCompleteOrder().length; j++){
						order[j+1] = myArmies[i].getCompleteOrder()[j];
					}
					listOfOrders.add(order);
//						sendMessage(msg);
				}
			}
			
			for (int i = 0; i < fleetCount; i++){
				if(myFleets[i].mustRetreat()){
					order = new String[myFleets[i].getCompleteOrder().length + 1];
					order[0] = "SUB";
					for (int j = 0; j < myFleets[i].getCompleteOrder().length; j++){
						order[j+1] = myFleets[i].getCompleteOrder()[j];
					}
					listOfOrders.add(order);
//						sendMessage(msg);
				}
			}
		}
		else{//SENDING BUILD/REMOVE ORDERS THIS IS WINTER. 
			System.out.println("WINTER");
			if (me.getArmySize()+me.getFleetSize() > me.countSCs()){
				System.out.println("remove");
				for (int i = 0; i < armyCount; i++){
					if(myArmies[i].beingRemoved()){
						order = new String[myArmies[i].getCompleteOrder().length + 1];
						order[0] = "SUB";
						for (int j = 0; j < myArmies[i].getCompleteOrder().length; j++){
							order[j+1] = myArmies[i].getCompleteOrder()[j];
						}
						listOfOrders.add(order);
//							sendMessage(msg);
					}
				}
				
				for (int i = 0; i < fleetCount; i++){
					if(myFleets[i].beingRemoved()){
						order = new String[myFleets[i].getCompleteOrder().length + 1];
						order[0] = "SUB";
						for (int j = 0; j < myFleets[i].getCompleteOrder().length; j++){
							order[j+1] = myFleets[i].getCompleteOrder()[j];
						}
						listOfOrders.add(order);
//							sendMessage(msg);
					}
				}
			}
			else{
				System.out.println("ok");
				List<Node> homes = getBuildHomeList(me);
				for (int i = 0; i < homes.size(); i++){
//						System.out.println(homes.get(i).getName()  + " build here? " + homes.get(i).buildHere());
					if (homes.get(i).buildHere()){
//						System.out.println(homes.get(i).getName());
						order = (homes.get(i).getName().substring(4).compareTo("CS") != 0) ? new String[9] : new String[12];

						order[0] = "SUB";
						order[1] = "(";
						order[2] = "(";
						order[3] = me.getName();
						order[4] = (homes.get(i).getName().substring(3).compareTo("AMY") == 0) ? "AMY" : "FLT";
						
						int orderIndex = 5;
						//if one coast
						if (homes.get(i).getName().substring(4).compareTo("CS") != 0){
							order[orderIndex++] = homes.get(i).getProvince().getName();
						}
						//if two coasts then longer name
						else{
							order[orderIndex++] = "(";
							order[orderIndex++] = homes.get(i).getProvince().getName();
							order[orderIndex++] = homes.get(i).getName().substring(3);
							order[orderIndex++] = ")";						
						}

						order[orderIndex++] = ")";
						order[orderIndex++] = "BLD";
						order[orderIndex++] = ")";
												
						listOfOrders.add(order);
//							sendMessage(msg);
					}
				}
				while(waives>0){
					String[] waiveMsg = {"SUB","(",me.getName(),"WVE",")"};
					waives--;
					listOfOrders.add(waiveMsg);
//						sendMessage(waiveMsg);
				}
			}
		}
		return listOfOrders;
	}

	
	public void handleORD(List<String[]> listORD, Power me) {
				
		movesWeAgreed.clear();
		
		String[] message;
		Power power;  // this is a reference to the power which is making the move 
		
		for(int i =0; i < listORD.size(); i++){ // ------------FIRST ITERATION - CHECK WHICH ARE MOVING TOWARDS US, AND MARK THEM AS OUR ENEMIES!
			message = listORD.get(i);
			power = (Power) listOfPowers.get(message[7]);
			Unit tempUnit; 
			Province destination = null;
			String order; 
			
//			printMessage(message);
			
			if(!message[9].equals("(")){
				tempUnit = listOfProvinces.get(message[9]).getUnit();
				order = message[11];
				if(order.equals("MTO")){
					destination = (message[12].equals("(")) ? listOfProvinces.get(message[13]) : listOfProvinces.get(message[12]);
				}
			}
			else { // it starts on a bi coastal province. 
				tempUnit = listOfProvinces.get(message[10]).getUnit();
				order = message[14];
				if(order.equals("MTO")){
					destination = (message[15].equals("(")) ? listOfProvinces.get(message[16]) : listOfProvinces.get(message[15]);
				}
			}
			
			if(order.equals("HLD")) {
				tempUnit.setStance(NEUTRAL);
			}
			else if(order.equals("MTO")){
				if(destination.ownedBy() != me) tempUnit.setStance(NEUTRAL);
				
				if(destination.isSC() && destination.ownedBy() == me) {
					tempUnit.setStance(ANGRY);
					//power.increaseTimesAttackedUs();
//					System.out.println("Destination of MTO is: " + destination.getName());
				} else {
					if(destination.isOccupied() && destination.getUnit().getController() == me){
						tempUnit.setStance(ANGRY);
						//power.increaseTimesAttackedUs();
//						System.out.println("Destination of MTO is: " + destination.getName() + "Destinatioon of tempunit is: " + tempUnit.getLocation().getName());
					}
				}
			}
		} // ---------------------------------END OF FIRST ITERATION--------------------------------------------------
		
		
		for(int i =0; i < listORD.size(); i++){ // ------------SECOND ITERATION - DEAL WITH SUP MTO--------------------
			message = listORD.get(i);
			power = (Power) listOfPowers.get(message[7]);
			Unit tempUnit;
			Unit supUnit = null;
			String order; 
			String supOrder = null; 
			
			if(!message[9].equals("(")){
				tempUnit = listOfProvinces.get(message[9]).getUnit();
				order = message[11];
				if(order.equals("SUP")){
					supOrder = (message[15].equals("(")) ? message[20] : message[17];
					supUnit = (message[15].equals("(")) ? listOfProvinces.get(message[16]).getUnit() : listOfProvinces.get(message[15]).getUnit();
				}
			}
			else { // it starts on a bi coastal province. 
				tempUnit = listOfProvinces.get(message[10]).getUnit();
				order = message[14];
				if(order.equals("SUP")){
					supOrder = (message[18].equals("(")) ? message[23] : message[20];
					supUnit = (message[18].equals("(")) ? listOfProvinces.get(message[19]).getUnit() : listOfProvinces.get(message[18]).getUnit();
				}
			}
			
			if(order.equals("SUP") && supOrder.equals("MTO")) {
				tempUnit.setStance(supUnit.getStance());
			}
				
		}
		
	}

	public void handleChances(Power me) {
		Army[] tempArmies = new Army[40];
		Fleet[] tempFleets = new Fleet[40];
		int armyCount;
		int fleetCount;
		Node tempNode;
		Node[] adjNodes = new Node[40];
		int adjNodeCount; 
		Enumeration powerIterator = listOfPowers.elements();			
	    int i,j;
		boolean unitChanceUsed = false;  
	    
		while (powerIterator.hasMoreElements()){
			Power power = (Power) powerIterator.nextElement();
			if(power!=me){
				tempArmies = power.getArmies();
				armyCount = power.getArmySize();
				tempFleets = power.getFleets();
				fleetCount = power.getFleetSize();
				
				for(i=0;i<armyCount; i++){
					tempNode = tempArmies[i].getLocation();
					adjNodes = tempNode.getAdjacentNodes();
					adjNodeCount = tempNode.getAdjNodeCount();
					for(j=0; j<adjNodeCount && !unitChanceUsed; j++){
						if (adjNodes[j].getProvince().ownedBy() == me) {
							//power.increaseChancesToAttack();
							unitChanceUsed = true; 
						}
						else if(adjNodes[j].getUnit() !=null){
							if (adjNodes[j].getUnit().getController() == me){
								//power.increaseChancesToAttack();
								unitChanceUsed = true; 
							}
						}
					}
					unitChanceUsed = false; 
				}
		
				for(i=0;i<fleetCount;i++){
					tempNode = tempFleets[i].getLocation();
					adjNodes = tempNode.getAdjacentNodes();
					adjNodeCount = tempNode.getAdjNodeCount();
					for(j=0; j<adjNodeCount && !unitChanceUsed; j++){
						if (adjNodes[j].getProvince().ownedBy() == me){
							//power.increaseChancesToAttack();
							unitChanceUsed = true; 
						}
						else if(adjNodes[j].getUnit() !=null){
							if (adjNodes[j].getUnit().getController() == me){
								//power.increaseChancesToAttack();
								unitChanceUsed = true;
							}
						}
					}
					unitChanceUsed = false; 
				}
				
			}
			//power.flushChances();
	    }
		
	}

	
	
	

}
