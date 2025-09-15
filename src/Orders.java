import java.util.ArrayList;
import java.util.Collection;

public abstract class Orders {


    public static Collection<Order> cleanse(Collection<Order> orders) {

        Collection<Order> orders2 = new ArrayList<>();
        for (Order order : orders) {
            if (Orders.orderIsValid(order))
                orders2.add(order);
        }

        return orders2;

    }

    public static boolean orderIsValid(Order order) {

        // Does not check for appropriate-ness of convoys

        switch (order.orderType) {

            case MOVE -> {
                if (order.pos0 == order.pos1)
                    // Units cannot order to their own location
                    // Locations in `Province` are not adjacent to themselves regardless,
                    // ... but for posterity's sake, we deem these moves illegal
                    return false;
                if (order.unitType == UnitType.ARMY && order.pos1.isWater())
                    // Armies cannot go into water
                    return false;
                else if (order.unitType == UnitType.FLEET && !order.pos1.isWater() && !order.pos1.isCoastal())
                    // Fleets cannot go inland
                    return false;
                else if (order.unitType == UnitType.FLEET && !order.pos0.isAdjacentTo(order.pos1))
                    // Fleets cannot skip provinces (i.e. cannot be convoyed)
                    return false;
                return true;
            }

            case SUPPORT, CONVOY -> {
                // Moves from Pos1-->Pos1 are illegal,
                // and support-holds are formatted with `pos2` == null
                if (order.pos1 == order.pos2)
                    return false;
                // Support & convoy orders must be adjacent to their dest. location
                if (!order.pos0.isAdjacentTo(order.pos2))
                    return false;
                return true;
            }

            case HOLD -> {
                // Hold orders must have no location fields
                return order.pos0 == null && order.pos1 == null;
            }

            case RETREAT -> {
                // Retreat orders must be adjacent to their dest. location
                // (No convoys for Retreat orders!)
                if (order.pos0 != order.pos2)
                    return false;
                // Must pass 'Move order tests'
                Order dummyMove = new Order(order);
                dummyMove.orderType = OrderType.MOVE;
                return orderIsValid(dummyMove);
            }

            default -> {
                return false;
            }

        }

    }


    public static Collection<Order> pruneForOrderType(OrderType orderType, Collection<Order> orders) {

        Collection<Order> newOrders = new ArrayList<>();
        for (Order order : orders) {
            if (orderType == order.orderType)
                newOrders.add(order);
        }

        return newOrders;

    }


    public static Order unitAtPosition(Province pos, Collection<Order> orders) {

        for (Order order : orders) {
            if (order.pos0 == pos)
                return order;
        }

        return null;

    }

    public static Collection<Order> unitsMovingToPosition(Province pos, Collection<Order> orders) {

        Collection<Order> ordersOut = new ArrayList<>();

        for (Order order : orders) {
            if (order.orderType != OrderType.MOVE && order.orderType != OrderType.RETREAT)
                continue;

            if (order.pos1 == pos)
                ordersOut.add(order);

        }

        return ordersOut;

    }

    public static Order locateHeadToHead(Order order, Collection<Order> orders) {

        if (order.orderType != OrderType.MOVE)
            throw new IllegalStateException(String.format("`locateHeadToHead()` called on non-move Order: %s", order));

        for (Order order2 : orders) {
            if (order2.equals(order) || order2.orderType != OrderType.MOVE)
                continue;
            if (order2.pos1 == order.pos0 && order2.pos0 == order.pos1)
                return order;
        }

        return null;

    }

    public static boolean adjacentMatchingConvoyFleetExists(Order order, Collection<Order> orders) {

        for (Order order2 : orders) {
            if (order2.equals(order))
                continue;

            if (order2.pos0.isAdjacentTo(order.pos0) &&
                    order2.orderType == OrderType.CONVOY &&
                    order2.unitType == UnitType.FLEET &&
                    order2.pos0.isWater()) {
                if (order2.pos1 == order.pos0 && order2.pos2 == order.pos1)
                    return true;
            }

        }

        return false;

    }

}
