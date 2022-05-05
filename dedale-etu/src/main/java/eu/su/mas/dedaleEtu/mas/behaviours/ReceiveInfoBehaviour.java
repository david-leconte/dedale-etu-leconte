package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.Map;
import java.util.Set;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.CollectorCoopAgent;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class ReceiveInfoBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = -534644739562899186L;

    private boolean finished = false;

    /**
	 * Agent whose behaviour it is
	 */
	private ExploreCoopAgent myAgent;

    public ReceiveInfoBehaviour(final ExploreCoopAgent myagent) {
		super(myagent);
        this.myAgent = myagent;
	}

    @Override
    public void action() {
        this.receiveMapInfo();

        if(this.myAgent instanceof CollectorCoopAgent) this.receivePartValues();
    }

    
    @SuppressWarnings("unchecked")
    private void receiveMapInfo() {
        MapRepresentation myMap = this.myAgent.getMap();
        if(myMap==null) {
			myMap = this.myAgent.createMap();
		}

		//0) Retrieve the current position
		String myPosition=((AbstractDedaleAgent)this.myAgent).getCurrentPosition();

		if (myPosition!=null){
            //3) while openNodes is not empty, continues.
			if (!myMap.hasOpenNode()){
				//Explo finished
				finished=true;
			}else{
                //System.out.println("Receiving messages");
                // At each time step, the agent check if he received a graph from a teammate. 	
				// If it was written properly, this sharing action should be in a dedicated behaviour set.
				MessageTemplate msgTemplate1=MessageTemplate.and(
                    MessageTemplate.MatchProtocol("SHARE-TOPO"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                );
                
                ACLMessage msgReceived1=this.myAgent.receive(msgTemplate1);
                if (msgReceived1!=null) {
                    SerializableSimpleGraph<String, MapAttribute> sgreceived=null;
                    try {
                        sgreceived = (SerializableSimpleGraph<String, MapAttribute>) msgReceived1.getContentObject();
                    } catch (UnreadableException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    myMap.mergeMap(sgreceived);
                    // System.out.println("Map chunk received by agent " + this.myAgent.getLocalName());
                }

                MessageTemplate msgTemplate2=MessageTemplate.and(
                    MessageTemplate.MatchProtocol("SHARE-RESOURCES"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                );
                
                ACLMessage msgReceived2=this.myAgent.receive(msgTemplate2);
                if (msgReceived2!=null) {
                    Set<String> nodesReceived=null;
                    try {
                        nodesReceived = (Set<String>) msgReceived2.getContentObject();
                        myMap.mergeResourcefulNodes(nodesReceived);

                        for(String node : nodesReceived) {
                            myMap.addNewNode(node);
                        }
                    } catch (UnreadableException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void receivePartValues() {
        CollectorCoopAgent myCollectorAgent = (CollectorCoopAgent) this.myAgent;

        MessageTemplate msgTemplate=MessageTemplate.and(
            MessageTemplate.MatchProtocol("SHARE-PARTVAL"),
            MessageTemplate.MatchPerformative(ACLMessage.INFORM)
        );
        
        ACLMessage msgReceived=this.myAgent.receive(msgTemplate);
        if (msgReceived!=null) {
            try {
                Map<String, Couple<Observation, Integer>> receivedPartValues = (Map<String, Couple<Observation, Integer>>) msgReceived.getContentObject();
                Map<String, Couple<Observation, Integer>> myOldPartValues = myCollectorAgent.getPartialValues();

                for (Map.Entry<String, Couple<Observation, Integer>> entry : receivedPartValues.entrySet()) {
                    String agentName = entry.getKey();
                    Observation receivedAgentTreasureType = entry.getValue().getLeft();
                    int receivedValForAgent = entry.getValue().getRight();
                    
                    if(myOldPartValues.containsKey(agentName)) {
                        int storedValForAgent = myOldPartValues.get(agentName).getRight();

                        // The value can only decrease, so checking if received value in inferior to the one stored already
                        if(receivedValForAgent < storedValForAgent) {
                            myCollectorAgent.putPartialValue(agentName, receivedAgentTreasureType, receivedValForAgent);
                        }
                    }

                    else {
                        myCollectorAgent.putPartialValue(agentName, receivedAgentTreasureType, receivedValForAgent);
                    }
                }
            } catch (UnreadableException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            // System.out.println("Map chunk received by agent " + this.myAgent.getLocalName());
        }
    }


    @Override
    public boolean done() {
        return finished;
    }
    
}