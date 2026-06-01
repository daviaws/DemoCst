package codelets.behaviors;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.Memory;
import br.unicamp.cst.core.entities.MemoryContainer;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.representation.idea.Idea;
import java.util.List;
import org.json.JSONObject;
import support.PlanStep;
import ws3dproxy.model.Thing;

public class PlanExecutor extends Codelet {

    private static final double REACH_DISTANCE = 50.0;
    private static final int    SPEED          = 3;
    private static final double ACTIVATION     = 0.8;

    private Memory planStepsMO;
    private Memory deliberativePhaseMO;
    private Memory innerSenseMO;
    private Memory knownJewelsMO;
    private Memory handsMO;
    private MemoryContainer legsMO;

    public PlanExecutor() {
        this.name = "PlanExecutor";
    }

    @Override
    public void accessMemoryObjects() {
        this.planStepsMO         = (MemoryObject)    this.getInput("PLAN_STEPS");
        this.deliberativePhaseMO = (MemoryObject)    this.getInput("DELIBERATIVE_PHASE");
        this.innerSenseMO        = (MemoryObject)    this.getInput("INNER");
        this.knownJewelsMO       = (MemoryObject)    this.getInput("KNOWN_JEWELS");
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

        @SuppressWarnings("unchecked")
        List<PlanStep> steps = (List<PlanStep>) planStepsMO.getI();

        PlanStep next = steps.stream()
            .filter(s -> s.status == PlanStep.Status.PENDING)
            .findFirst()
            .orElse(null);

        if (next == null) {
            activation = 0.0;
            return;
        }

        Idea cis     = (Idea) innerSenseMO.getI();
        double selfX = (double) cis.get("position.x").getValue();
        double selfY = (double) cis.get("position.y").getValue();

        double dx   = next.x - selfX;
        double dy   = next.y - selfY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist <= REACH_DISTANCE) {
            try {
                JSONObject msg = new JSONObject();
                msg.put("OBJECT", next.jewelName);
                msg.put("ACTION", "GET");
                handsMO.setI(msg.toString());
                System.out.println("[EXECUCAO] GET: " + next.jewelName + " (" + next.jewelColor + ")");
                @SuppressWarnings("unchecked")
                List<Thing> known = (List<Thing>) knownJewelsMO.getI();
                synchronized (known) {
                    known.removeIf(t -> t.getName().equals(next.jewelName));
                }
                next.status = PlanStep.Status.DONE;
                activation  = ACTIVATION;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Idea message = Idea.createIdea("message", "", Idea.guessType("Property", null, 1.0, 0.5));
            message.add(Idea.createIdea("ACTION", "GOTO",       Idea.guessType("Property", null, 1.0, 0.5)));
            message.add(Idea.createIdea("X",      (int) next.x, Idea.guessType("Property", null, 1.0, 0.5)));
            message.add(Idea.createIdea("Y",      (int) next.y, Idea.guessType("Property", null, 1.0, 0.5)));
            message.add(Idea.createIdea("SPEED",  SPEED,        Idea.guessType("Property", null, 1.0, 0.5)));
            activation = ACTIVATION;
            legsMO.setI(toJson(message), activation, name);
        }
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