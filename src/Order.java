/**
 * The `Order` class is a public-facing class representing a Diplomacy order 'struct'.<br><br>
 *
 * In addition to the relevant data fields, the `Order` class also contains adjudication-related metadata fields --
 * (i.e. `<i>resolved</i>`, `<i>verdict</i>`, & `<i>visited</i>`) -- and a 'dangling' field <i>bool</i> `<i>dislodged</i>` for general-purpose.
 */
public class Order {

    // TODO: Reformat? from class -> record (introduced Java 16; 2021)

    // Core fields
    public Nation owner;
    public UnitType unitType;
    public OrderType orderType;
    public Province pos0, pos1, pos2;

    // `dislodged` field not currently utilized (09-21-25 -- now: building up test cases)
    public boolean dislodged = false;

    // Metadata fields
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

    public Order(Nation owner, UnitType unitType, Province origin, OrderType orderType) {
        this(owner, unitType, origin, orderType, null, null);
    }

    public Order(Order order2) {
        this(order2.owner, order2.unitType, order2.pos0, order2.orderType, order2.pos1, order2.pos2, order2.dislodged);
        this.resolved = order2.resolved;
        this.verdict = order2.verdict;
        this.visited = order2.visited;
    }


    /**
     * Generates & returns a String representation of this Order's unit components (i.e. "no orders")
     *
     * @return String representation of this Order's unit components
     */
    public String unitToString() {
        String output = owner.getPrefix() + " ";
        output += unitType.name().charAt(0) + " ";
        output += pos0.name();
        return output;
    }

    /**
     * Formats & returns a String representation of this Order's metadata fields
     *
     * @return String representation of this Order's metadata fields
     */
    public String metaToString() {
        return String.format("%s:%b\t%s:%b\t%s:%b", "resolved", resolved, "verdict", verdict, "dislodged", dislodged);
    }

    /**
     * <b>Overridden</b> `toString()` method; generates & returns a String representation of this Order in
     * "<a href="https://www.backstabbr.com/"><i>Backstabbr</a> notation</i>".
     *
     * @return Implicit String representation of this Order in Backstabbr notation
     */
    @Override
    public String toString() {

        String output = this.unitToString();

        if (orderType == OrderType.MOVE) {
            output += " - " + pos1.name();  // .getName() would return the PROVINCE's full name
        } else if (orderType == OrderType.HOLD) {
            output += " H";
        } else if (orderType == OrderType.SUPPORT) {
            output += " S " + pos1.name() + " ";
            if (pos2 == null)
                output += "H";
            else
                output += "- " + pos2.name();
        } else if (orderType == OrderType.CONVOY) {
            output += " C " + pos1.name() + " - " + pos2.name();
        } else if (orderType == OrderType.RETREAT) {
            if (pos1 == null)
                output += " PIFF";
            else
                output += " R " + pos1.name();
        }

        return output;

    }


    /**
     * <b>Overridden</b> `equals()` method; compares Object equality with another given Object<br><br>
     *
     * Will return the equality of the core Order fields & `<i>dislodged</i>`, while ignoring all metadata fields.
     *
     * @param other   the reference object with which to compare.
     * @return Object equality of this and `other`
     */
    @Override
    public boolean equals(Object other) {

        if (!(other instanceof Order))
            return false;

        Order order2;
        try {
            order2 = (Order) other;
        } catch (ClassCastException ex) {
            ex.printStackTrace();
            return false;
        }

        return (this.owner == order2.owner && this.unitType == order2.unitType && this.orderType == order2.orderType &&
                this.pos0 == order2.pos0 && this.pos1 == order2.pos1 && this.pos2 == order2.pos2 &&
                this.dislodged == order2.dislodged);

    }

}
