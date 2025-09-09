import java.util.Objects;

public class Order {

    public Nation owner;
    public UnitType unitType;
    public OrderType orderType;
    public Province pos0, pos1, pos2;

    public boolean dislodged;

    public boolean resolved;
    public boolean verdict;
    public boolean visited;

    public Order(Nation owner, UnitType unitType, Province origin, OrderType orderType, Province pos1, Province pos2, boolean dislodged) {
        this.owner = owner;
        this.unitType = unitType;
        this.orderType = orderType;
        this.pos0 = origin;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.dislodged = dislodged;
    }

    public Order(Nation owner, UnitType unitType, Province origin, OrderType orderType, Province pos1, Province pos2) {
        this(owner, unitType, origin, orderType, pos1, pos2, false);
    }

    public Order(Nation owner, UnitType unitType, Province origin, OrderType orderType, Province pos1) {
        this(owner, unitType, origin, orderType, pos1, null);
    }

    @Override
    public String toString() {
        return String.format("%s %s %s %s %s %s",
                owner.toString().charAt(0)+owner.toString().substring(1,2).toLowerCase(),
                unitType.toString().charAt(0),
                pos0.toString(),
                orderType.toString().substring(0,4),
                pos1, Objects.requireNonNullElse(pos2, ""));
    }

    public String metaToString() {
        return String.format("%s:%b\t%s:%b\t%s:%b", "resolved", resolved, "verdict", verdict, "dislodged", dislodged);
    }

}
