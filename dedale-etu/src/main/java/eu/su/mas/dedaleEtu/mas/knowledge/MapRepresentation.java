package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.graphstream.algorithm.AStar;
import org.graphstream.algorithm.AStar.DistanceCosts;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;

import dataStructures.serializableGraph.SerializableNode;
import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Observation;
import javafx.application.Platform;

/**
 * This simple topology representation only deals with the graph, not its
 * content.</br>
 * The knowledge representation is not well written (at all), it is just given
 * as a minimal example.</br>
 * The viewer methods are not independent of the data structure, and the
 * dijkstra is recomputed every-time.
 * 
 * @author hc
 */
public class MapRepresentation implements Serializable {

	/**
	 * A node is open, closed, or agent
	 * 
	 * @author hc
	 *
	 */

	public enum MapAttribute {
		agent, open, closed;

	}

	private static final long serialVersionUID = -1333959882640838272L;

	/*********************************
	 * Parameters for graph rendering
	 ********************************/

	private String defaultNodeStyle = "node {" + "fill-color: black;"
			+ " size-mode:fit;text-alignment:under; text-size:14;text-color:white;text-background-mode:rounded-box;text-background-color:black;}";
	private String nodeStyle_open = "node.agent {" + "fill-color: forestgreen;" + "}";
	private String nodeStyle_agent = "node.open {" + "fill-color: blue;" + "}";
	private String nodeStyle = defaultNodeStyle + nodeStyle_agent + nodeStyle_open;

	private Graph g; // data structure non serializable
	private Viewer viewer; // ref to the display, non serializable
	private Integer nbEdges;// used to generate the edges ids

	private SerializableSimpleGraph<String, MapAttribute> sg;// used as a temporary dataStructure during migration

	private Map<String, Map<String, Object>> resourcefulNodes;

	public MapRepresentation(boolean opengui) {
		// System.setProperty("org.graphstream.ui.renderer","org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		System.setProperty("org.graphstream.ui", "javafx");
		this.g = new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet", nodeStyle);

		Platform.runLater(() -> {
			if (opengui) {
				openGui();
			}
		});
		// this.viewer = this.g.display();

		this.nbEdges = 0;
		this.resourcefulNodes = new HashMap<String, Map<String, Object>>();
	}

	public MapRepresentation() {
		this(true);
	}

	/**
	 * Add or replace a node and its attribute
	 * 
	 * @param id
	 * @param closed
	 */
	public synchronized void addNode(String id, MapAttribute closed) {
		Node n;
		if (this.g.getNode(id) == null) {
			n = this.g.addNode(id);
		} else {
			n = this.g.getNode(id);
		}
		n.clearAttributes();
		n.setAttribute("ui.class", closed.toString());
		n.setAttribute("ui.label", id);
	}

	/**
	 * Add a node to the graph. Do nothing if the node already exists.
	 * If new, it is labeled as open (non-visited)
	 * 
	 * @param id id of the node
	 * @return true if added
	 */
	public synchronized boolean addNewNode(String id) {
		if (this.g.getNode(id) == null) {
			addNode(id, MapAttribute.open);
			return true;
		}
		return false;
	}

	/**
	 * Add an undirect edge if not already existing.
	 * 
	 * @param idNode1
	 * @param idNode2
	 */
	public synchronized void addEdge(String idNode1, String idNode2) {
		this.nbEdges++;
		try {
			this.g.addEdge(this.nbEdges.toString(), idNode1, idNode2);
		} catch (IdAlreadyInUseException e1) {
			System.err.println("ID existing");
			System.exit(1);
		} catch (EdgeRejectedException e2) {
			this.nbEdges--;
		} catch (ElementNotFoundException e3) {

		}
	}

	/**
	 * Compute the shortest Path from idFrom to IdTo. The computation is currently
	 * not very efficient
	 * 
	 * @param idFrom id of the origin node
	 * @param idTo   id of the destination node
	 * @return the list of nodes to follow
	 */
	public synchronized List<String> getShortestPath(String idFrom, String idTo) {
		List<String> shortestPath = new ArrayList<String>();

		Dijkstra dijkstra = new Dijkstra();// number of edge

		dijkstra.init(g);

		dijkstra.setSource(g.getNode(idFrom));

		dijkstra.compute();// compute the distance to all nodes from idFrom

		List<Node> path = dijkstra.getPath(g.getNode(idTo)).getNodePath(); // the shortest path from idFrom to idTo

		Iterator<Node> iter = path.iterator();

		while (iter.hasNext()) {
			shortestPath.add(iter.next().getId());
		}

		dijkstra.clear();
		if (shortestPath.isEmpty()) {// The openNode is not currently reachable
			return null;
		} else {
			shortestPath.remove(0);// remove the current position
		}
		return shortestPath;
	}

	/**
	 * Compute the shortest Path from idFrom to IdTo using the AStar Algorithm
	 * 
	 * @param idFrom id of the origin node
	 * @param idTo   id of the destination node
	 * @return the list of nodes to follow
	 */
	public synchronized List<String> getAStarShortestPath(String idFrom, String idTo) {
		List<String> shortestPath = new ArrayList<String>();

		AStar astaralgo = new AStar(g);

		astaralgo.compute(idFrom, idTo);// compute the distance to all nodes from idFrom

		Path path = astaralgo.getShortestPath();
		List<Node> pathList = null;

		if (path != null) {
			pathList = path.getNodePath();
		} // the shortest path from idFrom to idTo
		else {
			return getShortestPath(idFrom, idTo);
		}

		Iterator<Node> iter = pathList.iterator();

		while (iter.hasNext()) {
			shortestPath.add(iter.next().getId());
		}

		if (shortestPath.isEmpty()) {// The openNode is not currently reachable
			return null;
		} else {
			shortestPath.remove(0);// remove the current position
		}
		return shortestPath;
	}

	public List<String> getShortestPathToClosestOpenNode(String myPosition) {
		// 1) Get all openNodes
		List<String> opennodes = getOpenNodes();
		// 1.2) We don't want the path to where we are now that makes no sense
		opennodes.remove(myPosition);

		// 2) select the closest one

		if (opennodes.size() == 1) {
			return getAStarShortestPath(myPosition, opennodes.get(0));
		}

		List<Couple<String, Integer>> lc = opennodes.stream()
				.map(on -> (getShortestPath(myPosition, on) != null)
						? new Couple<String, Integer>(on, getShortestPath(myPosition, on).size())
						: new Couple<String, Integer>(on, Integer.MAX_VALUE))// some nodes my be unreachable if the
																				// agents do not share at least one
																				// common node.
				.collect(Collectors.toList());

		Optional<Couple<String, Integer>> closest = lc.stream().min(Comparator.comparing(Couple::getRight));
		// 3) Compute shorterPath
		if(!closest.isEmpty()) {
			return getAStarShortestPath(myPosition, closest.get().getLeft());
		}
		else return null;
	}

	public List<String> getShortestPathToClosestResourcefulNode(String myPosition, Observation treasureType, 
		int lockpicking, int capacity, int strength) {
		// 1.2) We don't want the path to where we are now that makes no sense
		ConcurrentHashMap<String, Map<String, Object>> resourcefulNodesMap = new ConcurrentHashMap<String, Map<String, Object>>(this.resourcefulNodes);
		resourcefulNodesMap.remove(myPosition);

		for (Map.Entry<String, Map<String, Object>> entry : resourcefulNodesMap.entrySet()) {
			if (this.getClosedNodes().contains(entry.getKey())) {
				resourcefulNodesMap.remove(entry.getKey());
			}
		}

		String finalNode = null;
		String testedNodeId = null;
		Map<String, Object> testedNodeProperties = null;
		List<String> path = null;

		// 2) select the closest one
		if(resourcefulNodesMap.isEmpty()) finalNode = null;

		else if (resourcefulNodesMap.size() == 1) {
			String onlyNodeId = resourcefulNodesMap.entrySet().stream().findFirst().get().getKey();
			Map<String, Object> onlyNode= resourcefulNodesMap.entrySet().stream().findFirst().get().getValue();

			testedNodeId = onlyNodeId;
			testedNodeProperties = onlyNode;

			if((int) onlyNode.get("LOCK") <= lockpicking && (int)  onlyNode.get("QUANTITY") <= capacity &&
				(int) onlyNode.get("STRENGTH") <= strength &&
				(treasureType == Observation.ANY_TREASURE || treasureType == onlyNode.get("TYPE"))) {

				finalNode = onlyNodeId;
			}
		}

		else {
			boolean continueTrying = true;
			int testedNodes = 0;

			// All resourceful nodes aren't available at once, keep looping until one is available (== path not null)

			while (continueTrying && testedNodes < resourcefulNodesMap.size()) {
				List<String> resourcefulNodesList = new ArrayList<String>(resourcefulNodesMap.keySet());

				List<Couple<String, Integer>> lc = resourcefulNodesList.stream()
						.map(on -> (getShortestPath(myPosition, on) != null)
								? new Couple<String, Integer>(on, getShortestPath(myPosition, on).size())
								: new Couple<String, Integer>(on, Integer.MAX_VALUE))// some nodes my be unreachable if the
																						// agents do not share at least one
																						// common node.
						.collect(Collectors.toList());

				Optional<Couple<String, Integer>> closest = lc.stream().min(Comparator.comparing(Couple::getRight));
				// 3) Compute shorterPath

				String nodeId = closest.get().getLeft();
				Map<String, Object> nodeProperties = resourcefulNodesMap.get(nodeId);
				testedNodeId = nodeId;
				testedNodeProperties = nodeProperties;

				if((int) nodeProperties.get("LOCK") <= lockpicking && 
					(int) nodeProperties.get("QUANTITY") <= capacity && 
					(int) nodeProperties.get("STRENGTH") <= strength && 
					(treasureType == Observation.ANY_TREASURE || treasureType == nodeProperties.get("TYPE"))){
					path = getAStarShortestPath(myPosition, nodeId);

					if (!MapRepresentation.isPathNull(path)) {
						continueTrying = false;
					}
				}
				
				testedNodes++;
			}
		}

		if(finalNode != null && path == null) {
			path = getAStarShortestPath(myPosition, finalNode);
			System.out.println("Agent: \n{QUANTITY="+ capacity +", LOCK="+ lockpicking +", STRENGTH="+ strength +", TYPE="+ treasureType+"}" +
			"\nNode:\n" + testedNodeProperties + 
			"\n\tId : " + testedNodeId +"\n\tPath : " + path + "\n\n");
		}

		return path;
	}

	// public List<String> getShortestPathToFarthestOpenNode(String myPosition) {
	// //1) Get all openNodes
	// List<String> opennodes=getOpenNodes();
	// // 1.2) We don't want the path to where we are now that makes no sense
	// opennodes.remove(myPosition);

	// //2) select the farthest one
	// List<Couple<String,Integer>> lc=
	// opennodes.stream()
	// .map(on -> (getShortestPath(myPosition,on)!=null)? new Couple<String,
	// Integer>(on,getShortestPath(myPosition,on).size()): new Couple<String,
	// Integer>(on,Integer.MIN_VALUE))//some nodes my be unreachable if the agents
	// do not share at least one common node.
	// .collect(Collectors.toList());

	// Optional<Couple<String,Integer>> farthest =
	// lc.stream().max(Comparator.comparing(Couple::getRight));
	// //3) Compute shorterPath

	// return getAStarShortestPath(myPosition, farthest.get().getLeft());
	// }

	public static boolean isPathNull(List<String> path) {
		return path == null || path.size() <= 0;
	}

	public List<String> getOpenNodes() {
		List<String> openNodes = this.g.nodes()
				.filter(x -> x.getAttribute("ui.class") == MapAttribute.open.toString())
				.map(Node::getId)
				.collect(Collectors.toList());

		// System.out.println(openNodes);

		return openNodes;
	}

	public List<String> getClosedNodes() {
		return this.g.nodes()
				.filter(x -> x.getAttribute("ui.class") == MapAttribute.closed.toString())
				.map(Node::getId)
				.collect(Collectors.toList());
	}

	public String getRandomNode() {
		List<String> nodesList = this.getOpenNodes();
		if(nodesList.isEmpty()) nodesList.addAll(this.getClosedNodes());
		Random rand = new Random();
		return nodesList.get(rand.nextInt(nodesList.size()));
	}

	/**
	 * Before the migration we kill all non serializable components and store their
	 * data in a serializable form
	 */
	public void prepareMigration() {
		serializeGraphTopology();

		closeGui();

		this.g = null;
	}

	/**
	 * Before sending the agent knowledge of the map it should be serialized.
	 */
	private void serializeGraphTopology() {
		this.sg = new SerializableSimpleGraph<String, MapAttribute>();
		Iterator<Node> iter = this.g.iterator();
		while (iter.hasNext()) {
			Node n = iter.next();
			sg.addNode(n.getId(), MapAttribute.valueOf((String) n.getAttribute("ui.class")));
		}
		Iterator<Edge> iterE = this.g.edges().iterator();
		while (iterE.hasNext()) {
			Edge e = iterE.next();
			Node sn = e.getSourceNode();
			Node tn = e.getTargetNode();
			sg.addEdge(e.getId(), sn.getId(), tn.getId());
		}
	}

	public synchronized SerializableSimpleGraph<String, MapAttribute> getSerializableGraph() {
		serializeGraphTopology();
		return this.sg;
	}

	/**
	 * After migration we load the serialized data and recreate the non serializable
	 * components (Gui,..)
	 */
	public synchronized void loadSavedData() {

		this.g = new SingleGraph("My world vision");
		this.g.setAttribute("ui.stylesheet", nodeStyle);

		openGui();

		Integer nbEd = 0;
		for (SerializableNode<String, MapAttribute> n : this.sg.getAllNodes()) {
			this.g.addNode(n.getNodeId()).setAttribute("ui.class", n.getNodeContent().toString());
			for (String s : this.sg.getEdges(n.getNodeId())) {
				this.g.addEdge(nbEd.toString(), n.getNodeId(), s);
				nbEd++;
			}
		}
		System.out.println("Loading done");
	}

	/**
	 * Method called before migration to kill all non serializable graphStream
	 * components
	 */
	private synchronized void closeGui() {
		// once the graph is saved, clear non serializable components
		if (this.viewer != null) {
			// Platform.runLater(() -> {
			try {
				this.viewer.close();
			} catch (NullPointerException e) {
				System.err.println(
						"Bug graphstream viewer.close() work-around - https://github.com/graphstream/gs-core/issues/150");
			}
			// });
			this.viewer = null;
		}
	}

	/**
	 * Method called after a migration to reopen GUI components
	 */
	private synchronized void openGui() {
		this.viewer = new FxViewer(this.g, FxViewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);// GRAPH_IN_GUI_THREAD)
		viewer.enableAutoLayout();
		viewer.setCloseFramePolicy(FxViewer.CloseFramePolicy.CLOSE_VIEWER);
		viewer.addDefaultView(true);

		g.display();
	}

	public void mergeMap(SerializableSimpleGraph<String, MapAttribute> sgreceived, boolean allNew) {
		// System.out.println("You should decide what you want to save and how");
		// System.out.println("We currently blindy add the topology");

		for (SerializableNode<String, MapAttribute> n : sgreceived.getAllNodes()) {
			// System.out.println(n);
			boolean alreadyIn = false;
			// 1 Add the node
			Node newnode = null;
			try {
				newnode = this.g.addNode(n.getNodeId());
			} catch (IdAlreadyInUseException e) {
				alreadyIn = true;
				// System.out.println("Already in"+n.getNodeId());
			}
			if (!alreadyIn) {
				newnode.setAttribute("ui.label", newnode.getId());

				if(allNew) newnode.setAttribute("ui.class", MapAttribute.open.toString());
				else newnode.setAttribute("ui.class", n.getNodeContent().toString());
			} else {
				newnode = this.g.getNode(n.getNodeId());
				// 3 check its attribute. If it is below the one received, update it.
				if (((String) newnode.getAttribute("ui.class")) == MapAttribute.closed.toString()
						|| n.getNodeContent().toString() == MapAttribute.closed.toString()) {
					newnode.setAttribute("ui.class", MapAttribute.closed.toString());
				}
			}
		}

		// 4 now that all nodes are added, we can add edges
		for (SerializableNode<String, MapAttribute> n : sgreceived.getAllNodes()) {
			for (String s : sgreceived.getEdges(n.getNodeId())) {
				addEdge(n.getNodeId(), s);
			}
		}
		// System.out.println("Merge done");
	}

	/**
	 * 
	 * @return true if there exist at least one openNode on the graph
	 */
	public boolean hasOpenNode() {
		return (this.g.nodes()
				.filter(n -> n.getAttribute("ui.class") == MapAttribute.open.toString())
				.findAny()).isPresent();
	}

	public void putResourcefulNode(String node, Observation treasureType, 
		int lockpicking, int quantity, int strength) {
		if(treasureType == Observation.DIAMOND || treasureType == Observation.GOLD) {
			Map<String, Object> nodeProperties = new HashMap<String, Object>();

			nodeProperties.put("LOCK", lockpicking);
			nodeProperties.put("QUANTITY", quantity);
			nodeProperties.put("STRENGTH", strength);
			nodeProperties.put("TYPE", treasureType);

			this.resourcefulNodes.put(node, nodeProperties);
		}
	}

	public void removeResourcefulNode(String node) {
		if (this.resourcefulNodes.containsKey(node)) {
			this.resourcefulNodes.remove(node);
		}
	}

	public Map<String, Map<String, Object>> getResourcefulNodes() {
		return this.resourcefulNodes;
	}

	public Map<String, Map<String, Object>> mergeResourcefulNodes(Map<String, Map<String, Object>>  sentNodes) {
		for (Map.Entry<String, Map<String, Object>>  entry : sentNodes.entrySet()) {
			if (!this.resourcefulNodes.containsKey(entry.getKey()) && !this.getClosedNodes().contains(entry.getKey())) {
				this.addNewNode(entry.getKey());
				String nodeId = entry.getKey();
				Map<String, Object> nodeProperties = entry.getValue();

				this.putResourcefulNode(nodeId, 
					(Observation) nodeProperties.get("TYPE"), 
					(int) nodeProperties.get("LOCK"), 
					(int) nodeProperties.get("QUANTITY"),
					(int) nodeProperties.get("STRENGTH")
				);
			}
		}

		return this.resourcefulNodes;
	}

	public boolean resourcefulNodesLeftForAgent(String position) {
		for (Map.Entry<String, Map<String, Object>> entry : this.resourcefulNodes.entrySet()) {
			if (entry.getKey() != position && !this.getClosedNodes().contains(entry.getKey())) {
				return true;
			}
		}

		return false;
	}
}