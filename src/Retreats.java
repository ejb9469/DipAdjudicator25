import java.util.Collection;

/**
 * Abstract class of static utility functions re: Retreat & Retreat-Destroy (pull/piff) Orders
 */
public abstract class Retreats extends Orders {

    public static boolean suffocated(Order order, Collection<Order> orders) {

        if (order.orderType != OrderType.RETREAT)
            throw new IllegalArgumentException(String.format("`static %s:suffocated(...)` called on non-Retreat Order: %s",
                    "Retreats", order));

        /*for (Order order2 : orders) {

        }*/

        return false;  // TODO: Maybe a `order.suffocated` field instead? (akin to `order.dislodged`)

    }

}
