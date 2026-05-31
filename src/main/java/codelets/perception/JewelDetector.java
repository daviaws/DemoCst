package codelets.perception;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.Memory;
import br.unicamp.cst.core.entities.MemoryObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import ws3dproxy.model.Thing;
import ws3dproxy.util.Constants;

/**
 * Detects jewels in the vision field and maintains an accumulated memory of
 * known jewels, updating their positions as they are seen.
 */
public class JewelDetector extends Codelet {

    private Memory visionMO;
    private Memory knownJewelsMO;

    public JewelDetector() {
        this.name = "JewelDetector";
    }

    @Override
    public void accessMemoryObjects() {
        synchronized (this) {
            this.visionMO = (MemoryObject) this.getInput("VISION");
        }
        this.knownJewelsMO = (MemoryObject) this.getOutput("KNOWN_JEWELS");
    }

    @Override
    public void calculateActivation() {
    }

    @Override
    public void proc() {
        CopyOnWriteArrayList<Thing> vision;
        List<Thing> known;

        synchronized (visionMO) {
            vision = new CopyOnWriteArrayList<>((List<Thing>) visionMO.getI());
            known = Collections.synchronizedList((List<Thing>) knownJewelsMO.getI());

            synchronized (vision) {
                for (Thing t : vision) {
                    if (t.getCategory() != Constants.categoryJEWEL) continue;

                    boolean found = false;
                    synchronized (known) {
                        CopyOnWriteArrayList<Thing> snapshot = new CopyOnWriteArrayList<>(known);
                        for (Thing e : snapshot) {
                            if (t.getName().equals(e.getName())) {
                                found = true;
                                // Atualiza posição — a jóia pode ter sido empurrada
                                known.set(known.indexOf(e), t);
                                break;
                            }
                        }
                        if (!found) {
                            known.add(t);
                        }
                    }
                }
            }
        }
    }
}