package codelets.behaviors;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.Memory;
import br.unicamp.cst.core.entities.MemoryObject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import support.PlanStep;
import ws3dproxy.model.Leaflet;
import ws3dproxy.model.Thing;
import ws3dproxy.util.Constants;

/**
 * Selects the leaflet with the highest payment whose required jewel colors
 * all have at least one known jewel available. Sets TARGET_LEAFLET and
 * transitions DELIBERATIVE_PHASE to "planning".
 *
 * Only fires when there is no active deliberative phase.
 */
public class LeafletSelectorCodelet extends Codelet {

    private final Set<String> deliveredLeaflets = new HashSet<>();

    private Memory leafletsMO;
    private Memory knownJewelsMO;
    private Memory targetLeafletMO;
    private Memory deliberativePhaseMO;
    private Memory planStepsMO;

    public LeafletSelectorCodelet() {
        this.name = "LeafletSelectorCodelet";
    }

    /** Called by DeliveryCodelet after a successful delivery. */
    public void markDelivered(String leafletId) {
        deliveredLeaflets.add(leafletId);
    }

    @Override
    public void accessMemoryObjects() {
        this.leafletsMO          = (MemoryObject) this.getInput("LEAFLETS");
        this.knownJewelsMO       = (MemoryObject) this.getInput("KNOWN_JEWELS");
        this.targetLeafletMO     = (MemoryObject) this.getOutput("TARGET_LEAFLET");
        this.deliberativePhaseMO = (MemoryObject) this.getOutput("DELIBERATIVE_PHASE");
        this.planStepsMO         = (MemoryObject) this.getOutput("PLAN_STEPS");
    }

    @Override
    public void calculateActivation() {
    }

    @Override
    public void proc() {
        String phase = (String) deliberativePhaseMO.getI();
        if (phase != null && !phase.isEmpty()) return;

        @SuppressWarnings("unchecked")
        List<Leaflet> leaflets = (List<Leaflet>) leafletsMO.getI();
        @SuppressWarnings("unchecked")
        List<Thing> knownJewels = (List<Thing>) knownJewelsMO.getI();

        if (leaflets == null || leaflets.isEmpty()) return;

        Leaflet best = null;
        double bestPayment = -1;

        for (Leaflet l : leaflets) {
            if (deliveredLeaflets.contains(l.getID().toString())) continue;
            if (isLeafletReady(l)) continue;
            if (!isFeasible(l, knownJewels)) continue;

            if (l.getPayment() > bestPayment) {
                bestPayment = l.getPayment();
                best = l;
            }
        }

        if (best != null) {
            targetLeafletMO.setI(best);
            deliberativePhaseMO.setI("planning");
            @SuppressWarnings("unchecked")
            List<PlanStep> steps = (List<PlanStep>) planStepsMO.getI();
            steps.clear();
            System.out.println("[DELIBERATIVO] Leaflet alvo: " + best.getID() + " (" + bestPayment + " pts)");
        }
    }

    /** Returns true when every required color has at least one known jewel. */
    private boolean isFeasible(Leaflet l, List<Thing> knownJewels) {
        for (Map.Entry<String, Integer[]> entry : l.getItems().entrySet()) {
            String color  = entry.getKey();   // e.g. "red", "green"
            int missing   = entry.getValue()[0] - entry.getValue()[1];
            if (missing <= 0) continue;

            boolean found = false;
            synchronized (knownJewels) {
                for (Thing t : knownJewels) {
                    if (Constants.getColorName(t.getMaterial().getColor()).equals(color)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) return false;
        }
        return true;
    }

    /** Returns true when all required jewels have been collected. */
    private boolean isLeafletReady(Leaflet l) {
        for (Integer[] counts : l.getItems().values()) {
            if (counts[0] - counts[1] > 0) return false;
        }
        return true;
    }
}