package eu.su.mas.dedale.gui;


import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;

import org.graphstream.ui.fx_viewer.FxViewPanel;

import eu.su.mas.dedale.mas.agent.knowledge.AgentObservableElement;
import eu.su.mas.dedaleEtu.princ.ConfigurationFile;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MyController {

	private String keyPressed="";
	private boolean iskeyPressed=false;
	private KeyboardObservable keyPressedObserved= new KeyboardObservable();
	
	// When user click on myButton
	// this method will be called.

	//Game;
	@FXML private MenuItem configure;
	@FXML private MenuItem exit;

	//Map
	@FXML private MenuItem loadtopology;
	@FXML private MenuItem createtopoloy;
	@FXML private MenuItem savetopology;

	//Help;
	@FXML private MenuItem forum;
	@FXML private MenuItem tchat;
	@FXML private MenuItem website;
	@FXML private MenuItem about;

	//right pannel
	@FXML private AnchorPane right;

	//leftpanel
	/**
	 * Label related to communication
	 * 
	 * nbSent > nbReceived as non reachable agents --> lost messages
	 */

	@FXML private Label msgSent;
	@FXML private Label msgReceived;
	@FXML private TableView<AgentObservableElement> observeTab;


	/**************
	 * Game menu
	 *************/
	@FXML
	private void dedaleConfigure(ActionEvent event) {

	}

	@FXML
	private void dedaleStart(ActionEvent event) {

	}	

	@FXML
	private void handleExitAction(ActionEvent event) {
		System.out.println("exiting");
		System.exit(0);
		//Platform.exit();
	}

	/**@FXML
	public void dedaleHelp(ActionEvent event){
		System.out.println("Dedale site");
		openUrl("https://dedale.gitlab.io/");
		System.out.println("Post");
	}
	 **/


	/**************
	 * Map menu
	 *************/

	@FXML
	private void dedaleLoadTopo(ActionEvent event) {
		System.out.println("Not yet implemented through the GUI but you can do it in the configuration file");
	}

	@FXML
	private void dedaleCreateTopo(ActionEvent event) {
		System.out.println("Create a topology");
		openUrl("https://dedale.gitlab.io/page/tutorial/configureenv/");
	}	

	@FXML
	private void dedaleSaveTopo(ActionEvent event) {
		System.out.println("Not yet implemented through the GUI");
	}



	/**************
	 * Help menu
	 *************/


	@FXML
	public void dedaleAbout(ActionEvent event){
		System.out.println("About Dedale");
		openUrl("https://dedale.gitlab.io/page/about/");
	}

	@FXML
	public void dedaleWebsite(ActionEvent event){
		System.out.println("Dedale website");
		openUrl("https://dedale.gitlab.io/");
	}

	@FXML
	public void dedaleTchat(ActionEvent event){
		System.out.println("Discord server");
		openUrl("https://discord.gg/JZVz6sR");
	}

	@FXML
	public void dedaleForum(ActionEvent event){
		System.out.println("Dedale forum");

	}



	/**
	 * Open a browser tab with the uri given in parameter. Launch a new thread to be javafx compliant.
	 * @param uri
	 */
	private void openUrl(String uri) {
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			new Thread(() -> {
				try {
					Desktop.getDesktop().browse(new URI(uri));
				} catch (IOException | URISyntaxException e) {
					e.printStackTrace();
				}
			}).start();
		}
	}

	/**
	 * Trying to add the graph ref to the scene
	 * @param truc
	 */
	@SuppressWarnings("restriction")
	public synchronized void setGraph(FxViewPanel truc) {
		System.out.println(truc.scaleShapeProperty());
		right.getChildren().add(truc);
		truc.setScaleShape(true);
		truc.prefWidthProperty().bind(right.widthProperty());
		truc.prefHeightProperty().bind(right.heightProperty());		
	}


	/***************************
	 * 		MAS state feedback
	 ***************************/

	public synchronized void addToSentMessages(int n) {
		String[] tmp=msgSent.getText().split("=");
		int n1=Integer.parseInt(tmp[1].replace(" ",""))+n;
		msgSent.setText("nbSent = "+n1);
	}

	public synchronized void addToTransmittedMessages(int n) {
		String[] tmp=msgReceived.getText().split("=");
		int n1=Integer.parseInt(tmp[1].replace(" ",""))+n;
		msgReceived.setText("nbReceived = "+n1);
	}

	public synchronized void connectGuiWithAgentObservable(AgentObservableElement e) {

		this.observeTab.getItems().add(e);
		//ol.add(e);
		//ObservableList<AgentObservableElement>ol1=	FXCollections.observableArrayList(e);
		//ObservableList<AgentObservableElement> ol2=	FXCollections.concat(ol,ol1);
		//this.observeTab.setItems(ol1);
	}

	/**
	 * Method used to bypass a non understood bug.
	 * The listener cascade (agent 2 gs ooa 2 gui) is not working for the GUI step for an unknown reason.
	 * The Gui seems not to receive the changes in ooa. 
	 * But when I force a change in the list everything is updated. Which means the links are ok, just the links are ok.
	 * The trigger are just not executed.
	 * 
	 * @param e
	 */
	public synchronized void updateTableGui(AgentObservableElement e) {
		ObservableList<AgentObservableElement>ol=this.observeTab.getItems();
		ol.set(ol.indexOf(e),e);

		try {
			this.wait(250);
			this.logJSONStateOfAgents(ol);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private void logJSONStateOfAgents(ObservableList<AgentObservableElement>ol) {
		if(ol.size() <= 0) return;

		JSONArray data = new JSONArray();

		for(AgentObservableElement element : ol) {
			JSONObject item = new JSONObject();
			item.put("agentName", element.getAgentName());
			item.put("diamond", element.getCurrentDiamondValue());
			item.put("sentAndDelivered", element.getNbMsgSentAndDelivered());
			item.put("undelivered", element.getNbMsgSent() - element.getNbMsgSentAndDelivered());
			item.put("gold", element.getCurrentGoldValue());

			data.put(item);
		}

		File jsonFile = new File(ConfigurationFile.SAVE_PATH);

		try {
			jsonFile.createNewFile();
			Writer fw = new BufferedWriter(new FileWriter(jsonFile, false));

			fw.write(data.toString(4));
			fw.flush();
			fw.close();
		} catch (JSONException | IOException e) {
			System.out.println("Couldn't write save file.");
			e.printStackTrace();
		}
	}


	public synchronized void init() {
		//this.observeTab = new TableView();
		System.out.println(this.observeTab);
		TableColumn<AgentObservableElement, String> column1 = new TableColumn<>("Agent");
		column1.setCellValueFactory(new PropertyValueFactory<>("agentName"));

		TableColumn<AgentObservableElement, Integer> column2 = new TableColumn<>("Gold");
		column2.setCellValueFactory(new PropertyValueFactory<>("currentGoldValue"));

		TableColumn<AgentObservableElement, Integer> column3 = new TableColumn<>("Diamond");
		column3.setCellValueFactory(new PropertyValueFactory<>("currentDiamondValue"));

		// Set Sort type for userName column
		column1.setSortType(TableColumn.SortType.ASCENDING);


		observeTab.getColumns().add(column1);
		observeTab.getColumns().add(column2);
		observeTab.getColumns().add(column3);
	}


	/***********************
	 * User controlled agent
	 ***********************/

	@FXML
	public synchronized void keyPressed(KeyEvent keyEvent) {
		switch (keyEvent.getCode()) {
		case N:
			System.out.println("The key N is pressed");
			
			this.iskeyPressed=true;
			this.keyPressed="N";
			this.keyPressedObserved.setKeyPressed("N");
			break;
		case O:
			System.out.println("The key O is pressed");
			this.iskeyPressed=true;
			this.keyPressed="O";
			this.keyPressedObserved.setKeyPressed("O");
			break;
		default:
			break;
		}
	}
	
	public KeyboardObservable getKeyObservable() {
		return this.keyPressedObserved;
	}

}