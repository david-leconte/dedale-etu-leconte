package eu.su.mas.dedaleEtu.mas.agents.dummies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dataStructures.tuple.Couple;
import debug.Debug;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.exploration.ExploCoopBehaviour;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;



/**
 * 
 * This Wumpus is harmless. It moves randomly and shift the resources it encounters
 * 
 * 
 * @author hc
 *
 */
public class DummyWumpusShiftAdapted extends AbstractDedaleAgent{


	/**
	 * 
	 */
	private static final long serialVersionUID = 2703609263614775545L;

	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1 set the agent attributes 
	 *	 		2 add the behaviours
	 *          
	 */
	protected void setup(){

		super.setup();

		List<Behaviour> lb=new ArrayList<Behaviour>();
		lb.add(new RandomShiftBehaviour(this));

		addBehaviour(new startMyBehaviours(this,lb));

		System.out.println("the  agent "+this.getLocalName()+ " is started");

	}

	/**
	 * This method is automatically called after doDelete()
	 */
	protected void takeDown(){
		super.takeDown();
	}

	protected void beforeMove(){
		super.beforeMove();
		//System.out.println("I migrate");
	}

	protected void afterMove(){
		super.afterMove();
		//System.out.println("I migrated");
	}


	/**************************************
	 * 
	 * 
	 * 				BEHAVIOURS
	 * 
	 * 
	 **************************************/


	public class RandomShiftBehaviour extends TickerBehaviour{

		private int MAXshiftDistance=6;
		private int MINshiftDistance=2;

		private boolean pause=false;

		/**
		 * When an agent choose to move
		 *  
		 */
		private static final long serialVersionUID = 9088209402507795289L;

		//private Environment realEnv;
		private int waitingTimeBeforeDropOff;
		private int currentWaitingTimeBeforeDromOff;
		private boolean grabbed;

		public RandomShiftBehaviour (final AbstractDedaleAgent myagent) {
			super(myagent,ExploCoopBehaviour.waitingTime * 2);
			Random r = new Random();
			waitingTimeBeforeDropOff=MINshiftDistance+r.nextInt(MAXshiftDistance);
			currentWaitingTimeBeforeDromOff=0;
			grabbed=false;

		}

		@Override
		public void onTick() {
			if (pause) {
				try {
					System.out.println("Press a key to release the "+this.myAgent.getLocalName());
					System.in.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
				pause=false;
			}else {


				String myPosition=((AbstractDedaleAgent) this.myAgent).getCurrentPosition();
				if (myPosition!=""){
					List<Couple<String,List<Couple<Observation,Integer>>>> lobs=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
					//System.out.println(this.myAgent.getLocalName()+" -- list of observables: "+lobs);

					//If I have treasure in my backPack and the waitingTime is reached
					if(grabbed && currentWaitingTimeBeforeDromOff==0){
						//drop it off and reinitialise the waiting time before dropOff.

						//TODO write the dropOff method and call it here
						((AbstractDedaleAgent) this.myAgent).dropOff();

						this.grabbed=false;
						this.currentWaitingTimeBeforeDromOff=this.waitingTimeBeforeDropOff;
					}else{	
						//Otherwise
						//try to grab what is available on the current position
						//System.out.println("I try to open the safe: "+((AbstractDedaleAgent) this.myAgent).openLock(Observation.ANY_TREASURE));
						//((AbstractDedaleAgent) this.myAgent).openLock(Observation.ANY_TREASURE);
						//list of observations associated to the currentPosition
						List<Couple<Observation,Integer>> lObservations= lobs.get(0).getRight();

						//example related to the use of the backpack for the treasure hunt
						Boolean b=false;
						for(Couple<Observation,Integer> o:lObservations){
							try {
								switch (o.getLeft()) {



								//						System.out.println("Value of the treasure on the current position: "+o.getLeft() +": "+ o.getRight());
								//						System.out.println("The agent grabbed :"+((AbstractDedaleAgent) this.myAgent).pick());
								//						System.out.println("the remaining backpack capacity is: "+ ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace());
								//						b=true;
								//						break;

								case GOLD:
									//System.out.println("My treasure type is : "+((AbstractDedaleAgent) this.myAgent).getMyTreasureType());
									//System.out.println("My current backpack capacity is:"+ ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace());
									//((AbstractDedaleAgent)this.myAgent).openLock(Observation.GOLD);
									int valGrabbed=((AbstractDedaleAgent)this.myAgent).pick();
									//System.out.println("The agent grabbed :"+valGrabbed);

									System.out.println("the remaining backpack capacity is: "+ ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace());
									b=true;
									if (valGrabbed>0 && !grabbed){
										grabbed=true;
										this.currentWaitingTimeBeforeDromOff=this.waitingTimeBeforeDropOff;
									}
									break;
								case DIAMOND:
									//System.out.println("My treasure type is : "+((AbstractDedaleAgent) this.myAgent).getMyTreasureType());
									//System.out.println("My current backpack capacity is:"+ ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace());	//System.out.println("The agent grabbed :"+((mas.abstractAgent)this.myAgent).pick());
									int valdiamGrabbed=((AbstractDedaleAgent)this.myAgent).pick();
									//System.out.println("The agent grabbed :"+valdiamGrabbed);	
									System.out.println("the remaining backpack capacity is: "+ ((AbstractDedaleAgent) this.myAgent).getBackPackFreeSpace());
									b=true;
									if (valdiamGrabbed>0 && !grabbed){
										grabbed=true;
										this.currentWaitingTimeBeforeDromOff=this.waitingTimeBeforeDropOff;
									}
									break;
								default:
									break;
								}
							} catch(IndexOutOfBoundsException e) {
								System.out.println(this.myAgent.getLocalName() + " \"start pick\" error, execution continues");
							}
						}
						//If the agent picked (part of) the treasure
						//					if (b){
						//						List<Couple<String,List<Couple<Observation,Integer>>>> lobs2=((AbstractDedaleAgent)this.myAgent).observe();//myPosition
						//						System.out.println("State of the observations after picking "+lobs2);
						//					}

						//decrement the currentWaiting time
						if(grabbed){
							this.currentWaitingTimeBeforeDromOff--;
							//Little pause to allow you to follow what is going on
							//					try {
							//						System.out.println("Press a key to allow the agent "+this.myAgent.getLocalName() +" to execute its next move");
							//						System.in.read();
							//					} catch (IOException e) {
							//						e.printStackTrace();
							//					}
						};
					}

					//move
					Random r= new Random();
					int moveId=1+r.nextInt(lobs.size()-1);
					((AbstractDedaleAgent) this.myAgent).moveTo(lobs.get(moveId).getLeft());

				}else{
					Debug.error("Empty posit");
				}

			}
		}


	}
}