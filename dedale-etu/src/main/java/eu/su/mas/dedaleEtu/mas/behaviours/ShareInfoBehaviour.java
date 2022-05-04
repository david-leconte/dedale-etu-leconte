package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.agents.CollectorCoopAgent;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

public class ShareInfoBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = 5626456440832147655L;
    private boolean finished = false;

    private int maxPossibleDirections;

    private ExploreCoopAgent myAgent;

    private List<String> agentNames;

    public ShareInfoBehaviour(ExploreCoopAgent myagent, List<String> agentnames) {
        super(myagent);

		this.myAgent = myagent;
		this.agentNames = agentnames;

        this.maxPossibleDirections = 0;
    }

    @Override
    public void action() {
        this.shareMap();
        if(this.myAgent instanceof CollectorCoopAgent) this.sharePartialValue();
    }

    private void shareMap() {
		String myPosition = this.myAgent.getCurrentPosition();
		MapRepresentation myMap = this.myAgent.getMap();

		/*if (!myMap.hasOpenNode()){
			//Explo finished
			finished=true;
		}*/

		//List of observable from the agent's current position
		List<Couple<String,List<Couple<Observation,Integer>>>> lobs= this.myAgent.observe();//myPosition

		MapRepresentation mapToBeSent = new MapRepresentation(false);

		if(lobs.size() > this.maxPossibleDirections) this.maxPossibleDirections = lobs.size();

		//2) get the surrounding nodes and, if not in closedNodes, add them to open nodes.
		Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter=lobs.iterator();
		while(iter.hasNext()){
			Couple<String, List<Couple<Observation, Integer>>> c = iter.next();
			String nodeId = c.getLeft();
			List<Couple<Observation, Integer>> content = c.getRight();
			
			//System.out.println(myPosition + " " + nodeId);
			boolean isNodeOnBorder = nodeId == myPosition && lobs.size() < this.maxPossibleDirections;

			if(myMap.getClosedNodes().contains(nodeId) && content.isEmpty() && !isNodeOnBorder) {
				mapToBeSent.addNode(nodeId, MapAttribute.closed);
			} else {
				if(!content.isEmpty()) {
					myMap.putResourcefulNode(nodeId);
					//System.out.println(this.myAgent.getLocalName() + " found something at " + nodeId);
				}

				mapToBeSent.addNode(nodeId, MapAttribute.open);
			}

			//the node may exist, but not necessarily the edge
			if (myPosition!=nodeId) {
				mapToBeSent.addEdge(myPosition, nodeId);
			}
		}

		//3) At each time step, the agent blindly send all its graph to its surrounding to illustrate how to share its knowledge (the topology currently) with the the others agents. 	
		// If it was written properly, this sharing action should be in a dedicated behaviour set, the receivers be automatically computed, and only a subgraph would be shared.
		
		ACLMessage msgTopo = new ACLMessage(ACLMessage.INFORM);
		msgTopo.setProtocol("SHARE-TOPO");
		msgTopo.setSender(this.myAgent.getAID());

		for (String agentName : agentNames) {
			msgTopo.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}

		SerializableSimpleGraph<String, MapAttribute> sg=mapToBeSent.getSerializableGraph();
		try {					
			msgTopo.setContentObject(sg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.myAgent.sendMessage(msgTopo);

		ACLMessage msgResources = new ACLMessage(ACLMessage.INFORM);
		msgResources.setProtocol("SHARE-RESOURCES");
		msgResources.setSender(this.myAgent.getAID());

		for (String agentName : agentNames) {
			msgResources.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}

		Set<String> resourcefulNodes = myMap.getResourcefulNodes();

		try {					
			msgResources.setContentObject((Serializable) resourcefulNodes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.myAgent.sendMessage(msgResources);

		// System.out.println("Message sent by agent " + this.myAgent.getLocalName());
	}

	private void sharePartialValue() {
		CollectorCoopAgent collectorAgent = (CollectorCoopAgent) this.myAgent;
		Observation treasureType = collectorAgent.getMyTreasureType();
		List<Couple<Observation, Integer>> freeSpaces = this.myAgent.getBackPackFreeSpace();

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

		int partialValue = -(fullCapacity - spaceForTreasure);
		Couple<Observation, Integer> messageObject = new Couple<Observation, Integer>(treasureType, partialValue);

		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setProtocol("SHARE-PARTVAL");
		msg.setSender(this.myAgent.getAID());

		for (String agentName : agentNames) {
			msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}

		try {					
			msg.setContentObject((Serializable) messageObject);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.myAgent.sendMessage(msg);
	}

    @Override
    public boolean done() {
        return finished;
    }
    
}