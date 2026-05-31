package codelets.perception;
 
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.Memory;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.representation.idea.Idea;
import ws3dproxy.model.World;
import ws3dproxy.model.WorldPoint;
 
/**
 * Reads the delivery spot position from the World and publishes x, y and
 * distance from the creature to the DELIVERY_SPOT MemoryObject.
 */
public class DeliverySpotDetector extends Codelet {
 
    private Memory innerSenseMO;
    private Memory deliverySpotMO;
 
    public DeliverySpotDetector() {
        this.name = "DeliverySpotDetector";
    }
 
    @Override
    public void accessMemoryObjects() {
        this.innerSenseMO    = (MemoryObject) this.getInput("INNER");
        this.deliverySpotMO  = (MemoryObject) this.getOutput("DELIVERY_SPOT");
    }
 
    @Override
    public void calculateActivation() {
    }
 
    @Override
    public void proc() {
        try {
            WorldPoint ds = World.getDeliverySpot();
            if (ds == null) return;
 
            Idea cis  = (Idea) innerSenseMO.getI();
            double selfX = (double) cis.get("position.x").getValue();
            double selfY = (double) cis.get("position.y").getValue();
 
            double dx = ds.getX() - selfX;
            double dy = ds.getY() - selfY;
            double distance = Math.sqrt(dx * dx + dy * dy);
 
            Idea spot = (Idea) deliverySpotMO.getI();
            spot.get("x").setValue(ds.getX());
            spot.get("y").setValue(ds.getY());
            spot.get("distance").setValue(distance);
            deliverySpotMO.setI(spot);
 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
 