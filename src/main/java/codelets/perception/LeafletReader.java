package codelets.perception;
 
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.Memory;
import br.unicamp.cst.core.entities.MemoryObject;
import java.util.List;
import ws3dproxy.model.Creature;
import ws3dproxy.model.Leaflet;
 
/**
 * Reads the creature's leaflets and publishes them to the LEAFLETS MemoryObject.
 */
public class LeafletReader extends Codelet {
 
    private final Creature creature;
    private Memory leafletsMO;
 
    public LeafletReader(Creature creature) {
        this.creature = creature;
        this.name = "LeafletReader";
    }
 
    @Override
    public void accessMemoryObjects() {
        this.leafletsMO = (MemoryObject) this.getOutput("LEAFLETS");
    }
 
    @Override
    public void calculateActivation() {
    }
 
    @Override
    public void proc() {
        List<Leaflet> leaflets = creature.getLeaflets();
        leafletsMO.setI(leaflets);
    }
}