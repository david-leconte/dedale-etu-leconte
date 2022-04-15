package eu.su.mas.dedaleEtu.mas.behaviours.negotiation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.CollectorCoopAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.exploration.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class InitNegotiationsBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = 965500928136399681L;

    private static long baseTimeout = ExploCoopBehaviour.waitingTime * 4;
    private static double timeoutLimit = InitNegotiationsBehaviour.baseTimeout * 3.5;
    private static double timeoutMultiplicationFactor = 17;

    private boolean finished = false;

    /**
	 * Agent whose behaviour it is
	 */
	private CollectorCoopAgent myAgent;

    private long currentTimeout;

    private Map<Integer, Integer> ongoingNegotiationsTreasureQuantities;
    private Map<Integer, Long> ongoingNegotiationsTimestamps;

    private List<String> emptiedPositions;

	/**
	 * 
	 * @param myagent
	 */
	public InitNegotiationsBehaviour(final CollectorCoopAgent myagent) {
		super(myagent);
		this.myAgent = myagent;

        this.currentTimeout = InitNegotiationsBehaviour.baseTimeout;

        this.ongoingNegotiationsTreasureQuantities = new HashMap<Integer, Integer>();
        this.ongoingNegotiationsTimestamps = new HashMap<Integer, Long>();

        this.emptiedPositions = new ArrayList<String>();
	}

    @Override
    public void action() {

        MapRepresentation myMap = this.myAgent.getMap();

		if(myMap!=null) {
            // Waiting for the ExploCoopBehaviour to launch
			this.initNegotiation();
            this.receiveNegotiationAnswer();
        }
    }

    private void initNegotiation() {

        MapRepresentation myMap = this.myAgent.getMap();
        //0) Retrieve the current position
		String myPosition= this.myAgent.getCurrentPosition();
        boolean agentLocked = this.myAgent.getLocked();

        Observation myTreasureType = this.myAgent.getMyTreasureType();

		if (myPosition!=null && !agentLocked){
            // If the map has no oepn nodes left, signal to the treasure agent to remove the current agent from negotiations
            if (!myMap.hasOpenNode()){
                //Explo finished
                finished=true;
                
                //this.removeAgentFromNegotiations();
            }

            else if (!this.emptiedPositions.contains(myPosition) ) {

                //List of observable from the agent's current position
                List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition

                Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
                while(iter.hasNext()){
                    List<Couple<Observation, Integer>> obsContentList = iter.next().getRight();
                    
                    if(obsContentList.isEmpty()) { // remove the current node from openlist and add it to closedNodes.
                        myMap.addNode(myPosition, MapAttribute.closed);
                        myMap.removeResourcefulNode(myPosition);
                    }

                    else {
                        myMap.addNode(myPosition, MapAttribute.open);
                    }

                    for(Couple<Observation, Integer> c : obsContentList) {
                        Observation treasureType = c.getLeft();
                        int treasureQuantity;
                        if(c.getRight() != null) {
                            treasureQuantity = c.getRight();
                        }

                        else return;

                        if(myTreasureType == treasureType) {
                            List<Couple<Observation, Integer>> freeSpaces = this.myAgent.getBackPackFreeSpace();

                            int spaceForTreasure = 0;

                            for(Couple<Observation, Integer> obs : freeSpaces) {
                                if(obs.getLeft() == treasureType) {
                                    spaceForTreasure = obs.getRight();
                                }
                            }

                            if(spaceForTreasure >= treasureQuantity) {
                                int fullCapacity = 0;

                                for(Couple<Observation, Integer> coup : this.myAgent.getBackpackCapacities()) {
                                    if(coup.getLeft() == treasureType) {
                                        fullCapacity = coup.getRight();
                                    }
                                }

                                // The more it has already taken the less the value
                                int realValue =  - (fullCapacity - spaceForTreasure) - treasureQuantity;

                                int sharedNegotiationID = new Random().nextInt(1000) + 100;
                                this.ongoingNegotiationsTreasureQuantities.put(sharedNegotiationID, treasureQuantity);
                                this.ongoingNegotiationsTimestamps.put(sharedNegotiationID, System.currentTimeMillis());

                                Couple<Observation, Integer> negTermsLeft = new Couple<Observation, Integer>(treasureType, sharedNegotiationID);
                                Couple<Integer, Integer> negTermsRight = new Couple<Integer, Integer>(treasureQuantity, realValue);

                                Couple<Couple<Observation, Integer>, Couple<Integer, Integer>> negotiationTerms = new Couple<Couple<Observation, Integer>,  Couple<Integer, Integer>>(negTermsLeft, negTermsRight);
                                
                                sendNegotiation(negotiationTerms);
                                this.myAgent.lockPosition();
                            }
                        }
                    }
                }
            }
        }
    }

    private void sendNegotiation( Couple<Couple<Observation, Integer>,  Couple<Integer, Integer>> content) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setSender(this.myAgent.getAID());
        msg.setProtocol("INIT-NEG");
        
        msg.addReceiver(new AID("TAgent",AID.ISLOCALNAME));

        try {					
            msg.setContentObject(content);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.myAgent.send(msg);
        //System.out.println("Sent message to TreasureAgent");
    }

    private void receiveNegotiationAnswer() {
        /*try {
            this.myAgent.doWait(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        String myPosition= this.myAgent.getCurrentPosition();
        MapRepresentation myMap = this.myAgent.getMap();

        MessageTemplate msgTemplate=MessageTemplate.MatchProtocol("END-NEG");

        ACLMessage msgReceived=this.myAgent.receive(msgTemplate);

        //System.out.println("Message received by agent " + this.myAgent.getLocalName());
        if(msgReceived != null) {
            int answer = msgReceived.getPerformative();
            boolean pick = false;

            try {

                int negotiationID = (int) msgReceived.getContentObject();

                pick = (answer == ACLMessage.ACCEPT_PROPOSAL && this.ongoingNegotiationsTreasureQuantities.containsKey(negotiationID) ) ? true : false;

                if(pick) {
                    int pickedQuantity = this.myAgent.pick();
                    this.currentTimeout = baseTimeout;
        
                    if(pickedQuantity > 0) {
                        int treasureQuantity = this.ongoingNegotiationsTreasureQuantities.get(negotiationID);
                        
                        if(pickedQuantity == treasureQuantity) { // remove the current node from openlist and add it to closedNodes.
                            myMap.addNode(myPosition, MapAttribute.closed);
                        }
        
                        System.out.println(this.myAgent.getLocalName() + " picked " + pickedQuantity+ " " + this.myAgent.getMyTreasureType().toString() + " at " + myPosition);
                        //System.out.println(this.myAgent.getBackPackFreeSpace() + " of space left in backpack");
                    }
                }
        
                else {
                    myMap.addNode(myPosition, MapAttribute.closed);
                    myMap.putResourcefulNode(myPosition);
                }
            } catch (UnreadableException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                this.myAgent.unlockPosition();
            }
        }

        for(Iterator<Map.Entry<Integer, Long>> it = this.ongoingNegotiationsTimestamps.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Long> entry = it.next();

            int negotiationID = entry.getKey();
            long timestamp = entry.getValue();

            if(this.currentTimeout > InitNegotiationsBehaviour.timeoutLimit) {
                myMap.putResourcefulNode(myPosition);

                this.myAgent.setWaitingNode(myPosition);
                this.myAgent.unlockPosition();

                it.remove();
                this.ongoingNegotiationsTreasureQuantities.remove(negotiationID);

                this.currentTimeout /= InitNegotiationsBehaviour.timeoutMultiplicationFactor;
            }
            else if(System.currentTimeMillis() > timestamp + this.currentTimeout) {
                
                int treasureQuantity = this.ongoingNegotiationsTreasureQuantities.get(negotiationID);
                boolean pickOrNot = true;

                List<Couple<Observation, Integer>> freeSpaces = this.myAgent.getBackPackFreeSpace();

                int spaceForTreasure = 0;

                for (Couple<Observation, Integer> obs : freeSpaces) {
                    if (obs.getLeft() == this.myAgent.getMyTreasureType()) {
                        spaceForTreasure = obs.getRight();
                    }
                }

                int fullCapacity = 0;

                for (Couple<Observation, Integer> obs : this.myAgent.getBackpackCapacities()) {
                    if (obs.getLeft() == this.myAgent.getMyTreasureType()) {
                        fullCapacity = obs.getRight();
                    }
                }

                int myRealValue = -(fullCapacity - spaceForTreasure) - treasureQuantity + HandleProposalBehaviour.valueMargin;
                
                for(Map.Entry<String, Couple<Observation, Integer>> entry2 : this.myAgent.getPartialValues().entrySet()) {
                    Observation treasureType = entry2.getValue().getLeft();
                    int partValue = entry2.getValue().getRight();

                    int otherAgentRealValue = partValue - treasureQuantity;

                    if(treasureType == this.myAgent.getMyTreasureType() &&
                        otherAgentRealValue > myRealValue) {
                            pickOrNot = false;
                        }
                }
                
                if(pickOrNot) {
                    this.pickTreasure(negotiationID);
                }

                else {
                    this.myAgent.setWaitingNode(myPosition);
                }

                
                this.myAgent.unlockPosition();
                
                it.remove();
                this.ongoingNegotiationsTreasureQuantities.remove(negotiationID);

                this.currentTimeout *= InitNegotiationsBehaviour.timeoutMultiplicationFactor;
                //System.out.println(this.myAgent.getLocalName() + " used forced unlock (" + this.myAgent.getCurrentPosition() + ")");
            }
        }
    }

    private void pickTreasure(int negotiationID) {
        String myPosition= this.myAgent.getCurrentPosition();
        MapRepresentation myMap = this.myAgent.getMap();

        try {
        
            int pickedQuantity = this.myAgent.pick();
            this.emptiedPositions.add(myPosition);
            
            if(pickedQuantity > 0) {
                int treasureQuantity = this.ongoingNegotiationsTreasureQuantities.get(negotiationID);
                
                if(pickedQuantity == treasureQuantity) { // remove the current node from openlist and add it to closedNodes.
                    myMap.addNode(myPosition, MapAttribute.closed);
                    myMap.removeResourcefulNode(myPosition);
                }

                System.out.println(this.myAgent.getLocalName() + " picked " + pickedQuantity+ " " + this.myAgent.getMyTreasureType().toString() + " at " + myPosition);
                //System.out.println(this.myAgent.getBackPackFreeSpace() + " of space left in backpack");
            }
        } catch(IndexOutOfBoundsException e) {
            System.out.println(this.myAgent.getLocalName() + " \"start pick\" error, execution continues");
        }
    }

    // private void removeAgentFromNegotiations() {
    //     ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
    //     msg.setSender(this.myAgent.getAID());
    //     msg.setProtocol("REMOVE-NEG");
        
    //     msg.addReceiver(new AID("TAgent",AID.ISLOCALNAME));

    //     this.myAgent.send(msg);
    // }

    @Override
    public boolean done() {
        return finished;
    }
    
}