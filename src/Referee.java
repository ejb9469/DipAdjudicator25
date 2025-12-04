import java.util.*;

/**
 * A subclass of Judge which rules on Paradoxes.
 */
public class Referee extends Judge {

    public static final int NUM_TRIALS = 2500;

    protected final Collection<Set<Order>> permutations;


    public Referee() {
        super();
        this.permutations = new HashSet<>();
    }

    public Referee(Collection<Order> orders) {
        super(orders);
        this.permutations = new HashSet<>();
    }


    @Override
    public void judge() {

        Collection<Order> originalOrders = Orders.deepCopy(this.orders);

        for (int i = 1; i <= NUM_TRIALS; i++) {
            // deep clone the orders
            List<Order> ordersClone = new ArrayList<>(Orders.deepCopy(this.orders));
            Collections.shuffle(ordersClone);  // generate a random permutation
            // evaluate
            this.orders = ordersClone;
            super.judge();
            // will only truly add to `permutations` if the resolution is unique,
            // ... because we are using a Set [equality determined by `Order::hashcode()`]
            permutations.add(new HashSet<>(Set.copyOf(
                    Orders.deepCopy(this.orders))));
            // reset the judge
            this.orders = Orders.deepCopy(originalOrders);
        }

        if (!orders.isEmpty() && permutations.isEmpty())
            throw new IllegalStateException("No permutations found!");

        else if (permutations.size() == 1) {

            // set `this.orders` to the 1 and only resolution
            for (Set<Order> decisiveResolution : permutations)  // trick to grab first (& only) element; not a real loop
                this.orders = decisiveResolution;

        } else if (permutations.size() > 1) {

            /* The current Referee rules for multiple resolutions -
                1. Compound all replacement orders into one set where all Szykman holds are present
                2. The remaining orders will be taken from the set with the most # of Orders with `resolved=true`
            */

            // add all szykman holds from all permutations / resolutions
            Set<Order> szykmanHolds = new HashSet<>();
            for (Set<Order> permutation : permutations) {
                for (Order order : permutation) {
                    if (order.getSnapshot() != null)
                        szykmanHolds.add(order);
                }
            }

            // build out the remaining orders from here
            Collection<Order> heuristicOrders = new ArrayList<>(szykmanHolds);

            // find order set with most `resolved=true`
            Set<Order> mostResolvedPerm = new HashSet<>();  // blank instead of null
            int mostNumResolved = -1;
            for (Set<Order> permutation : permutations) {
                int numResolved = 0;
                for (Order order : permutation)
                    numResolved += order.resolved ? 1 : 0;  // 1 if true, 0 if false (ternary operator)
                if (mostResolvedPerm.isEmpty() || numResolved > mostNumResolved) {
                    mostResolvedPerm = permutation;
                    mostNumResolved = numResolved;
                }
            }

            // populate the new order set with the winner
            for (Order order : mostResolvedPerm) {
                if (Orders.locateUnitAtPosition(order.pos0, szykmanHolds) == null)
                    heuristicOrders.add(order);
            }

            // assign to `this.orders`
            this.orders = heuristicOrders;

        }

    }


}