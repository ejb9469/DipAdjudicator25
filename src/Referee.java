import java.util.*;

public class Referee extends Judge {

    public static final boolean DEBUG_PRINT = false;

    // This constant represents the maximum `super.orders` size allowed, ...
    // ... b/c it is the permutation factor -- [k in `k!`] -- of `this.permutations`.
    // Without this hard cap, its size(n) would be `39916800` or greater (`10!`==`3628800`)
    // Perhaps `11!` or `12!` can be achieved with optimizations: the very nature of...
    // ... the permutation algorithm can be improved.
    public static final int MAX_K = 10;

    protected final   TestCase                                  testCase;
    protected final   Collection<List<Order>>                   permutations;
    protected final   Map<Set<Order>, Collection<List<Order>>>  resolutions  = new HashMap<>();


    public Referee() {
        super();
        this.testCase = null;
        this.permutations = new HashSet<>();
    }

    public Referee(Collection<Order> orders) throws TooIntensiveException {
        super(orders);
        this.testCase = null;
        this.permutations = generateOrderListingPermutations((List<Order>) orders);
    }

    public Referee(TestCase testCase) throws TooIntensiveException {
        super(testCase.getOrders());
        this.testCase = testCase;
        this.permutations = generateOrderListingPermutations(testCase.getOrders());
    }


    @Override
    public void judge() {

        // Generate all possible permutations of orders,
        // but do NOT generate them if permutations already exist (simply create a shallow copy)
        Collection<List<Order>> permutations;
        if (this.permutations.isEmpty())
            permutations = generateOrderListingPermutations(new ArrayList<>(this.orders));
        else
            permutations = new ArrayList<>(this.permutations);

        List<Order> originalOrders = new ArrayList<>(this.orders);
        Map<Set<Order>, Collection<List<Order>>> resolutions = new HashMap<>();

        if (permutations.isEmpty())
            return;  // no need to proceed further -- would just allocate unnecessary resources

        else if (permutations.size() == 1) {  // We can infer (orders.size() == 1)
            super.judge();  // simply treat the Referee like a default Judge
            Collection<List<Order>> pList = new HashSet<>();
            pList.add(new ArrayList<>(this.orders));
            resolutions.put(new HashSet<>(this.orders), pList);
        }

        else {  // (permutations.size() >= 2)
            for (List<Order> permutation : permutations) {
                this.orders = new ArrayList<>(permutation);
                super.judge();  // `Judge::resolve(...)` all orders
                Set<Order> orderSetSnapshot = new HashSet<>(this.orders);
                if (resolutions.containsKey(orderSetSnapshot))
                    resolutions.get(orderSetSnapshot).add(new ArrayList<>(this.orders));
                else {
                    Set<Order> snapshotDeepCopy = Orders.deepCopy(orderSetSnapshot);
                    resolutions.put(snapshotDeepCopy, new HashSet<>());
                    resolutions.get(orderSetSnapshot).add(new ArrayList<>(this.orders));
                }
            }
        }

        this.orders = originalOrders;
        this.resolutions.clear();
        this.resolutions.putAll(resolutions);

        for (Order order : this.orders)
            order.wipeMetaInf();

    }


    protected Collection<List<Order>> generateOrderListingPermutations(List<Order> ordersList) {

        /* Ideas to improve the permutation algorithm (to allow for greater `ordersList` sizes):
        *       1. Rewrite it iteratively.
        *           a. A good indicator for how severely performance could be impacted by recursion is...
        *               ...[CALLSTACK DEPTH]!! (check).
        *       2. Stop generating so many Heap Writes.
        *           a. Space complexity may sincerely be a limiting factor
        *           b. However, the tax on time complexity is probably more severe / limiting, ...
        *               ... Heap Writes are quite the slow operation, especially when dealing with Lists (ordered), ...
        *               ... and Collection sizes in the millions
        *           c. Many of these Collection copy-constructor calls are likely pointless, for example.
        *           d. `this.resolutions` could be a Set instead of a Map
        *       3. Refactor the `Referee` class AND permutation algorithm to never store ALL permutations in memory directly, ...
        *           ... unless absolutely necessary.
        * Remember, we are dealing with an upper range of [`9!` =< n >= `12!`] (atm), ...
        * ... which (imo) strongly indicates that algorithmic efficiency will be important here...
        * ... --> talking the difference between millions and billions, which is probably near our edge of realistic computation...
        */
        if (ordersList.size() > MAX_K)
            throw new TooIntensiveException(String.format("Referee cannot permute greater than `%s!`\t(attempted factor: `%d!`)", MAX_K, ordersList.size()));

        Collection<List<Order>> permutations = permute(ordersList);

        if (DEBUG_PRINT)
            System.out.println("`Referee.java` -- expected # permutations: " + Constants.factorial(ordersList.size()));

        return permutations;

    }

    private static Collection<List<Order>> permute(List<Order> ordersList) {
        Collection<List<Order>> permutations = new ArrayList<>(Constants.factorial(ordersList.size()));
        permute(ordersList, 0, permutations);
        return permutations;
    }

    private static void permute(List<Order> ordersList, int k, Collection<List<Order>> storage) {
        for (int i = k; i < ordersList.size(); i++) {
            Collections.swap(ordersList, i, k);
            permute(ordersList, k+1, storage);
            Collections.swap(ordersList, k, i);
        }
        if (k == ordersList.size()-1)
            storage.add(new ArrayList<>(ordersList));
    }

}