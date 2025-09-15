import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class Convoys extends Orders {

    /**
     * Draws one possible convoy path for a given move Order
     * @param moveOrder Move order
     * @param orders List of all orders
     * @return One possible convoy path for `moveOrder`
     */
    public static List<Order> drawConvoyPath(Order moveOrder, Collection<Order> orders) {

        List<Order> convoyPath;
        List<Order> beginningConvoys = new ArrayList<>();
        for (Order order : orders) {
            if (order.equals(moveOrder) ||
                order.unitType == UnitType.ARMY ||
                order.orderType != OrderType.CONVOY) {
                continue;
            }
            if (order.pos1.equals(moveOrder.pos0) && order.pos2.equals(moveOrder.pos1) && order.pos0.isAdjacentTo(moveOrder.pos0))
                beginningConvoys.add(order);
        }

        if (beginningConvoys.isEmpty())
            return beginningConvoys;  // empty list

        Order firstConvoy = beginningConvoys.getFirst();
        List<Order> initPath = new ArrayList<>();
        initPath.add(firstConvoy);
        convoyPath = convoyPath(firstConvoy, initPath, new ArrayList<>(), (List<Order>) orders);

        return convoyPath;

    }

    /**
     * Locate convoy orders identical to `convoyOrder` and adjacent to its unit
     * @param convoyOrder Matching convoy order
     * @param excludedOrders List of orders to exclude from the search
     * @param orders List of all orders
     * @return List of adjacent fleets convoying the same army
     */
    public static List<Order> findAdjacentConvoys(Order convoyOrder, Collection<Order> excludedOrders, Collection<Order> orders) {
        List<Order> adjacentConvoys = new ArrayList<>();
        for (Order order : orders) {
            if (excludedOrders.contains(order) ||
                    order.equals(convoyOrder) ||
                    order.orderType != OrderType.CONVOY)
                continue;
            if (order.pos1.equals(convoyOrder.pos1) && order.pos2.equals(convoyOrder.pos2) && order.pos0.isAdjacentTo(convoyOrder.pos0))
                adjacentConvoys.add(order);
        }
        return adjacentConvoys;
    }


    /**
     * Convoy pathing function, will convoy chain if necessary
     * @param firstConvoy The principal convoy
     * @param convoyPath The convoy path taken so far
     * @param excludedOrders List of orders to exclude from the search
     * @param orders List of all orders
     * @return A convoy path -- a List of at least 1 mutually-adjacent convoy(s)
     */
    private static List<Order> convoyPath(Order firstConvoy, List<Order> convoyPath, List<Order> excludedOrders, List<Order> orders) {

        List<Order> adjacentConvoys = findAdjacentConvoys(firstConvoy, convoyPath, orders);

        adjacentConvoys.removeIf(excludedOrders::contains);

        if (adjacentConvoys.isEmpty())
            return convoyPath;

        convoyPath.add(adjacentConvoys.getFirst());
        excludedOrders.add(adjacentConvoys.getFirst());
        return convoyPath(adjacentConvoys.getFirst(), convoyPath, excludedOrders, orders);

    }

}
