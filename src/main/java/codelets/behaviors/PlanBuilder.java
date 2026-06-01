package codelets.behaviors;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.Memory;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.representation.idea.Idea;
import java.util.List;
import java.util.Map;
import support.PlanStep;
import ws3dproxy.model.Leaflet;
import ws3dproxy.model.Thing;
import ws3dproxy.util.Constants;

/**
 * Builds the deliberative plan while DELIBERATIVE_PHASE == "planning".
 *
 * For each color still needed by the target leaflet, picks the closest known
 * jewel of that color that doesn't already have a plan step, adds a PlanStep,
 * and repeats until all colors are covered — then transitions to "executing".
 *
 * If a needed color has no known jewel yet, stays in "planning" until one
 * appears (creature keeps wandering/eating in the meantime).
 */
public class PlanBuilder extends Codelet {

    private Memory targetLeafletMO;
    private Memory knownJewelsMO;
    private Memory innerSenseMO;
    private Memory planStepsMO;
    private Memory deliberativePhaseMO;

    public PlanBuilder() {
        this.name = "PlanBuilder";
    }

    @Override
    public void accessMemoryObjects() {
        this.targetLeafletMO     = (MemoryObject) this.getInput("TARGET_LEAFLET");
        this.knownJewelsMO       = (MemoryObject) this.getInput("KNOWN_JEWELS");
        this.innerSenseMO        = (MemoryObject) this.getInput("INNER");
        this.planStepsMO         = (MemoryObject) this.getOutput("PLAN_STEPS");
        this.deliberativePhaseMO = (MemoryObject) this.getOutput("DELIBERATIVE_PHASE");
    }

    @Override
    public void calculateActivation() {
    }

    @Override
    public void proc() {
        String phase = (String) deliberativePhaseMO.getI();
        if (!"planning".equals(phase)) return;

        Leaflet target = (Leaflet) targetLeafletMO.getI();
        if (target == null) return;

        @SuppressWarnings("unchecked")
        List<Thing> knownJewels = (List<Thing>) knownJewelsMO.getI();
        @SuppressWarnings("unchecked")
        List<PlanStep> steps = (List<PlanStep>) planStepsMO.getI();

        Idea cis     = (Idea) innerSenseMO.getI();
        double selfX = (double) cis.get("position.x").getValue();
        double selfY = (double) cis.get("position.y").getValue();

        boolean allCovered = true;

        for (Map.Entry<String, Integer[]> entry : target.getItems().entrySet()) {
            String color  = entry.getKey();
            int total     = entry.getValue()[0];
            int collected = entry.getValue()[1];
            int missing   = total - collected;
            if (missing <= 0) continue;

            long stepsForColor = steps.stream()
                .filter(s -> s.jewelColor.equals(color) && s.status != PlanStep.Status.DISCARDED)
                .count();

            int toAdd = missing - (int) stepsForColor;

            for (int i = 0; i < toAdd; i++) {
                Thing best = closestJewelOfColor(color, knownJewels, steps, selfX, selfY);
                if (best == null) {
                    allCovered = false;
                    break;
                }
                double dx   = best.getX1() - selfX;
                double dy   = best.getY1() - selfY;
                double dist = Math.sqrt(dx * dx + dy * dy);
                PlanStep step = new PlanStep(best.getName(), color, best.getX1(), best.getY1(), dist);
                steps.add(step);
                System.out.println("[PLANO] Passo adicionado: " + best.getName() + " (" + color + ") dist=" + dist);
            }
        }

        boolean hasPending = steps.stream().anyMatch(s -> s.status == PlanStep.Status.PENDING);
        if (allCovered || (!hasPending && !steps.isEmpty())) {
            deliberativePhaseMO.setI("executing");
            System.out.println("[DELIBERATIVO] Plano pronto, iniciando execução. Passos: " + steps.size());
        }
    }

    private Thing closestJewelOfColor(String color, List<Thing> knownJewels,
                                       List<PlanStep> steps, double selfX, double selfY) {
        Thing best      = null;
        double bestDist = Double.MAX_VALUE;

        synchronized (knownJewels) {
            for (Thing t : knownJewels) {
                if (!Constants.getColorName(t.getMaterial().getColor()).equals(color)) continue;

                boolean assigned = steps.stream()
                    .anyMatch(s -> s.jewelName.equals(t.getName()) && s.status != PlanStep.Status.DISCARDED);
                if (assigned) continue;

                double dx   = t.getX1() - selfX;
                double dy   = t.getY1() - selfY;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = t;
                }
            }
        }
        return best;
    }
}