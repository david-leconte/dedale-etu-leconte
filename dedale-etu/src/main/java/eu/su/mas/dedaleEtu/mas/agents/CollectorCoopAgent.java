package eu.su.mas.dedaleEtu.mas.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.behaviours.CollectionCoopBehaviour;
import jade.core.behaviours.Behaviour;

public class CollectorCoopAgent extends ExploreCoopAgent {

    private static final long serialVersionUID = 2517409590316590340L;

    private List<Couple<Observation, Integer>> backpackCapacities = new ArrayList<Couple<Observation, Integer>>();

    private String waitingNode;

    private Map<String, Couple<Observation, Integer>> partialValues;

    @Override
    protected void setup(){
      super.setup();
      this.backpackCapacities = this.getBackPackFreeSpace();
      this.partialValues = new HashMap<String, Couple<Observation, Integer>>();
    }

    @Override
    protected List<Behaviour> addOptionalBehaviours() {
		List<Behaviour> lb = new ArrayList<Behaviour>();
		
		lb.add(new CollectionCoopBehaviour(this));
		return lb;
	}

  public List<Couple<Observation, Integer>> getBackpackCapacities() {
    return backpackCapacities;
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