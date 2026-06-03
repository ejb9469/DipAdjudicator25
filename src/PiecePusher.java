import java.util.*;

/**
 * The `PiecePusher` class holds a combination of:
 *          "movement phase" (i.e. Spring or Fall) Orders,
 *          and retreat phase Orders<br><br>
 *
 * Each time `PiecePusher::push()` is called, PiecePusher progresses in 3 states: <i>movement</i>, <i>retreats</i>, and <i>"complete"</i>
 *      (builds are handled separately)<br><br>
 * When `PiecePusher::push()` is 'pushing' in the <u>movement</u> phase, the movements are forced / reflected on the board, and retreats are generated.<br>
 * When `PiecePusher::push()` is 'pushing' in the <u>retreats</u> phase, the retreats are forced / reflected on the board. The movement phase Orders remain unaltered.<br><br>
 *
 * <u><b>IMPORTANT:</b></u> Each time `PiecePusher::push()` is called, the current instance of PiecePusher will "LOCK", requiring a new instance to function.
 * (This is to avoid using "output" Retreats, by contract)
 */
public class PiecePusher implements Lockable {


    protected final Collection<Order>       movementPhaseOrders;  // must be explicitly assigned via constructor
    protected final Collection<Order>       retreatPhaseOrders;

    protected       Map<Province, Unit>     unitMap;

    private         boolean                 locked = false;


    public PiecePusher(Collection<Order> movementPhaseOrders) {
        this.movementPhaseOrders = movementPhaseOrders;
        this.retreatPhaseOrders = new HashSet<>();
        this.unitMap = new EnumMap<>(Province.class);
        initialize(false);
    }

    public PiecePusher(Collection<Order> movementPhaseOrders, Collection<Order> retreatPhaseOrders) {
        this.movementPhaseOrders = movementPhaseOrders;
        this.retreatPhaseOrders = retreatPhaseOrders;
        this.unitMap = new EnumMap<>(Province.class);
        initialize(true);
    }

    // constructor helper method
    private void initialize(boolean useRetreatsPos) {
        Collection<Order> orders;
        if (useRetreatsPos)
            orders = retreatPhaseOrders;
        else
            orders = movementPhaseOrders;
        for (Order order : orders)
            this.unitMap.put(order.pos0, new Unit(order.owner, order.unitType));
    }


    public Collection<Order> getMovementPhaseOrders() {
        return movementPhaseOrders;
    }

    public Collection<Order> getRetreatPhaseOrders() {
        return retreatPhaseOrders;
    }

    public Map<Province, Unit> getUnitMap() {
        return unitMap;
    }


    public void lock() { this.locked = true; }

    public boolean isLocked() {
        return locked;
    }


    @SuppressWarnings("PointlessBooleanExpression")
    public void push() {

        // Will lock this PiecePusher at the end of this method

        if (this.isLocked())
            throw new IllegalStateException("`%s::push()` ==> executed in locked state!");
        // ELSE: below

        Map<Province, Unit> unitMap = new EnumMap<>(Province.class);

        if (!retreatPhaseOrders.isEmpty()) {

            for (Order retreatOrder : Orders.pruneForOrderType(OrderType.RETREAT, retreatPhaseOrders))
                if (retreatOrder.verdict == true)
                    unitMap.put(retreatOrder.pos1, new Unit(retreatOrder.owner, retreatOrder.unitType));

            this.unitMap.putAll(unitMap);

        }

        else {

            Collection<Order> retreats = new ArrayList<>();
            for (Order order : movementPhaseOrders) {

                if (order.orderType == OrderType.MOVE) {
                    if (order.verdict == true)
                        unitMap.put(order.pos1, new Unit(order.owner, order.unitType));
                    else
                        // TODO: Check for assailants (?)
                        unitMap.put(order.pos0, new Unit(order.owner, order.unitType));
                } else if (order.orderType == OrderType.HOLD || order.orderType == OrderType.SUPPORT || order.orderType == OrderType.CONVOY) {
                    if (order.verdict == true)
                        unitMap.put(order.pos0, new Unit(order.owner, order.unitType));
                    else {
                        Collection<Order> assailants = Orders.locateUnitsMovingToPosition(order.pos0, movementPhaseOrders);
                        boolean anySuccessfulAssailant = false;
                        for (Order moveOrder : assailants) {
                            if (moveOrder.verdict == true) {
                                anySuccessfulAssailant = true;
                                Order retreatOrder = new Order(
                                        moveOrder.owner, moveOrder.unitType, moveOrder.pos1,
                                        OrderType.RETREAT, null, null);
                                retreatOrder.dislodged = true;
                                retreats.add(retreatOrder);
                                break;  // 2+ units cannot succeed to the same area
                            }
                        }
                        if (!anySuccessfulAssailant)
                            unitMap.put(order.pos0, new Unit(order.owner, order.unitType));
                    }
                }

            }

            this.retreatPhaseOrders.addAll(retreats);  // no need to `.clear()`: is already empty
            this.unitMap.putAll(unitMap);

        }

        this.lock();  // LOCK

    }


}