package eu.su.mas.dedaleEtu.mas.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.behaviours.negotiation.InitNegotiationsBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.negotiation.HandleProposalBehaviour;
import jade.core.behaviours.Behaviour;

public class CollectorCoopAgent extends ExploreCoopAgent {

    private static final long serialVersionUID = 2517409590316590340L;

    private List<Couple<Observation, Integer>> backpackCapacities = new ArrayList<Couple<Observation, Integer>>();

    private boolean isLocked;
    private String waitingNode;

    private Map<String, Couple<Observation, Integer>> partialValues;

    @Override
    protected void setup(){
      super.setup();
      this.backpackCapacities = this.getBackPackFreeSpace();
      this.isLocked = false;
      this.partialValues = new HashMap<String, Couple<Observation, Integer>>();
    }

    @Override
    protected List<Behaviour> addOptionalBehaviours() {
		List<Behaviour> lb = new ArrayList<Behaviour>();
		
		lb.add(new InitNegotiationsBehaviour(this));
    lb.add(new HandleProposalBehaviour(this, this.list_agentNames));

		return lb;
	}

  public List<Couple<Observation, Integer>> getBackpackCapacities() {
    return backpackCapacities;
  }

  public void lockPosition() {
    String myPosition= this.getCurrentPosition();

    if(myPosition != null) {
      this.isLocked = true;

      //System.out.println(this.getLocalName() + " has been locked on position " + myPosition);
    }
  }

  public void unlockPosition() {
    String myPosition= this.getCurrentPosition();

    if(myPosition != null) {
      this.isLocked = false;
    }
  }

  public boolean getLocked() {
    return this.isLocked;
  }

    
  public String getWaitingNode() {
      return waitingNode;
  }

  public void setWaitingNode(String waitingNode) {
      this.waitingNode = waitingNode;
  }

  public void putPartialValue(String agentName, Observation treasureType, int partialValue) {
    Couple<Observation, Integer> c = new Couple<Observation, Integer>(treasureType, partialValue);
    this.partialValues.put(agentName, c);
  }

  public Map<String, Couple<Observation, Integer>> getPartialValues() {
    return this.partialValues;
  }

}