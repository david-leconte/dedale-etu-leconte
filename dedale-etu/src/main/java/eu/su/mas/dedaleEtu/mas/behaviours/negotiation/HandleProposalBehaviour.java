package eu.su.mas.dedaleEtu.mas.behaviours.negotiation;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.agents.CollectorCoopAgent;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.behaviours.exploration.ExploCoopBehaviour;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class HandleProposalBehaviour extends SimpleBehaviour {

    /**
     *
     */
    private static final long serialVersionUID = 1935814663308500918L;

    /**
     * Timeout before sending positive answer
     */

    public static int negTimeoutFactor = 10;

    /**
     * Allows a max difference in resources between two agents
     */
    public static int valueMargin = 80;

    private boolean finished = false;

    /**
     * Agent whose behaviour it is
     */
    private ExploreCoopAgent myAgent;

    private List<String> list_agentNames;

    /**
     * Treasure type for each negotiation ID
     */

    private Map<Integer, Observation> negotiationsTreasureType;

    /**
     * Treasure quantity for each negotiation ID
     */

    private Map<Integer, Integer> negotiationsTreasureQuantity;

    /**
     * Initiating agent for each negotiation ID
     */
    private Map<Integer, AID> negotiationsInitiators = new HashMap<Integer, AID>();

    /**
     * Timestamp of negotiation initialization for each negotiation ID
     */

    private Map<Integer, Long> negotiationsTimestamps = new HashMap<Integer, Long>();

    /**
     * Answers received for each negotiation ID
     */
    private Map<Integer, Boolean> negotiationsAnswersReceived = new HashMap<Integer, Boolean>();

    /**
     * Real value of the treasure for each agent for each negotiation ID
     */
    private Map<Integer, Integer> negotiationsSenderRealValues = new HashMap<Integer, Integer>();

    /**
     * 
     * @param myagent
     * @param agentNames name of the agents to share the map with
     */
    public HandleProposalBehaviour(ExploreCoopAgent myagent, List<String> list_agentnames) {
        super(myagent);
        this.myAgent = myagent;
        this.list_agentNames = list_agentnames;
    }

    @Override
    public void action() {
        this.handleNegotiationStart();
        this.handleNegotiationAnswers();
        this.checkNegotiationsTimeouts();
    }

    @SuppressWarnings("unchecked")
    private void handleNegotiationStart() {
        //System.out.println("Negotiation handling started");
        MessageTemplate msgTemplate=MessageTemplate.and(
            MessageTemplate.MatchProtocol("INIT-NEG"),
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
        );

        ACLMessage msgReceived=this.myAgent.receive(msgTemplate);
       

        if(msgReceived != null) {
            AID sender = msgReceived.getSender();

            try {
                Couple<Couple<Observation, Integer>,  Couple<Integer, Integer>> content = (Couple<Couple<Observation, Integer>,  Couple<Integer, Integer>>) msgReceived.getContentObject();
                int sharedNegotiationID = content.getLeft().getRight();
                
                sharedNegotiationID = content.getLeft().getRight();

                Observation treasureType = content.getLeft().getLeft();
                this.negotiationsTreasureType.put(sharedNegotiationID,  treasureType);
                
                int treasureQuantity = content.getRight().getLeft();
                this.negotiationsTreasureQuantity.put(sharedNegotiationID, treasureQuantity);

                this.negotiationsInitiators.put(sharedNegotiationID, sender);
                this.negotiationsAnswersReceived.put(sharedNegotiationID, true);
                this.negotiationsTimestamps.put(sharedNegotiationID, System.currentTimeMillis());

                int realValueForSender = content.getRight().getRight();
                
                //Map<AID, Integer> realValues = new HashMap<AID, Integer>();

                if(treasureQuantity < valueMargin) {
                    this.negotiationsSenderRealValues.put(sharedNegotiationID, realValueForSender + HandleProposalBehaviour.valueMargin);
                } else { // Just in case the treasure has a very high value, so it isn't unpickable at all
                    this.negotiationsSenderRealValues.put(sharedNegotiationID, realValueForSender + (HandleProposalBehaviour.valueMargin*2));
                }

                Couple<Integer, Integer> rightPart = new Couple<Integer, Integer>(treasureQuantity, sharedNegotiationID);
                Couple<Observation, Couple<Integer, Integer>> negotiationTerms = new Couple<Observation, Couple<Integer, Integer>>(treasureType, rightPart);


                ACLMessage globalAskMsg = new ACLMessage(ACLMessage.INFORM);

                globalAskMsg.setProtocol("INIT-NEG");
                globalAskMsg.setContentObject(negotiationTerms);

                for (String agentName : this.list_agentNames) {

                    if(agentName != sender.getLocalName()) {
                        globalAskMsg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
                    }
                }

                this.myAgent.sendMessage(globalAskMsg);

                try {
                    this.myAgent.doWait(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (UnreadableException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
            // System.out.println("Treasure agent received negotiation request");
    }

    @SuppressWarnings("unchecked")
    private void handleNegotiationAnswers() {
        //System.out.println("Negotiation handling started");
        MessageTemplate msgTemplate=MessageTemplate.and(
            MessageTemplate.MatchProtocol("END-NEG"),
            MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
            , 
                MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL) 
        ));

        ACLMessage msgReceived=this.myAgent.receive(msgTemplate);
       

        if(msgReceived != null) {

            Couple<Boolean, Integer> content;
            try {
                content = (Couple<Boolean, Integer>) msgReceived.getContentObject();
                int negotiationID = content.getRight();

                if(this.negotiationsAnswersReceived.containsKey(negotiationID)) {

                    boolean answer = content.getLeft();
                    this.negotiationsAnswersReceived.put(negotiationID, answer);

                    if(answer == false) {
                        AID negotiationInitiator= this.negotiationsInitiators.get(negotiationID);
                        ACLMessage decisionMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
        
                        decisionMsg.setSender(this.myAgent.getAID());
                        decisionMsg.setProtocol("END-NEG");
                        
                        decisionMsg.addReceiver(negotiationInitiator);

                        decisionMsg.setContentObject(negotiationID);
        
                        this.myAgent.send(decisionMsg);
                    }
                
                }

            } catch (UnreadableException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void treatNegotiationForMyself(int negotiationID) {
        Observation myTreasureType = this.myAgent.getMyTreasureType();
        int decision = ACLMessage.ACCEPT_PROPOSAL;

        Observation treasureType = this.negotiationsTreasureType.get(negotiationID);

        if (this.myAgent instanceof CollectorCoopAgent && myTreasureType == treasureType) {
            CollectorCoopAgent collectorAgent = (CollectorCoopAgent) this.myAgent;

            int treasureQuantity = this.negotiationsTreasureQuantity.get(negotiationID);
            List<Couple<Observation, Integer>> freeSpaces = this.myAgent.getBackPackFreeSpace();

            // MapRepresentation myMap = this.myAgent.getMap();

            int spaceForTreasure = 0;

            for (Couple<Observation, Integer> obs : freeSpaces) {
                if (obs.getLeft() == treasureType) {
                    spaceForTreasure = obs.getRight();
                }
            }

            int fullCapacity = 0;

            for (Couple<Observation, Integer> obs : collectorAgent.getBackpackCapacities()) {
                if (obs.getLeft() == treasureType) {
                    fullCapacity = obs.getRight();
                }
            }

            int realValue = -(fullCapacity - spaceForTreasure) - treasureQuantity;
            int senderRealValue = this.negotiationsSenderRealValues.get(negotiationID);

            if(realValue > senderRealValue) {
                decision = ACLMessage.REJECT_PROPOSAL;
            }

        }

        AID negotiationInitiator = this.negotiationsInitiators.get(negotiationID);

        ACLMessage decisionMsg = new ACLMessage(decision);

        decisionMsg.setSender(this.myAgent.getAID());
        decisionMsg.setProtocol("END-NEG");
        
        decisionMsg.addReceiver(negotiationInitiator);

        this.myAgent.send(decisionMsg);
    }

    private void checkNegotiationsTimeouts() {
        for(Map.Entry<Integer, Long> entry : this.negotiationsTimestamps.entrySet()) {
            int negotiationID = entry.getKey();
            long timestamp = entry.getValue();

            if(System.currentTimeMillis() > timestamp + (HandleProposalBehaviour.negTimeoutFactor * ExploCoopBehaviour.waitingTime)) {
                this.treatNegotiationForMyself(negotiationID);
            }
        }
    }

    @Override
    public boolean done() {
        return finished;
    }

}
