package polybot;

public class Army extends Unit {

	public Army(Power controller, Node location) {
		super(controller, location);
		// TODO Auto-generated constructor stub
	}
        
        
        //ex: SUB ((ENG AMY LVP) HLD))
        public String[] getCompleteOrder(){
            String first = location.getName().substring(0,3);
            String firstOL = "";
            String firstOSU = "";
            String second = "";
            String secondOL= "";
            String secondOSU = "";
            
            if (orderLocation != null){
                firstOL = orderLocation.getName().substring(0,3);
                if (!(orderLocation.getName().substring(3).equals("FLT")|| orderLocation.getName().substring(3).equals("AMY"))){
                    secondOL = orderLocation.getName().substring(3);
                }
            }            
            
            if (orderSupportUnit != null){
                firstOSU = orderSupportUnit.location.getName().substring(0,3);
                if (!(orderSupportUnit.location.getName().substring(3).equals("FLT")|| orderSupportUnit.location.getName().substring(3).equals("AMY"))){
                	secondOSU = orderSupportUnit.location.getName().substring(3);
                }  
            }                    
                      
            
//            if (location.getName().length() > 3){
//                second = location.getName().substring(3);
//           }          
            
            if(order.equals("HLD")){
                if(second.equals("")){
                    String orders[] = {"(","(",controller.getName(),"AMY",first,")","HLD",")"};
                    return orders;
                } else {
                    String orders[] = {"(","(",controller.getName(),"AMY",first,second,")","HLD",")"};
                    return orders;
                }
            } else if (order.equals("MTO")){
                if(second.equals("")){
                    if(secondOL.equals("")){
                        String orders[] = {"(","(",controller.getName(),"AMY",first,")","MTO",firstOL,")"};
                        return orders;
                    } else {
                        String orders[] = {"(","(",controller.getName(),"AMY",first,")","MTO",firstOL,")"};
                        return orders;
                    }
                } else {
                    if(secondOL.equals("")){
                        String orders[] = {"(","(",controller.getName(),"AMY",first,")","MTO",firstOL,")"};
                        return orders;
                    } else {
                        String orders[] = {"(","(",controller.getName(),"AMY",first,")","MTO",firstOL,")"};
                        return orders;
                    }
                }
            } else if (order.equals("SUP")){
            	if(orderLocation != null){
                    String[] orderSUnit = orderSupportUnit.getCompleteOrder();
                    int len = orderSUnit.length;
                    String orders[];
                    int j;
                    if(second.equals("")){
                        if(orderSUnit[len-3].equals("MTO")){
                        	orders = new String[8 + len-2];
                        } else {
                        	orders = new String[5 + len-2];
                        }
                        j = 0;
                        orders[4]= first;
                    } else {
                        if(orderSUnit[len-3].equals("MTO")){
                        	orders = new String[9 + len-2];
                        } else {
                        	orders = new String[6 + len-2];
                        }                      
                        j = 1;
                        orders[4] = first;
                        orders[5] = second;
                    }
                    orders[0] = "(";
                    orders[1] = "(";
                    orders[2] = controller.getName();
                    orders[3] = "AMY";
                    orders[5+j] = ")";
                    orders[6+j] = "SUP";
                    if(orderSUnit[len-3].equals("MTO")){
                        for(int i = 1;i < len-1; i++){
                            orders[i+6+j] = orderSUnit[i];
                        }	
                    } else {
                        for(int i = 1;i < len-1; i++){
                        	if(i < len -5){
                        		orders[i+6+j] = orderSUnit[i];
                        	} else if (i == len -4){
                        		orders[i+5+j] = orderSUnit[i];
                        	} else if (i > len -2){
                        		orders[i+3+j] = orderSUnit[i];
                        	}
                        }
                    }
                    orders[orders.length - 1] = ")";
                    return orders;
                } else{
                    String unitType = orderSupportUnit.getUnitType();
                if(second.equals("")){
                    if(secondOSU.equals("")){
                        String orders[] = {"(","(",controller.getName(),"AMY",first,")","SUP","(",orderSupportUnit.controller.getName(),unitType,firstOSU,")",")"};
                        return orders;
                    } else {
                        String orders[] = {"(","(",controller.getName(),"AMY",first,")","SUP","(",orderSupportUnit.controller.getName(),unitType,"(",firstOSU,secondOSU,")",")",")"};
                        return orders;
                    }
                } else {
                    if(secondOSU.equals("")){
                        String orders[] = {"(","(",controller.getName(),"AMY",first,")","SUP","(",orderSupportUnit.controller.getName(),unitType,firstOSU,")",")"};
                        return orders;
                    } else {
                        String orders[] = {"(","(",controller.getName(),"AMY",first,")","SUP","(",orderSupportUnit.controller.getName(),unitType,"(",firstOSU,secondOSU,")",")",")"};
                        return orders;
                    }
                }
                    
                    //String orders[] = {"(","(",controller.getName(),"AMY",location.getName(),")","SUP","(",orderSupportUnit.controller.getName(),unitType,orderSupportUnit.location.getName(),")",")"};
                    //return orders;
                }
           } else if (order.equals("RTO")){
                if(second.equals("")){
                    if(secondOL.equals("")){
                        String orders[] = {"(","(",controller.getName(),"AMY",first,")","RTO",firstOL,")"};
                        return orders;
                    } else {
                        String orders[] = {"(","(",controller.getName(),"AMY",first,")","RTO",firstOL,secondOL,")"};
                        return orders;
                    }
                } else {
                    if(secondOL.equals("")){
                        String orders[] = {"(","(",controller.getName(),"AMY",first,second,")","RTO",firstOL,")"};
                        return orders;
                    } else {
                        String orders[] = {"(","(",controller.getName(),"AMY",first,second,")","RTO",firstOL,secondOL,")"};
                        return orders;
                    }
                }                
           }else if (order.equals("DSB")){
                if(second.equals("")){
                    String orders[] = {"(","(",controller.getName(),"AMY",first,")","DSB",")"};
                    return orders;
                } else {
                    String orders[] = {"(","(",controller.getName(),"AMY",first,second,")","DSB",")"};
                    return orders;
                }
           } else if (order.equals("REM")){
                if(second.equals("")){
                    String orders[] = {"(","(",controller.getName(),"AMY",first,")","REM",")"};
                    return orders;
                } else {
                    String orders[] = {"(","(",controller.getName(),"AMY",first,second,")","REM",")"};
                    return orders;
                }                
           } else {
            String orders[] = {};
            return orders;    
           }
        }
        
        public String getUnitType(){
            return "AMY";
        }
}