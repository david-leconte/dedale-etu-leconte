package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.agents.CollectorCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.SimpleBehaviour;

public class CollectionCoopBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = 965500928136399681L;

    /**
     * Allows a max difference in resources between two agents
     */
    public static final int valueMargin = 200;

    private boolean finished = false;

    /**
	 * Agent whose behaviour it is
	 */
	private CollectorCoopAgent myAgent;

    private List<String> emptiedPositions;

	/**
	 * 
	 * @param myagent
	 */
	public CollectionCoopBehaviour(final CollectorCoopAgent myagent) {
		super(myagent);
		this.myAgent = myagent;

        this.emptiedPositions = new ArrayList<String>();
	}

    @Override
    public void action() {

        MapRepresentation myMap = this.myAgent.getMap();

		if(myMap!=null) {
            // Waiting for the ExploCoopBehaviour to launch
			this.tryPicking();
        }
    }

    private void tryPicking() {
        /*try {
            this.myAgent.doWait(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }*/


        String myPosition= this.myAgent.getCurrentPosition();
        MapRepresentation myMap = this.myAgent.getMap();
        Observation myTreasureType = this.myAgent.getMyTreasureType();

        if (!myMap.hasOpenNode()){
            // Collection finished
            finished=true;

            return;
        }

        Couple<String,List<Couple<Observation,Integer>>> lobs0 = this.myAgent.observe().get(0); //myPosition
        List<Couple<Observation, Integer>> positionContentList = lobs0.getRight(); // myPosition content

        if(positionContentList.isEmpty() || this.emptiedPositions.contains(myPosition)) { // remove the current node from openlist and add it to closedNodes.
            myMap.addNode(myPosition, MapAttribute.closed);
            myMap.removeResourcefulNode(myPosition);
        }

        else {
            myMap.addNode(myPosition, MapAttribute.open);
            boolean pickOrNot = true;

            for(Couple<Observation, Integer> c : positionContentList) {
                Observation treasureType = c.getLeft();

                if( ( treasureType == Observation.DIAMOND || treasureType == Observation.GOLD ) &&
                    (myTreasureType == treasureType || myTreasureType == Observation.ANY_TREASURE )
                    && c.getRight() != null) {
                    int treasureQuantity = c.getRight();

                    List<Couple<Observation, Integer>> freeSpaces = this.myAgent.getBackPackFreeSpace();
                    int spaceForTreasure = 0;

                    for (Couple<Observation, Integer> obs : freeSpaces) {
                        if (obs.getLeft() == treasureType) {
                            spaceForTreasure = obs.getRight();
                        }
                    }

                    int fullCapacity = 0;

                    for (Couple<Observation, Integer> obs : this.myAgent.getBackpackCapacities()) {
                        if (obs.getLeft() == treasureType) {
                            fullCapacity = obs.getRight();
                        }
                    }

                    int myRealValue = -(fullCapacity - spaceForTreasure) - treasureQuantity + CollectionCoopBehaviour.valueMargin;
                    
                    for(Map.Entry<String, Couple<Observation, Integer>> entry2 : this.myAgent.getPartialValues().entrySet()) {
                        Observation otherAgentTreasureType = entry2.getValue().getLeft();
                        int partValue = entry2.getValue().getRight();

                        int otherAgentRealValue = partValue - treasureQuantity;

                        if(otherAgentTreasureType == treasureType &&
                            otherAgentRealValue > myRealValue) {
                                pickOrNot = false;
                            }
                    }
                    
                    if(pickOrNot) {
                        //System.out.println(this.myAgent.getLocalName() + " tries to pick " + treasureQuantity +
                           // " of " + treasureType + " with remaining capacity " + spaceForTreasure);
                        this.pickTreasure(treasureQuantity, treasureType);
                    }

                    else {
                        this.myAgent.setWaitingNode(myPosition);
                    }
                }
            }
        }
    }

    private void pickTreasure(int treasureQuantity, Observation treasureType) {
        String myPosition= this.myAgent.getCurrentPosition();
        MapRepresentation myMap = this.myAgent.getMap();

        try {
            this.myAgent.openLock(treasureType);
            int pickedQuantity = this.myAgent.pick();

            //System.out.println(this.myAgent.getLocalName() + " picked " + pickedQuantity +
                            //" of " + treasureQuantity + " " + treasureType + " available at " + myPosition);
            
            if(pickedQuantity > 0) {
                
                if(pickedQuantity == treasureQuantity) { // remove the current node from openlist and add it to closedNodes.
                    myMap.removeResourcefulNode(myPosition);
                }

                System.out.println(this.myAgent.getLocalName() + " picked " + pickedQuantity+ " " + treasureType.toString() + " at " + myPosition);
                //System.out.println(this.myAgent.getBackPackFreeSpace() + " of space left in backpack");
            }

            else {
                // If it doesn't work, then the lockpicking ability is probably not strong enough anyway
                //this.emptiedPositions.add(myPosition);
            }
        } catch(IndexOutOfBoundsException e) {
            System.out.println(this.myAgent.getLocalName() + " \"start pick\" error, execution continues");
        } finally {
            this.emptiedPositions.add(myPosition);
            myMap.addNode(myPosition, MapAttribute.closed);
        }
    }

    @Override
    public boolean done() {
        return finished;
    }
    
}