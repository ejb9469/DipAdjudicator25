import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Abstract class of static utility functions re: Orders and Collections of Orders
 */
public abstract class Orders {

    /**
     * Checks the validity of an Order
     * @param order Order to check
     * @return True if `order` is valid, false otherwise
     */
    public static boolean orderIsValid(Order order) {

        // Does not check for appropriate-ness of Convoys
        // Does not check for adjacency of Moves
        switch (order.orderType) {

            case MOVE -> {
                if (order.pos0 == order.pos1)
                    // Units cannot order to their own location
                    // Locations in `Province` are not adjacent to themselves regardless,
                    // ... but for posterity's sake, we deem these moves illegal
                    return false;
                if (order.unitType == UnitType.ARMY && order.pos1.geography == Geography.WATER)
                    // Armies cannot go into water
                    return false;
                else if (order.unitType == UnitType.ARMY && order.pos1.coastType == CoastType.SPLIT)
                    // Armies cannot traverse split coasts
                    return false;
                else if (order.unitType == UnitType.FLEET && order.pos1.geography == Geography.INLAND)
                    // Fleets cannot go inland
                    return false;
                else if (order.unitType == UnitType.FLEET && !order.pos0.isAdjacentTo(order.pos1))
                    // Fleets cannot skip provinces (i.e. cannot be convoyed)
                    // This statement should also prevent MOVEs like F Mar - Spa/nc
                    // (but a move like F Mar S Gas - Spa/nc should succeed b/c of the SUPPORT conditions below)
                    return false;
                else
                    return true;
            }

            case SUPPORT, CONVOY -> {  // TODO: Separate Supports from Convoys (if/once they can no longer be handled identically)
                // Moves from Pos1-->Pos1 are illegal,
                // and support-holds are formatted with `pos2` == null
                if (order.pos1 == order.pos2)
                    return false;
                // !! Support & convoy orders must be adjacent to their dest. location !!
                // Fleets issuing supports can break split-coast adjacency rules, so use the appropriate helper method
                // Fleets must also be "adjacent by sea" when coast-crawling
                // ... [`.isAdjacentToIgnoreSplitCoast(...)`]
                if (order.pos2 == null)  // Handle support holds...
                    return (order.pos0.isAdjacentToIgnoreSplitCoast(order.pos1) &&
                            (order.unitType != UnitType.FLEET || Province.adjacentBySea(order.pos0, order.pos1)));
                else  // ...Handle support-moves
                    return (order.pos0.isAdjacentToIgnoreSplitCoast(order.pos2) &&
                            (order.unitType != UnitType.FLEET || Province.adjacentBySea(order.pos0, order.pos2)));
            }

            case HOLD -> {
                // Hold orders must have no location fields
                return order.pos1 == null && order.pos2 == null;
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

    /**
     * Removes all invalid Orders from a given Collection of Orders, and returns the Orders removed from the Collection
     * @param orders Collection of Orders
     * @return A Collection of the Orders removed from `orders`
     */
    // MUTATOR \\
    public static Collection<Order> cleanse(Collection<Order> orders) {

        Collection<Order> invalidOrders = new ArrayList<>();
        for (Order order : orders) {
            if (!Orders.orderIsValid(order))
                invalidOrders.add(order);
        }

        orders.removeAll(invalidOrders);
        return invalidOrders;

    }

    /**
     * Searches for Orders of a given OrderType, returns results
     * @param orderType OrderType to prune for
     * @param orders Collection of Orders to search
     * @return New Collection of Orders: results of pruning `orders` for Orders of `orderType`
     */
    // NOT A MUTATOR \\
    public static Collection<Order> pruneForOrderType(OrderType orderType, Collection<Order> orders) {

        Collection<Order> newOrders = new ArrayList<>();
        for (Order order : orders) {
            if (orderType == order.orderType)
                newOrders.add(order);
        }

        return newOrders;

    }


    /**
     * Searches for & returns the first Order at a given Position
     * @param pos Position to search for
     * @param orders Collection of Orders to search
     * @return First Order found at `pos` in `orders`
     */
    public static Order locateUnitAtPosition(Province pos, Collection<Order> orders) {

        for (Order order : orders) {
            if (order.pos0 == pos)
                return order;
            else if (Province.equalsIgnoreCoast(order.pos0, pos))  // e.g. Spa, Spa/nc, & Spa/sc are all "one province" for battles purposes
                return order;
        }

        return null;

    }

    /**
     * Searches for & returns a Collection of Movers to a given Position
     * @param pos Position to search for
     * @param orders Collection of Orders to search
     * @return Collection of Movers to `pos` in `orders`
     */
    public static Collection<Order> locateUnitsMovingToPosition(Province pos, Collection<Order> orders) {

        Collection<Order> ordersOut = new ArrayList<>();

        for (Order order : orders) {

            if (order.orderType != OrderType.MOVE && order.orderType != OrderType.RETREAT)
                continue;

            if (order.pos1 == pos)
                ordersOut.add(order);
            else if (Province.equalsIgnoreCoast(order.pos1, pos))  // e.g. Spa, Spa/nc, & Spa/sc are all "one province" for battles purposes
                ordersOut.add(order);

        }

        return ordersOut;

    }

    /**
     * Searches for & returns the 'other' Order participating in the same Head-to-Head Battle as a given Move Order
     * @param moveOrder Move Order
     * @param orders Collection of Orders to search
     * @return `moveOrder`'s Head-to-Head "opponent"
     */
    public static Order locateHeadToHead(Order moveOrder, Collection<Order> orders) {

        if (moveOrder.orderType != OrderType.MOVE)
            throw new IllegalArgumentException(String.format("`locateHeadToHead()` called on non-move Order: %s", moveOrder));

        for (Order order2 : orders) {

            if (order2.equals(
                    moveOrder) || order2.orderType != OrderType.MOVE)
                continue;
            if (order2.pos1 == moveOrder.pos0 && order2.pos0 == moveOrder.pos1)
                return order2;
            else if (Province.equalsIgnoreCoast(order2.pos1, moveOrder.pos0) &&
                    Province.equalsIgnoreCoast(order2.pos0, moveOrder.pos1))
                return order2;

        }

        return null;

    }

    /**
     * Searches for & returns the Order corresponding to a given Support / Convoy Order
     * @param supportOrConvoyOrder Support or Convoy Order
     * @param orders Collection of Orders to search
     * @return Order matching the specifications of `supportOrConvoyOrder`
     */
    public static Order locateCorresponding(Order supportOrConvoyOrder, Collection<Order> orders) {

        if (supportOrConvoyOrder.orderType != OrderType.SUPPORT &&
            supportOrConvoyOrder.orderType != OrderType.CONVOY) {
            throw new IllegalArgumentException(String.format("`locateCorresponding()` called on non-support-or-convoy Order: %s", supportOrConvoyOrder));
        }

        // Handle Support-Holds separately
        if (supportOrConvoyOrder.orderType == OrderType.SUPPORT && supportOrConvoyOrder.pos2 == null) {
            for (Order order : orders) {
                if (order.equals(supportOrConvoyOrder) || order.orderType == OrderType.MOVE)
                    continue;
                if (order.pos0 == supportOrConvoyOrder.pos1)
                    return order;
            }
        } else {  // Handle Support-Moves and Convoys
            for (Order order : orders) {
                if (order.equals(supportOrConvoyOrder) || order.orderType != OrderType.MOVE)
                    continue;
                if (order.pos0 == supportOrConvoyOrder.pos1 && order.pos1 == supportOrConvoyOrder.pos2)
                    return order;
            }
        }

        return null;

    }

    /**
     * Searches for a Convoy Order adjacent to & corresponding to a given Move Order, returns true if found
     * @param moveOrder Move Order
     * @param orders Collection of Orders to search
     * @return True if found adjacent & correspondent Convoy fleet to `moveOrder`, false otherwise
     */
    public static boolean adjacentMatchingConvoyFleetExists(Order moveOrder, Collection<Order> orders) {

        for (Order order2 : orders) {

            if (order2.equals(moveOrder))
                continue;

            if (order2.pos0.isAdjacentTo(moveOrder.pos0) &&
                    order2.orderType == OrderType.CONVOY &&
                    order2.unitType == UnitType.FLEET &&
                    order2.pos0.geography == Geography.WATER) {
                if (order2.pos1 == moveOrder.pos0 && order2.pos2 == moveOrder.pos1)
                    return true;
            }

        }

        return false;

    }

}
