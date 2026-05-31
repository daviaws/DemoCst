/*****************************************************************************
 * Copyright 2007-2015 DCA-FEEC-UNICAMP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Contributors:
 *    Klaus Raizer, Andre Paraense, Ricardo Ribeiro Gudwin
 *****************************************************************************/

package codelets.behaviors;

import java.awt.Point;
import java.awt.geom.Point2D;

import org.json.JSONException;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.Memory;
import br.unicamp.cst.core.entities.MemoryContainer;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.representation.idea.Idea;
import ws3dproxy.model.Thing;

public class GoToClosestApple extends Codelet {

	private Memory closestAppleMO;
	private Memory selfInfoMO;
	private MemoryContainer legsMO;
	private int creatureBasicSpeed;
	private double reachDistance;

	private static final double FUEL_THRESHOLD = 300.0;

	public GoToClosestApple(int creatureBasicSpeed, int reachDistance) {
		this.creatureBasicSpeed=creatureBasicSpeed;
		this.reachDistance=reachDistance;
                this.name = "GoToClosestApple";
	}

	@Override
	public void accessMemoryObjects() {
		closestAppleMO=(MemoryObject)this.getInput("CLOSEST_APPLE");
		selfInfoMO=(MemoryObject)this.getInput("INNER");
		legsMO=(MemoryContainer)this.getOutput("LEGS");
	}

	@Override
	public void proc() {
		// Find distance between creature and closest apple
		//If far, go towards it
		//If close, stops

                Thing closestApple = (Thing) closestAppleMO.getI();
                Idea cis = (Idea) selfInfoMO.getI();

		double fuel = (double) cis.get("fuel").getValue();
		if (fuel >= FUEL_THRESHOLD) {
			activation = 0.0;
			legsMO.setI("", activation, name);
			return;
		}

		if(closestApple != null)
		{
			double appleX=0;
			double appleY=0;
			try {
                                appleX = closestApple.getCenterPosition().getX();
                                appleY = closestApple.getCenterPosition().getY();

			} catch (Exception e) {
				e.printStackTrace();
			}
                        
			double selfX=(double)cis.get("position.x").getValue();
			double selfY=(double)cis.get("position.y").getValue();

			Point2D pApple = new Point();
			pApple.setLocation(appleX, appleY);

			Point2D pSelf = new Point();
			pSelf.setLocation(selfX, selfY);

			double distance = pSelf.distance(pApple);
			//JSONObject message=new JSONObject();
                        Idea message = Idea.createIdea("message","", Idea.guessType("Property",null,1.0,0.5));
			try {
				if(distance>reachDistance){ //Go to it
                                        message.add(Idea.createIdea("ACTION","GOTO", Idea.guessType("Property",null,1.0,0.5)));
                                        message.add(Idea.createIdea("X",(int)appleX, Idea.guessType("Property",null,1.0,0.5)));
                                        message.add(Idea.createIdea("Y",(int)appleY, Idea.guessType("Property",null,1.0,0.5)));
                                        message.add(Idea.createIdea("SPEED",creatureBasicSpeed, Idea.guessType("Property",null,1.0,0.5)));
                                        activation=1.0;

				}else{//Stop
                                        message.add(Idea.createIdea("ACTION","GOTO", Idea.guessType("Property",null,1.0,0.5)));
                                        message.add(Idea.createIdea("X",(int)appleX, Idea.guessType("Property",null,1.0,0.5)));
                                        message.add(Idea.createIdea("Y",(int)appleY, Idea.guessType("Property",null,1.0,0.5)));
                                        message.add(Idea.createIdea("SPEED",0, Idea.guessType("Property",null,1.0,0.5)));
                                        activation=0.5;
				}
				legsMO.setI(toJson(message),activation,name);
			} catch (JSONException e) {
				e.printStackTrace();
			}	
		}
                else {
                    activation=0.0;
                    legsMO.setI("",activation,name);
                }
                
	}//end proc
        
        @Override
        public void calculateActivation() {
        
        }
        
        String toJson(Idea i) {
            String q = "\"";
            String out = "{";
            String val;
            int ii=0;
            for (Idea il : i.getL()) {
                if (il.getL().isEmpty()) {
                    if (il.isNumber()) val = il.getValue().toString();
                    else val = q+il.getValue()+q;
                }
                else val = toJson(il);
                if (ii == 0) out += q+il.getName()+q+":"+val;
                else out += ","+q+il.getName()+q+":"+val;
                ii++;
            }
            out += "}";
            return out;
        }

}
