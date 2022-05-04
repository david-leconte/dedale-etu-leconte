package eu.su.mas.dedaleEtu.mas.agents;

import java.util.ArrayList;
import java.util.List;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.HelpTerminationBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ReceiveInfoBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareInfoBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

import jade.core.behaviours.Behaviour;

/**
 * <pre>
 * ExploreCoop agent. 
 * Basic example of how to "collaboratively" explore the map
 *  - It explore the map using a DFS algorithm and blindly tries to share the topology with the agents within reach. (DONE)
 *  - The shortestPath computation is not optimized (TDODO)
 *  - Agents do not coordinate themselves on the node(s) to visit, thus progressively creating a single file. It's bad. (TODO)
 *  - The agent sends all its map, periodically, forever. Its bad x3. -(DONE)
 *   - You should give him the list of agents'name to send its map to in parameter when creating the agent (DONE)
 *   Object [] entityParameters={"Name1","Name2};
 *   ag=createNewDedaleAgent(c, agentName, ExploreCoopAgent.class.getName(), entityParameters);
 *  
 * It stops when all nodes have been visited.
 * 
 * 
 *  </pre>
 *  
 * @author hc
 *
 */


public class ExploreCoopAgent extends AbstractDedaleAgent {

	private static final long serialVersionUID = -7969469610241668140L;
	
	private MapRepresentation myMap;
	private Couple<String, String> last2Positions;
	private String nextDest;
	private List<String> neighbourhood = new ArrayList<String>();

	protected List<String> list_agentNames=new ArrayList<String>();
	

	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){

		super.setup();
		
		//get the parameters added to the agent at creation (if any)
		final Object[] args = getArguments();
		
		if(args.length==0){
			System.err.println("Error while creating the agent, names of agent to contact expected");
			System.exit(-1);
		}else{
			int i=2;// WARNING YOU SHOULD ALWAYS START AT 2. This will be corrected in the next release.
			while (i<args.length) {
				this.list_agentNames.add((String)args[i]);
				i++;
			}
		}

		//System.out.println("Agent names " + list_agentNames);

		List<Behaviour> lb = new ArrayList<Behaviour>();

		lb.add(new ExploCoopBehaviour(this, this.list_agentNames));
		lb.add(new ShareInfoBehaviour(this, this.list_agentNames));
		lb.add(new ReceiveInfoBehaviour(this));
		lb.add(new HelpTerminationBehaviour(this, this.list_agentNames));

		List<Behaviour> optionallb=addOptionalBehaviours();
		lb.addAll(optionallb);
		
		/***
		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 */
		
		addBehaviour(new startMyBehaviours(this,lb));
		
		System.out.println("the  agent "+this.getLocalName()+ " is started");
		this.last2Positions = new Couple<String, String>(null, null);
	}

	protected List<Behaviour> addOptionalBehaviours() {
		return new ArrayList<Behaviour>();
	}

	public MapRepresentation createMap() {
		myMap = new MapRepresentation();
		return myMap;
	}
	
	public MapRepresentation getMap() {
		return myMap;
	}

	public void setLastPosition() {
		if(this.getCurrentPosition() != this.last2Positions.getRight()) {

			this.last2Positions = new Couple<String, String>(this.last2Positions.getRight(), this.getCurrentPosition());
		}
	}

	public Couple<String, String> getLast2Positions() {
		return this.last2Positions;
	}

	public List<String> getNeighbourhood() {
		return neighbourhood;
	}
	
	public void cleanNeighbourhood() {
		this.neighbourhood.clear();
	}
	
	public void addNeighbourhood(String node) {
		this.neighbourhood.add(node);
	}

	public void setNeighbourhood(List<String> neighbourhood) {
		this.neighbourhood = neighbourhood;
	}

	public String getNextDest() {
		return nextDest;
	}

	public void setNextDest(String nextDest) {
		this.nextDest = nextDest;
	}
}
