package codelets.behaviors;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.Memory;
import br.unicamp.cst.core.entities.MemoryContainer;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.representation.idea.Idea;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import support.PlanStep;
import ws3dproxy.model.Creature;
import ws3dproxy.model.Leaflet;

public class PlanDelivery extends Codelet {

    private static final double REACH_DISTANCE = 50.0;
    private static final int    SPEED          = 3;

    private final Creature creature;
    private final LeafletSelectorCodelet selector;

    private Memory targetLeafletMO;
    private Memory deliberativePhaseMO;
    private Memory planStepsMO;
    private Memory deliverySpotMO;
    private Memory leafletsMO;
    private Memory handsMO;
    private MemoryContainer legsMO;

    public PlanDelivery(Creature creature, LeafletSelectorCodelet selector) {
        this.creature = creature;
        this.selector = selector;
        this.name = "PlanDelivery";
    }

    @Override
    public void accessMemoryObjects() {
        this.targetLeafletMO     = (MemoryObject)    this.getInput("TARGET_LEAFLET");
        this.deliberativePhaseMO = (MemoryObject)    this.getInput("DELIBERATIVE_PHASE");
        this.planStepsMO         = (MemoryObject)    this.getInput("PLAN_STEPS");
        this.deliverySpotMO      = (MemoryObject)    this.getInput("DELIVERY_SPOT");
        this.leafletsMO          = (MemoryObject)    this.getInput("LEAFLETS");
        this.handsMO             = (MemoryObject)    this.getOutput("HANDS");
        this.legsMO              = (MemoryContainer) this.getOutput("LEGS");
    }

    @Override
    public void calculateActivation() {
    }

    @Override
    public void proc() {
        String phase = (String) deliberativePhaseMO.getI();
        if (!"executing".equals(phase)) {
            activation = 0.0;
            return;
        }

        Leaflet target = (Leaflet) targetLeafletMO.getI();
        if (target == null) {
            activation = 0.0;
            return;
        }

        @SuppressWarnings("unchecked")
        List<PlanStep> steps = (List<PlanStep>) planStepsMO.getI();
        boolean hasPending = steps.stream().anyMatch(s -> s.status == PlanStep.Status.PENDING);
        if (hasPending) {
            activation = 0.0;
            return;
        }

        boolean ready = isReady(target);
        System.out.println("[DELIVERY] isReady=" + ready + " targetId=" + target.getID());
        if (!ready) {
            // Log why it's not ready
            Leaflet current = getCurrentLeaflet(target);
            if (current == null) {
                System.out.println("[DELIVERY] current leaflet not found in LEAFLETS list");
            } else {
                for (Map.Entry<String, Integer[]> entry : current.getItems().entrySet()) {
                    System.out.println("[DELIVERY] " + entry.getKey() 
                        + " needed=" + entry.getValue()[0] 
                        + " collected=" + entry.getValue()[1]);
                }
            }
            steps.clear();
            deliberativePhaseMO.setI("planning");
            activation = 0.0;
            return;
        }

        Idea spot   = (Idea) deliverySpotMO.getI();
        double dsX  = (double) spot.get("x").getValue();
        double dsY  = (double) spot.get("y").getValue();
        double dist = (double) spot.get("distance").getValue();

        if (dist > REACH_DISTANCE) {
            Idea message = Idea.createIdea("message", "", Idea.guessType("Property", null, 1.0, 0.5));
            message.add(Idea.createIdea("ACTION", "GOTO",    Idea.guessType("Property", null, 1.0, 0.5)));
            message.add(Idea.createIdea("X",      (int) dsX, Idea.guessType("Property", null, 1.0, 0.5)));
            message.add(Idea.createIdea("Y",      (int) dsY, Idea.guessType("Property", null, 1.0, 0.5)));
            message.add(Idea.createIdea("SPEED",  SPEED,     Idea.guessType("Property", null, 1.0, 0.5)));
            legsMO.setI(toJson(message), 1.0, name);
            handsMO.setI("");
            activation = 1.0;
            System.out.println("[ENTREGA] Indo ao delivery spot (dist=" + dist + ")");
        } else {
            try {
                String leafletId = target.getID().toString();
                creature.deliverLeaflet(leafletId);

                JSONObject msg = new JSONObject();
                msg.put("ACTION", "DELIVER");
                msg.put("LEAFLET_ID", leafletId);
                handsMO.setI(msg.toString());

                System.out.println("[ENTREGA] *** Leaflet " + leafletId + " entregue! ***");

                selector.markDelivered(leafletId);
                deliberativePhaseMO.setI("");
                targetLeafletMO.setI(null);
                steps.clear();
                legsMO.setI("", 0.0, name);
                activation = 0.0;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Leaflet getCurrentLeaflet(Leaflet target) {
        @SuppressWarnings("unchecked")
        List<Leaflet> leaflets = (List<Leaflet>) leafletsMO.getI();
        if (leaflets == null) return null;
        return leaflets.stream()
            .filter(l -> l.getID().equals(target.getID()))
            .findFirst().orElse(null);
    }

    private boolean isReady(Leaflet target) {
        Leaflet current = getCurrentLeaflet(target);
        if (current == null) return false;
        for (Map.Entry<String, Integer[]> entry : current.getItems().entrySet()) {
            if (entry.getValue()[0] - entry.getValue()[1] > 0) return false;
        }
        return true;
    }

    private String toJson(Idea i) {
        String q = "\"", out = "{";
        int ii = 0;
        for (Idea il : i.getL()) {
            String val = il.getL().isEmpty()
                ? (il.isNumber() ? il.getValue().toString() : q + il.getValue() + q)
                : toJson(il);
            out += (ii == 0 ? "" : ",") + q + il.getName() + q + ":" + val;
            ii++;
        }
        return out + "}";
    }
}