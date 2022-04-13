package eu.su.mas.dedaleEtu.mas.behaviours.exploration;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class HelpTerminationBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = 4894827732742647734L;

    public static int endOfWorldTimeoutFactor = 425;

    private boolean finished = false;

    // Reseted every time the agent receives a message
    private long timeLastMessage;

    private ExploreCoopAgent myAgent;

    private Set<String> agentNames;
    private Set<String> terminatedAgents;
    private Set<String> agentsAcknowledgingMyTermination;

    /**
     * 
     * @param myagent
     * @param agentNames name of the agents to share the map with
     */
    public HelpTerminationBehaviour(ExploreCoopAgent myagent, List<String> agentnames) {
        super(myagent);

        this.myAgent = myagent;
        this.agentNames = new HashSet<String>(agentnames);

        this.terminatedAgents = new CopyOnWriteArraySet<String>();
        this.agentsAcknowledgingMyTermination = new CopyOnWriteArraySet<String>();

        this.resetTimeout();
    }

    private void resetTimeout() {
        this.timeLastMessage = System.currentTimeMillis();
    }

    @Override
    public void action() {
        if (!this.myAgent.getMap().hasOpenNode()) {
            try {
                this.myAgent.doWait(ExploCoopBehaviour.waitingTime);
            } catch (Exception e) {
                e.printStackTrace();
            }

            this.terminatedAgents.add(this.myAgent.getLocalName());
            this.randomWalk();

        }

        if (!this.terminatedAgents.isEmpty()) {
            this.sendTerminationMessage();
        }

        if (this.terminatedAgents.contains(this.myAgent.getLocalName())) {
            this.receiveACKTerminationMessage();
        }

        this.sendACKTerminationMessages();
        this.receiveTerminationMessage();

        this.tryFinishing();
    }

    private boolean randomWalk() {
        List<Couple<String, List<Couple<Observation, Integer>>>> lobs = this.myAgent.observe();
        lobs.remove(0); // not moving to the same node

        int availableDirections = lobs.size();
        Random rand = new Random();
        int direction = (int) Math.floor(rand.nextDouble() * availableDirections);
        String nextNode = lobs.get(direction).getLeft();

        this.myAgent.setNextDest(nextNode);
        this.myAgent.setLastPosition();

        // System.out.println(this.myAgent.getLocalName() + " trying move to " +
        // nextNode);

        return this.myAgent.moveTo(nextNode);
    }

    private void sendTerminationMessage() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setProtocol("AGENT-TERM");
        msg.setSender(this.myAgent.getAID());

        for (String agentName : agentNames) {
            msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
        }

        try {
            msg.setContentObject((Serializable) this.terminatedAgents);
            this.myAgent.sendMessage(msg);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @SuppressWarnings("unchecked")
    private void receiveTerminationMessage() {
        MessageTemplate msgTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol("AGENT-TERM"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));

        ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
        if (msgReceived != null) {
            this.resetTimeout();

            try {
                Set<String> terminatedAgentsReceived = (Set<String>) msgReceived.getContentObject();

                for (String termAgent : terminatedAgentsReceived) {
                    this.terminatedAgents.add(termAgent);
                }

                // System.out.println(this.myAgent.getLocalName() + " terminated agents " +
                // this.terminatedAgents);
            } catch (UnreadableException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void sendACKTerminationMessages() {
        Set<String> otherTerminatedAgents = this.terminatedAgents;
        otherTerminatedAgents.remove(this.myAgent.getLocalName());

        for (String termAgentName : otherTerminatedAgents) {
            ACLMessage msgACK = new ACLMessage(ACLMessage.CONFIRM);
            msgACK.setProtocol("AGENT-TERM");
            msgACK.setSender(this.myAgent.getAID());

            msgACK.addReceiver(new AID(termAgentName, AID.ISLOCALNAME));
            msgACK.setContent("ACK");

            this.myAgent.sendMessage(msgACK);
        }
    }

    private void receiveACKTerminationMessage() {
        MessageTemplate msgTemplate = MessageTemplate.and(
                MessageTemplate.MatchProtocol("AGENT-TERM"),
                MessageTemplate.MatchPerformative(ACLMessage.CONFIRM));

        ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
        if (msgReceived != null && msgReceived.getContent().contains("ACK")) {
            this.resetTimeout();
            this.agentsAcknowledgingMyTermination.add(msgReceived.getSender().getLocalName());
            // System.out.println(this.myAgent.getLocalName() + " agents informed of my
            // termination " + this.agentsInformedOfMyTermination);
        }
    }

    private boolean tryFinishing() {
        Set<String> otherTerminatedAgents = this.terminatedAgents;
        otherTerminatedAgents.remove(this.myAgent.getLocalName()); // removing the current agent to check sets equality

        long fullTimeout = !otherTerminatedAgents.isEmpty() ? this.timeLastMessage + 
            ( (ExploCoopBehaviour.waitingTime * HelpTerminationBehaviour.endOfWorldTimeoutFactor) / otherTerminatedAgents.size() ) : 
            Long.MAX_VALUE;

        if (otherTerminatedAgents.equals(this.agentNames)
                && this.agentsAcknowledgingMyTermination.equals(this.agentNames)) {
            System.out.println(
                    "*** " + this.myAgent.getLocalName()
                            + " has informed and was informed by everyone that all processes were done. ***");
            this.finished = true;
        }

        else if (System.currentTimeMillis() > fullTimeout) {
            System.out.println(
                    "*** " + this.myAgent.getLocalName()
                            + " has reached the end of the world timeout. ***");
            this.finished = true;
        }

        if (finished)
            this.myAgent.doSuspend();
        return this.finished;
    }

    @Override
    public boolean done() {
        return finished;
    }

}