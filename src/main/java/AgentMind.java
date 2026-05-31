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

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.Memory;
import br.unicamp.cst.core.entities.Mind;
import br.unicamp.cst.representation.idea.Idea;
import codelets.behaviors.EatClosestApple;
import codelets.behaviors.Forage;
import codelets.behaviors.GoToClosestApple;
import codelets.behaviors.LeafletSelectorCodelet;
import codelets.motor.HandsActionCodelet;
import codelets.motor.LegsActionCodelet;
import codelets.perception.AppleDetector;
import codelets.perception.ClosestAppleDetector;
import codelets.perception.DeliverySpotDetector;
import codelets.perception.JewelDetector;
import codelets.perception.LeafletReader;
import codelets.sensors.InnerSense;
import codelets.sensors.Vision;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import support.PlanStep;
import ws3dproxy.model.Leaflet;
import ws3dproxy.model.Thing;

/**
 *
 * @author rgudwin
 */
public class AgentMind extends Mind {
    
    private static int creatureBasicSpeed=3;
    private static int reachDistance=50;
    public ArrayList<Codelet> behavioralCodelets = new ArrayList<Codelet>();
    
    public AgentMind(Environment env) {
                super();
                
                // Create CodeletGroups and MemoryGroups for organizing Codelets and Memories
                createCodeletGroup("Sensory");
                createCodeletGroup("Motor");
                createCodeletGroup("Perception");
                createCodeletGroup("Behavioral");
                createMemoryGroup("Sensory");
                createMemoryGroup("Motor");
                createMemoryGroup("Working");
                
                // Declare Memory Objects
	        Memory legsMO;  // This Memory is going to be a MemoryContainer
	        Memory handsMO;
                Memory visionMO;
                Memory innerSenseMO;
                Memory closestAppleMO;
                Memory knownApplesMO;
                Memory knownJewelsMO;
                // Leaflet Planning
                Memory targetLeafletMO;
                Memory deliberativePhaseMO;
                Memory planStepsMO;

                //Initialize Memory Objects
                legsMO=createMemoryContainer("LEGS");
                registerMemory(legsMO,"Motor");
		handsMO=createMemoryObject("HANDS", "");
                registerMemory(handsMO,"Motor");
                List<Thing> vision_list = Collections.synchronizedList(new ArrayList<Thing>());
		visionMO=createMemoryObject("VISION",vision_list);
                registerMemory(visionMO,"Sensory");
                //CreatureInnerSense cis = new CreatureInnerSense();
                Idea cis = Idea.createIdea("cis","", Idea.guessType("AbstractObject",null,1.0,0.5));
                cis.add(Idea.createIdea("cis.pitch", 0D, Idea.guessType("Property", null,1.0,0.5)));
                cis.add(Idea.createIdea("cis.fuel", 0D, Idea.guessType("Property", null,1.0,0.5)));
                Idea position = Idea.createIdea("cis.position","", Idea.guessType("Property",null,1.0,0.5));
                position.add(Idea.createIdea("cis.position.x",0D,Idea.guessType("QualityDimension",null,1.0,0.5)));
                position.add(Idea.createIdea("cis.position.y",0D,Idea.guessType("QualityDimension",null,1.0,0.5)));
                cis.add(position);
                Idea fov = Idea.createIdea("cis.FOV","", Idea.guessType("Property", null,1.0,0.5));
                Idea bounds = Idea.createIdea("cis.FOV.bounds","", Idea.guessType("Property", null,1.0,0.5));
                bounds.add(Idea.createIdea("cis.FOV.bounds.x",null, Idea.guessType("Property", null,1.0,0.5)));
                bounds.add(Idea.createIdea("cis.FOV.bounds.y",null, Idea.guessType("Property", null,1.0,0.5)));
                bounds.add(Idea.createIdea("cis.FOV.bounds.height",null, Idea.guessType("Property", null,1.0,0.5)));
                bounds.add(Idea.createIdea("cis.FOV.bounds.width",null, Idea.guessType("Property", null,1.0,0.5)));
                fov.add(bounds);
                fov.add(Idea.createIdea("cis.FOV.npoints",0, Idea.guessType("Property", null,1.0,0.5)));
                fov.add(Idea.createIdea("cis.FOV.points","", Idea.guessType("Property", null,1.0,0.5)));
                cis.add(fov);
                innerSenseMO=createMemoryObject("INNER", cis);
                registerMemory(innerSenseMO,"Sensory");
                Thing closestApple = null;
                closestAppleMO=createMemoryObject("CLOSEST_APPLE", closestApple);
                registerMemory(closestAppleMO,"Working");
                List<Thing> knownApples = Collections.synchronizedList(new ArrayList<Thing>());
                knownApplesMO=createMemoryObject("KNOWN_APPLES", knownApples);
                registerMemory(knownApplesMO,"Working");
                List<Thing> knownJewels = Collections.synchronizedList(new ArrayList<Thing>());
                knownJewelsMO = createMemoryObject("KNOWN_JEWELS", knownJewels);
                registerMemory(knownJewelsMO, "Working");
                // Leaflets
                Memory leafletsMO;
                List<Leaflet> leaflets = new ArrayList<>();
                leafletsMO = createMemoryObject("LEAFLETS", leaflets);
                registerMemory(leafletsMO, "Working");
                // DeliverySpot
                Memory deliverySpotMO;
                Idea spot = Idea.createIdea("deliverySpot", "", Idea.guessType("AbstractObject", null, 1.0, 0.5));
                spot.add(Idea.createIdea("x",        0D, Idea.guessType("Property", null, 1.0, 0.5)));
                spot.add(Idea.createIdea("y",        0D, Idea.guessType("Property", null, 1.0, 0.5)));
                spot.add(Idea.createIdea("distance", 0D, Idea.guessType("Property", null, 1.0, 0.5)));
                deliverySpotMO = createMemoryObject("DELIVERY_SPOT", spot);
                registerMemory(deliverySpotMO, "Working");

                // Planning
                targetLeafletMO      = createMemoryObject("TARGET_LEAFLET", null);
                deliberativePhaseMO  = createMemoryObject("DELIBERATIVE_PHASE", "");
                List<PlanStep> planSteps = new ArrayList<>();
                planStepsMO          = createMemoryObject("PLAN_STEPS", planSteps);
                registerMemory(targetLeafletMO,     "Working");
                registerMemory(deliberativePhaseMO, "Working");
                registerMemory(planStepsMO,         "Working");

 		// Create Sensor Codelets	
		Codelet vision=new Vision(env.c);
		vision.addOutput(visionMO);
                insertCodelet(vision); //Creates a vision sensor
                registerCodelet(vision,"Sensory");
		
		Codelet innerSense=new InnerSense(env.c);
		innerSense.addOutput(innerSenseMO);
                insertCodelet(innerSense); //A sensor for the inner state of the creature
                registerCodelet(innerSense,"Sensory");
		
		// Create Actuator Codelets
		Codelet legs=new LegsActionCodelet(env.c);
		legs.addInput(legsMO);
                insertCodelet(legs);
                registerCodelet(legs,"Motor");

		Codelet hands=new HandsActionCodelet(env.c);
		hands.addInput(handsMO);
                insertCodelet(hands);
                registerCodelet(hands,"Motor");
		
		// Create Perception Codelets
                Codelet ad = new AppleDetector();
                ad.addInput(visionMO);
                ad.addOutput(knownApplesMO);
                insertCodelet(ad);
                registerCodelet(ad,"Perception");
                
		Codelet closestAppleDetector = new ClosestAppleDetector();
		closestAppleDetector.addInput(knownApplesMO);
		closestAppleDetector.addInput(innerSenseMO);
		closestAppleDetector.addOutput(closestAppleMO);
                insertCodelet(closestAppleDetector);
                registerCodelet(closestAppleDetector,"Perception");

                Codelet jd = new JewelDetector();
                jd.addInput(visionMO);
                jd.addOutput(knownJewelsMO);
                insertCodelet(jd);
                registerCodelet(jd, "Perception");

                // Leaflet
                Codelet leafletReader = new LeafletReader(env.c);
                leafletReader.addOutput(leafletsMO);
                insertCodelet(leafletReader);
                registerCodelet(leafletReader, "Perception");

                // DeliverySpot
                Codelet deliverySpotDetector = new DeliverySpotDetector();
                deliverySpotDetector.addInput(innerSenseMO);
                deliverySpotDetector.addOutput(deliverySpotMO);
                insertCodelet(deliverySpotDetector);
                registerCodelet(deliverySpotDetector, "Perception");
		
		// Create Behavior Codelets
		Codelet goToClosestApple = new GoToClosestApple(creatureBasicSpeed,reachDistance);
		goToClosestApple.addInput(closestAppleMO);
		goToClosestApple.addInput(innerSenseMO);
		goToClosestApple.addOutput(legsMO);
                insertCodelet(goToClosestApple);
                registerCodelet(goToClosestApple,"Behavioral");
                
                behavioralCodelets.add(goToClosestApple);
		
		Codelet eatApple=new EatClosestApple(reachDistance);
		eatApple.addInput(closestAppleMO);
		eatApple.addInput(innerSenseMO);
		eatApple.addOutput(handsMO);
                eatApple.addOutput(knownApplesMO);
                insertCodelet(eatApple);
                registerCodelet(eatApple,"Behavioral");
                behavioralCodelets.add(eatApple);
                
                Codelet forage=new Forage();
		forage.addInput(knownApplesMO);
                forage.addOutput(legsMO);
                insertCodelet(forage);
                registerCodelet(forage,"Behavioral");
                behavioralCodelets.add(forage);

                // Planning Leaflet
                Codelet leafletSelector = new LeafletSelectorCodelet();
                leafletSelector.addInput(leafletsMO);
                leafletSelector.addInput(knownJewelsMO);
                leafletSelector.addOutput(targetLeafletMO);
                leafletSelector.addOutput(deliberativePhaseMO);
                leafletSelector.addOutput(planStepsMO);
                insertCodelet(leafletSelector);
                registerCodelet(leafletSelector, "Behavioral");
                
                // sets a time step for running the codelets to avoid heating too much your machine
                for (Codelet c : this.getCodeRack().getAllCodelets())
                    c.setTimeStep(200);
		
		// Start Cognitive Cycle
		start(); 
    }             
    
}
