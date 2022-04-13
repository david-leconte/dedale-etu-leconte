package eu.su.mas.dedaleEtu.mas.behaviours.exploration;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedaleEtu.mas.agents.CollectorCoopAgent;
import eu.su.mas.dedaleEtu.mas.agents.ExploreCoopAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.SimpleBehaviour;

/**
 * <pre>
 * This behaviour allows an agent to explore the environment and learn the associated topological map.
 * The algorithm is a pseudo - DFS computationally consuming because its not optimised at all.
 * 
 * When all the nodes around him are visited, the agent randomly select an open node and go there to restart its dfs. 
 * This (non optimal) behaviour is done until all nodes are explored. 
 * 
 * Warning, this behaviour does not save the content of visited nodes, only the topology.
 * Warning, the sub-behaviour ShareMap periodically share the whole map
 * </pre>
 * 
 * @author hc
 *
 */
public class ExploCoopBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	public static final long waitingTime = 300; // in milliseconds
	private static final int maxLoopTrials = 3;

	private List<String> regularPath;
	private int regularPathStep;

	private static final double randomWalkDirectionChangeProbability = 0.25;
	private int randomWalkDirection;

	private static final int maxStuckPoints = 10;
	private int stuckPoints = 0;

	// The index is for the path, the counter counts for every path and tells to
	// stop when unstucking has been called enough times
	private int unstuckingStepsIndex;
	private static final int maxStepsForUnstucking = 5;
	private int unstuckingStepsCounter;
	private List<String> unstuckingPath;

	private boolean finished = false;

	/**
	 * Agent whose behaviour it is
	 */
	private ExploreCoopAgent myAgent;

	private MapRepresentation myMap;
	private String myPosition;

	/**
	 * 
	 * @param myagent
	 * @param agentNames name of the agents to share the map with
	 */
	public ExploCoopBehaviour(ExploreCoopAgent myagent, List<String> agentnames) {
		super(myagent);

		this.myAgent = myagent;
		this.reloadMapAndPosition();

		this.regularPathStep = 0;

		this.stuckPoints = 0;
		this.unstuckingStepsIndex = 0;
		this.unstuckingStepsCounter = 0;

		Random rand = new Random();

		this.randomWalkDirection = rand.nextInt(4);
	}

	@Override
	public void action() {
		this.reloadMapAndPosition();

		if (this.myMap == null) {
			this.myAgent.createMap();
			this.reloadMapAndPosition();
		}

		// If the agent is only an exploration agent, no need to come back to this node
		// ever again

		if (this.myPosition != null && this.myAgent.getClass() == ExploreCoopAgent.class) {
			this.myMap.addNode(this.myPosition, MapAttribute.closed);
		}

		// for debugging purposes
		// String currentAgentName = this.myAgent.getLocalName();
		// List<String> closedNodes = this.myMap.getClosedNodes();
		// List<String> openNodes = this.myMap.getOpenNodes();
		// Set<String> resourcefulNodes = this.myMap.getResourcefulNodes();
		// boolean resourcefulNodesLeft = this.myMap.resourcefulNodesLeftForAgent(this.myPosition);

		if (this.myPosition != null &&
				!(this.myAgent instanceof CollectorCoopAgent && ((CollectorCoopAgent) this.myAgent).getLocked())) {

			this.exploreMap();
		}
	}

	private void exploreMap() {
		/**
		 * Just added here to let you see what the agent is doing, otherwise he will be
		 * too quick
		 */
		try {
			this.myAgent.doWait(ExploCoopBehaviour.waitingTime);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// 1) get the surrounding nodes and, if not in closedNodes, add them to open
		// nodes.

		this.reloadMapAndPosition();
		this.reloadNeighbourhood();

		// 2) while openNodes is not empty, continues.
		if (!this.myMap.hasOpenNode()) {
			// Explo finished
			this.finish();
		}

		else {
			// System.out.println(this.myAgent.getLocalName() + " trying to move");
			String nextNode = null;

			// do {
			// System.out.println("Open nodes left for agent " + this.myAgent.getLocalName()
			// + " : " + this.myMap.getOpenNodes().size());

			boolean triedRegularTooManyTimes = false;
			int trialCounter = 0;

			while (MapRepresentation.isPathNull(this.regularPath) || this.regularPathStep == this.regularPath.size()) {
				this.regularPathStep = 0;

				if (this.myAgent instanceof CollectorCoopAgent
						&& this.myMap.resourcefulNodesLeftForAgent(this.myPosition)) {
					this.regularPath = this.myMap.getShortestPathToClosestResourcefulNode(this.myPosition);
				}

				else { 
					this.regularPath = this.myMap.getShortestPathToClosestOpenNode(this.myPosition);
				}

				// if the search for a path to a resourceful node above has failed because unavailable with current map
				if( this.myAgent instanceof CollectorCoopAgent && this.myMap.resourcefulNodesLeftForAgent(this.myPosition) && 
					MapRepresentation.isPathNull(this.regularPath) ) {
						this.regularPath = this.myMap.getShortestPathToClosestOpenNode(this.myPosition);
				}

				trialCounter++;

				if (trialCounter > ExploCoopBehaviour.maxLoopTrials) {
					triedRegularTooManyTimes = true;
					break;
				}
			}

			Couple<String, String> last2Positions = this.myAgent.getLast2Positions();
			String lastPosition = last2Positions.getRight();
			String penultimatePosition = last2Positions.getLeft();

			if (MapRepresentation.isPathNull(this.regularPath)
					|| this.myPosition.equals(nextNode = regularPath.get(this.regularPathStep))
					|| (lastPosition != null &&
							(lastPosition.equals(this.myPosition)
									|| lastPosition.equals(nextNode)
									|| (penultimatePosition != null && penultimatePosition.equals(this.myPosition))))) {
				this.stuckPoints++;
			}

			boolean agentStuck = triedRegularTooManyTimes || stuckPoints > ExploCoopBehaviour.maxStuckPoints;

			// DEALING WITH STUCK SITUATIONS
			if (agentStuck || this.unstuckingStepsCounter > 0) {
				this.stuckPoints = 0;

				// If unstucking process has ended
				if (this.unstuckingStepsCounter >= ExploCoopBehaviour.maxStepsForUnstucking ||
						(unstuckingPath != null && this.myPosition == unstuckingPath.get(unstuckingPath.size() - 1))) {
					this.unstuckingStepsCounter = 0;
					this.unstuckingPath = null;
				}

				else if (this.unstuckingStepsCounter < ExploCoopBehaviour.maxStepsForUnstucking) {

					String far = (MapRepresentation.isPathNull(this.unstuckingPath)) ? null
							: this.unstuckingPath.get(this.unstuckingPath.size() - 1);

					boolean isPathValid = !MapRepresentation.isPathNull(this.unstuckingPath);
					boolean hasMoved = false;

					int unstuckingTrialCounter = 0;

					while (!isPathValid || far.equals(this.myPosition) || !hasMoved) {
						// String currentAgentName = this.myAgent.getLocalName(); // for debugging
						// purposes
						this.myPosition = this.myAgent.getCurrentPosition();

						// System.out.println(this.myAgent.getLocalName() + "called stuck loop ");

						far = this.myAgent.getMap().getRandomNode();
						this.unstuckingPath = this.myAgent.getMap().getAStarShortestPath(this.myPosition, far);
						this.unstuckingStepsIndex = 0;

						isPathValid = !MapRepresentation.isPathNull(this.unstuckingPath);

						if (isPathValid) {
							nextNode = unstuckingPath.get(this.unstuckingStepsIndex);

							// System.out.println(this.myAgent.getLocalName() + " stuck : " +
							// this.myPosition +
							// ", trying to escape to " + nextNode
							// + "\n\tCurrent unstucking path at step " + this.unstuckingStepsDone + " is "
							// + this.unstuckingPath);

							if (this.tryMove(nextNode)) {
								this.unstuckingStepsCounter++;
								this.unstuckingStepsIndex++;
								hasMoved = true;
							}
						}

						unstuckingTrialCounter++;

						if (unstuckingTrialCounter > ExploCoopBehaviour.maxLoopTrials) {
							this.randomWalk();
							return;
						}
					}

					// Reset original path and increase random walk probability
					this.regularPath = null;
				}

			}

			else {
				// this.randomWalkCurrentProbability = 0;

				// System.out.println(this.myAgent.getLocalName() + " acted in its normal
				// behaviour, moving from" + this.myPosition + " to " + nextNode);

				if (this.tryMove(nextNode)) {
					this.regularPathStep++;
				}
			}
			// } while(nextNode == null || this.myMap.getAStarShortestPath(this.myPosition,
			// nextNode)
			// == null || this.myMap.getAStarShortestPath(this.myPosition, nextNode).size()
			// != 1);
		}
	}

	private void reloadMapAndPosition() {
		this.myMap = this.myAgent.getMap();
		this.myPosition = this.myAgent.getCurrentPosition();
	}

	private boolean tryMove(String nextNode) {
		if(this.reloadNeighbourhood().contains(nextNode)) {
			this.myAgent.setNextDest(nextNode);
			this.myAgent.setLastPosition();

			//System.out.println(this.myAgent.getLocalName() + " trying move to " + nextNode);

			if (this.myAgent instanceof CollectorCoopAgent
					&& nextNode == ((CollectorCoopAgent) this.myAgent).getWaitingNode()) {
				return false;
			}

			boolean moveSuccessful = this.myAgent.moveTo(nextNode);

			boolean immediatelyCloseIfUnavailable = true; // Closes node anyway or not
			// boolean immediatelyCloseIfUnavailable = this.myAgent.getClass() ==
			// ExploreCoopAgent.class; // Closes node only if agent an is Exploration agent

			if (!moveSuccessful && immediatelyCloseIfUnavailable) {
				this.myAgent.getMap().addNode(nextNode, MapAttribute.closed);
			}

			return moveSuccessful;
		}

		return false;
	}

	private void randomWalk() {
		List<String> neighbourhood = this.reloadNeighbourhood();
		neighbourhood.remove(0);

		Random rand = new Random();

		double pDirectionChange = rand.nextDouble();
		String nextNode = null;

		if (pDirectionChange > 1 - randomWalkDirectionChangeProbability
				|| this.randomWalkDirection >= neighbourhood.size()) {
			this.randomWalkDirection = rand.nextInt(neighbourhood.size());
		}

		nextNode = neighbourhood.get(this.randomWalkDirection);

		// randomWalk means reseting everything else
		this.regularPath = null;
		this.unstuckingStepsCounter = 0;
		this.unstuckingPath = null;

		// System.out.println(this.myAgent.getLocalName() + " called random walk from "
		// + this.myAgent.getCurrentPosition() + ", moving to " + nextNode);

		this.tryMove(nextNode);
	}

	private List<String> reloadNeighbourhood() {
		this.reloadMapAndPosition();
		List<Couple<String, List<Couple<Observation, Integer>>>> lobs = this.myAgent.observe();

		// Cleaning neighbourhood everytime to avoid errors
		this.myAgent.cleanNeighbourhood();
		Iterator<Couple<String, List<Couple<Observation, Integer>>>> iter = lobs.iterator();

		while (iter.hasNext()) {
			String nodeId = iter.next().getLeft();
			this.myAgent.addNeighbourhood(nodeId);

			this.myMap.addNewNode(nodeId);

			// the node may exist, but not necessarily the edge
			if (this.myPosition != nodeId) {
				this.myMap.addEdge(this.myPosition, nodeId);
			}
		}

		return this.myAgent.getNeighbourhood();
	}

	private void finish() {
		this.finished = true;

		String role = (this.myAgent instanceof CollectorCoopAgent) ? "Collection" : "Exploration";

		System.out.println(
				this.myAgent.getLocalName() + " - " + role + " successufully done, exploration behaviour removed.");
	}

	@Override
	public boolean done() {
		return finished;
	}

}
