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


    public String unitToString() {

        String output = owner.getPrefix() + " ";
        output += unitType.name().charAt(0) + " ";
        output += pos0.name();
        return output;

    }


    @Override
    public String toString() {

        String output = this.unitToString();

        if (orderType == OrderType.MOVE) {
            output += " - " + pos1.name();  // .getName() would return the PROVINCE's full name
        } else if (orderType == OrderType.HOLD) {
            output += " H ";
        } else if (orderType == OrderType.SUPPORT) {
            output += " S " + pos1.name() + " ";
            if (pos2 == null)
                output += "H";
            else
                output += "- " + pos2.name();
        } else if (orderType == OrderType.CONVOY) {
            output += " C " + pos1.name() + " - " + pos2.name();
        } else {
            output += " ???";
        }

        return output;

    }

    public String metaToString() {
        return String.format("%s:%b\t%s:%b\t%s:%b", "resolved", resolved, "verdict", verdict, "dislodged", dislodged);
    }

}
