import java.util.Objects;

/**
 * The `Order` class is a public-facing class representing a Diplomacy order 'struct'.<br><br>
 *
 * In addition to the relevant data fields, the `Order` class also contains adjudication-related metadata fields --
 * (i.e. `<i>resolved</i>`, `<i>verdict</i>`, & `<i>visited</i>`) -- and a 'dangling' field <i>bool</i> `<i>dislodged</i>` for general-purpose.
 */
public class Order {

    // TODO: Reformat? from class -> record (introduced Java 16; 2021)

    // Core fields
    public Nation       owner;
    public UnitType     unitType;
    public OrderType    orderType;
    public Province     pos0, pos1, pos2;

    // `dislodged` field not currently utilized (09-21-25 -- now: building up test cases)
    // TODO: Quasi-implemented: 10/19/25 --> WIP
    public boolean      dislodged;

    // Metadata fields -- `protected` access modifiers
    // REMEMBER to update `Order.wipeMetaInf()` when adding new metadata flags
    protected boolean resolved;
    protected boolean verdict;

    protected boolean visited;

    protected boolean suppressH2HAdjudication = false;

    // "Original order" field, used if Order is changed during adjudication
    // ... (for e.g. using Szykman rules)
    // Will CLONE if .setOriginalOrder() is used
    // TODO: Quasi-implemented: 11/15/25 --> WIP
    private Order originalOrder = null;


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
        this.suppressH2HAdjudication = order2.suppressH2HAdjudication;
        this.originalOrder = order2.originalOrder;
    }


    protected Order getSnapshot() {
        return this.originalOrder;
    }

    protected void takeSnapshot() {
        this.originalOrder = new Order(this);  // CLONE constructor
        getSnapshot().originalOrder = null;  // avoid infinite reference loop
    }

    protected void restoreFromSnapshot() {

        this.owner = getSnapshot().owner;
        this.unitType = getSnapshot().unitType;
        this.orderType = getSnapshot().orderType;
        this.pos0 = getSnapshot().pos0;
        this.pos1 = getSnapshot().pos1;
        this.pos2 = getSnapshot().pos2;
        // does replicate `dislodged` field, because so does clone constructor
        this.dislodged = getSnapshot().dislodged;

        // un-set snapshot
        this.originalOrder = null;

    }


    protected void wipeMetaInf() {
        this.resolved = false;
        this.verdict = false;
        this.visited = false;
        this.suppressH2HAdjudication = false;
    }


    /**
     * Generates & returns a String representation of this Order's unit components (i.e. "no orders")
     *
     * @return String representation of this Order's unit components
     */
    public String unitToString() {
        String output = owner.getPrefix() + " ";
        output += unitType.toString().charAt(0) + " ";
        output += pos0.toString();
        return output;
    }

    /**
     * Formats & returns a String representation of this Order's metadata fields
     *
     * @return String representation of this Order's metadata fields
     */
    protected String metaToString() {
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
            output += " - " + pos1.toString();  // .getName() would return the PROVINCE's full name
        } else if (orderType == OrderType.HOLD) {
            output += " H";
        } else if (orderType == OrderType.SUPPORT) {
            output += " S " + pos1.toString() + " ";
            if (pos2 == null)
                output += "H";
            else
                output += "- " + pos2.toString();
        } else if (orderType == OrderType.CONVOY) {
            output += " C " + pos1.toString() + " - " + pos2.toString();
        } else if (orderType == OrderType.RETREAT) {
            if (pos1 == null)
                output += " PIFF";
            else
                output += " R " + pos1.toString();
        } else if (orderType == OrderType.BUILD) {
            output += " BUILD";
        } else if (orderType == OrderType.DESTROY) {
            output += " DESTROY";
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

    // TODO: Documentation needed!
    @Override
    public int hashCode() {
        return Objects.hash(this.owner, this.unitType, this.orderType,
                            this.pos0, this.pos1, this.pos2,
                            this.dislodged,
                            this.resolved, this.verdict);
        // Notable exceptions: `visited` and `suppressH2HAdjudication` are not computed into the hash
    }

}
