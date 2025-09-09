import java.util.ArrayList;
import java.util.Collection;

public abstract class Orders {

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

    public static boolean adjacentConvoyingFleetExists(Order order, Collection<Order> orders) {

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
