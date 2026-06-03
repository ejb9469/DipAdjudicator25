import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract class of static <i>utility functions</i>
 * re: Order (& Collections-of) creation
 */
public abstract class OrdersFactory {


    public static Collection<Order> deepCopy(Collection<Order> orders) {
        // Default to List collection-type
        return deepCopy(List.copyOf(orders));
    }

    public static List<Order> deepCopy(List<Order> orders) {
        return (new ArrayList<>(orders)).stream().map(Order::new).collect(Collectors.toList());
    }

    public static Set<Order> deepCopy(Set<Order> orders) {
        return (new HashSet<>(orders)).stream().map(Order::new).collect(Collectors.toSet());
    }


    /**
     * 'Conforms' the order of elements in `orders` to the order in `orderedOrders`.<br>
     * Returns the result; this method does not mutate either Collection.<br><br>
     *
     * Element equality is derived from UNIT POSITION (`pos0`) only
     *
     * @param orders Collection of Orders (used for content)
     * @param orderedOrders <i><u>List</u></i> of Orders (used for ordering)
     * @return A new List containing the elements of [Collection `orders`] in the order of [List `orderedOrders`]
     */
    public static List<Order> conformOrder(Collection<Order> orders, List<Order> orderedOrders) {

        // null for either field returns back null always, so that NullPointerException will be thrown above
        if (orders == null || orderedOrders == null)
            return null;

        // Don't allow the function to proceed if the sizes mismatch
        else if (orders.size() != orderedOrders.size())
            throw new IllegalStateException(String.format(
                    "[`static List<Order> %s::conformOrder(Collection<Order>, List<Order>)`]\n\t==>collection size mismatch! (orders.length=%d, orderedOrders.length=%d)\n",
                    "Orders", orders.size(), orderedOrders.size()));

        // reflect back an empty list if Orders is empty
        else if (orders.isEmpty())
            return new ArrayList<>();

        // conform `orders` to the order of elements in `orderedOrders`
        Order[] ordersArr = new Order[orders.size()];  // arrays are the most efficient solution here
        for (Order order : orders) {
            for (int i = 0; i < orderedOrders.size(); i++) {
                if (order.pos0 == orderedOrders.get(i).pos0) {
                    ordersArr[i] = order;
                    break;
                }
            }
        }

        return new ArrayList<>(Arrays.asList(ordersArr));

    }


    /**
     * Computes a new Set of unique Orders in a given 2D Collection
     * @param ordersBag Collection of Orders Collections
     * @return A new Set of all unique Orders in 2D Collection `ordersBag`
     */
    public static Set<Order> uniq(Collection<Collection<Order>> ordersBag) {

        Set<Order> finalBag = new HashSet<>();

        for (Collection<Order> orders : ordersBag) {
            for (Order order : orders) {

                for (Collection<Order> orders2 : ordersBag) {
                    if (orders2 == orders) continue;
                    if (!orders2.contains(order)) {
                        finalBag.add(order);
                        break;
                    }
                }

            }
        }

        return finalBag;

    }


}
