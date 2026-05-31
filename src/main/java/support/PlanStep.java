package support;

/**
 * Represents a single step in the deliberative plan — one jewel to collect.
 */
public class PlanStep {

    public enum Status { PENDING, DONE, DISCARDED }

    public String jewelName;
    public String jewelColor;
    public double x;
    public double y;
    public double dist;
    public Status status;

    public PlanStep(String jewelName, String jewelColor, double x, double y, double dist) {
        this.jewelName  = jewelName;
        this.jewelColor = jewelColor;
        this.x          = x;
        this.y          = y;
        this.dist       = dist;
        this.status     = Status.PENDING;
    }

    @Override
    public String toString() {
        return "PlanStep{" + jewelName + " (" + jewelColor + ") dist=" + dist + " status=" + status + "}";
    }
}