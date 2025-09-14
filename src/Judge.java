import java.util.*;

public class Judge {

    // The adjudication program needs to handle the following situations:
        // a. An order that is not indirectly dependent on itself
        // b. An order that is indirectly dependent on itself, but there is still exactly 1 resolution
        // c. An order that is indirectly dependent on itself, but there are 0 or 2 possible resolutions

    private final List<Order> orders;


    public Judge(List<Order> orders) {
        this.orders = orders;
    }

    public Judge() {
        this.orders = new ArrayList<>();
    }


    private List<Order> cycle = new ArrayList<>();
    private int         recursionHits = 0;
    private boolean     uncertain = false;


    public List<Order> getOrders() {
        return orders;
    }


    public void judge() {

        for (Order order : orders)
            resolve(order, true);

    }

    private boolean adjudicate(Order order, boolean optimistic) {

        // Handle MOVE orders
        if (order.orderType == OrderType.MOVE) {

            int attackStrength;

            Order headToHead = Orders.locateHeadToHead(order, this.orders);
            if (headToHead != null) {  // HEAD-TO-HEAD Battle

                // Calculate Move order's ATTACK STRENGTH
                // [Must be greater than... a. the Defend Strength of the opposing mover, and
                //                          b. the Prevent Strength of all movers competing for the same area]
                attackStrength = calculateAttackStrength(order, optimistic, true, orders);

                // Calculate opponent's DEFEND STRENGTH
                int opponentDefendStrength = calculateDefendStrength(headToHead, !optimistic, orders);

                if (attackStrength > opponentDefendStrength) {

                    Collection<Order> otherOpponents = Orders.unitsMovingToPosition(order.pos1, orders);

                    // Calculate PREVENT STRENGTH of all 'opponents' (other movers going  to the same destination)
                    // `champion` will be true if our Move order is the greatest (with no ties)
                    boolean champion = true;
                    for (Order order2 : otherOpponents) {
                        int opponentPreventStrength = calculatePreventStrength(order2, !optimistic, orders);
                        if (opponentPreventStrength >= attackStrength) {
                            champion = false;
                            break;
                        }
                    }
                    return champion;

                } else {  // Lost to opponent mover, return false
                    return false;
                }

            } else {  // NON-HEAD-TO-HEAD Battle

                // Calculate Move order's ATTACK STRENGTH
                // [Must be greater than... a. the Hold Strength of the area, and
                //                          b. the Prevent Strength of all movers competing for the same area]
                attackStrength = calculateAttackStrength(order, optimistic, false, orders);

                // Calculate destination's HOLD STRENGTH
                int destHoldStrength = calculateHoldStrength(order.pos1, !optimistic, orders);

                if (attackStrength > destHoldStrength) {

                    Collection<Order> otherOpponents = Orders.unitsMovingToPosition(order.pos1, orders);

                    // Calculate PREVENT STRENGTH of all 'opponents' (other movers going to the same destination)
                    // `champion` will be true if our Move order is the greatest (with no ties)
                    boolean champion = true;
                    for (Order order2 : otherOpponents) {
                        if (order2.equals(order)) continue;
                        int opponentPreventStrength = calculatePreventStrength(order2, !optimistic, orders);
                        if (opponentPreventStrength >= attackStrength) {
                            champion = false;
                            break;
                        }
                    }
                    return champion;

                } else {  // Lost on hold strength, return false
                    return false;
                }

            }

        }

        // Handle SUPPORT orders
        else if (order.orderType == OrderType.SUPPORT) {

            for (Order order2 : orders) {

                if (order2.equals(order) || order2.orderType != OrderType.MOVE)
                    continue;

                if (order2.pos1 != order.pos0)
                    continue;

                if (pathSuccessful(order2, optimistic, orders) &&
                        order2.owner != order.owner &&
                        order.pos2 != order2.pos0) {
                    return false;
                } else if (resolve(order2, !optimistic)) {
                    return false;
                }

            }

            return true;

        }

        // Handle HOLDS and CONVOYS
        else if (order.orderType == OrderType.CONVOY || order.orderType == OrderType.HOLD) {

            Collection<Order> assailants = Orders.unitsMovingToPosition(order.pos0, orders);
            for (Order order2 : assailants) {
                if (order2.equals(order)) continue;
                if (resolve(order2, !optimistic)) {
                    return false;
                }
            }

            return true;

        }

        // Handle RETREAT & PIFF orders (retreats phase)
        else if (order.orderType == OrderType.RETREAT) {

            // Retreated off the board; 'piffed'
            // technically successful order; SPECIAL CASE, will be handled at the unit removal level
            if (order.pos1 == null)
                return true;

            // The retreat fails if & only if there is another (dislodged) unit retreating there
            Collection<Order> bouncers = Orders.unitsMovingToPosition(order.pos1, orders);
            return (bouncers.size() > 1);

        }

        // Unknown / impossible order type, throw exception
        System.err.println("LINE 158 :: Judge.java");
        throw new IllegalStateException("Impossible OrderType");

    }

    private boolean resolve(Order order, boolean optimistic) {

        if (order.resolved)
            // Resolution already exists
            return order.verdict;


        if (cycle.contains(order)) {
            // We already concluded this order is in a cycle,
            // ... which we cannot yet resolve
            // Result is based on uncertain information
            uncertain = true;
            // Success if optimistic
            return optimistic;
        }

        if (order.visited) {
            // We hit cyclic dependency
            // Success if optimistic
            cycle.add(order);
            recursionHits++;
            uncertain = true;
            return optimistic;
        }

        order.visited = true;  // Prevent endless recursion; block from recursing to self
        int cycleLen_Old = cycle.size();
        int recursionHits_Old = recursionHits;
        boolean uncertain_Old = uncertain;
        uncertain = false;
        boolean optResult = this.adjudicate(order, true);
        boolean pesResult;
        /*// Try to avoid a 2nd adjudication for performance
        if (optResult && uncertain)
            pesResult = this.adjudicate(order, false);
        else
            pesResult = optResult;*/
        pesResult = this.adjudicate(order, false);
        order.visited = false;  // Un-block recursion for this Order

        if (optResult == pesResult) {
            // We have a single resolution
            // Delete any cycle info that was found in recursion
            cycle.subList(0, cycleLen_Old).clear();
            recursionHits = recursionHits_Old;
            // The uncertain variable must be unaltered, because the order is now resolved
            uncertain = uncertain_Old;
            // Store the result and return it
            order.verdict = optResult;
            order.resolved = true;
            return optResult;
        }

        if (cycle.contains(order)) {
            // We returned from recursion, where this order hit the cycle,
            // ... and we didn't receive any resolution
            recursionHits--;
        }

        if (recursionHits == recursionHits_Old) {
            // We have sufficiently retreated from recursion such that ...
            // ... this order is the ancestor of the whole cycle
            // Apply the backup rule on all orders in the cycle
            this.backupRule(cycle.subList(0, cycleLen_Old));
            cycle.subList(0, cycleLen_Old).clear();
            uncertain = uncertain_Old;
            // The backup rule may not have resolved THIS order
            return this.resolve(order, optimistic);
        } else {
            // We returned from a situation where a cycle was detected
            // However, this order is not the ancestor of the whole cycle
            // We further retreat from recursion
            if (!cycle.contains(order))
                cycle.add(order);
            return optimistic;
        }

    }

    private void backupRule(List<Order> cyclicalOrders) {

        boolean areAllMovers = true;
        for (Order order : cyclicalOrders) {
            if (order.orderType != OrderType.MOVE) {
                areAllMovers = false;
                break;
            }
        }

        if (areAllMovers) {
            for (Order order : orders) {
                order.resolved = true;
                order.verdict = true;
            }
        } else {
            szykmanRule(orders);
        }

    }

    private void szykmanRule(List<Order> cyclicalOrders) {

        for (Order order : cyclicalOrders) {
            if (order.orderType == OrderType.CONVOY) {
                order.resolved = true;
                order.verdict = false;
            }
        }

    }


    private boolean pathSuccessful(Order moveOrder, boolean optimistic, Collection<Order> orders) {

        if (moveOrder.pos0.isAdjacentTo(moveOrder.pos1)) {

            if (moveOrder.unitType == UnitType.ARMY && moveOrder.pos1.isWater())
                return false;
            else if (moveOrder.unitType == UnitType.FLEET && !moveOrder.pos1.isWater() && !moveOrder.pos1.isCoastal())
                return false;
            else
                return true;

        } else if (Orders.adjacentMatchingConvoyFleetExists(moveOrder, orders)) {

            List<Order> convoyPath = Convoys.drawConvoyPath(moveOrder, (List<Order>) orders);
            boolean allConvoysSuccessful = true;
            for (Order convoyOrder : convoyPath) {
                if (!resolve(convoyOrder, optimistic)) {
                    allConvoysSuccessful = false;
                    break;
                }
            }

            return allConvoysSuccessful;
            // TODO: Change to support Multiple Convoy Routes:
            //         Check if there is a different convoy route,
            //          ... comprised of fleets that are all successful.
            //         Continue to check until `Convoys.drawConvoyPath()` fails.

        } else
            return false;

    }

    private int tallySuccessfulSupports(Order order, boolean optimistic, Collection<Order> orders) {

        int supports = 0;
        if (order.orderType == OrderType.MOVE) {  // SUPPORT to MOVE

            for (Order order2 : orders) {
                if (order2.equals(order) || order2.orderType != OrderType.SUPPORT)
                    continue;

                if (order2.pos1 == order.pos0 && order2.pos2 == order.pos1) {
                    if (resolve(order2, optimistic))
                        supports++;
                }

            }

        } else {  // SUPPORT to HOLD

            for (Order order2 : orders) {
                if (order2.equals(order) || order2.orderType != OrderType.SUPPORT)
                    continue;

                if (order2.pos1 == order.pos0 && order2.pos2 == null) {
                    if (resolve(order2, optimistic))
                        supports++;
                }

            }

        }

        return supports;

    }

    private int tallySuccessfulSupportsForeign(Order order, boolean optimistic, Nation forbiddenOwner, Collection<Order> orders) {

        int supports = 0;
        if (order.orderType == OrderType.MOVE) {  // SUPPORT to MOVE

            for (Order order2 : orders) {
                if (order2.equals(order) || order2.orderType != OrderType.SUPPORT || order2.owner == forbiddenOwner)
                    continue;

                if (order2.pos1 == order.pos0 && order2.pos2 == order.pos1) {
                    if (resolve(order2, optimistic))
                        supports++;
                }

            }

        } else {  // SUPPORT to HOLD

            for (Order order2 : orders) {
                if (order2.equals(order) || order2.orderType != OrderType.SUPPORT || order2.owner == forbiddenOwner)
                    continue;

                if (order2.pos1 == order.pos0 && order2.pos2 == null) {
                    if (resolve(order2, optimistic))
                        supports++;
                }

            }

        }

        return supports;

    }

    private int calculateAttackStrength(Order order, boolean optimistic, boolean headToHead, Collection<Order> orders) {

        if (!pathSuccessful(order, optimistic, orders))
            return 0;

        if (Orders.unitAtPosition(order.pos1, orders) == null)
            return 1+tallySuccessfulSupports(order, optimistic, orders);

        Order destOrder = Orders.unitAtPosition(order.pos1, orders);
        if (!headToHead) {
            if (resolve(destOrder, optimistic)) {
                return 1+tallySuccessfulSupports(order, optimistic, orders);
            } else if (destOrder.owner == order.owner) {
                return 0;
            }
        }

        return 1+tallySuccessfulSupportsForeign(order, optimistic, destOrder.owner, orders);

    }

    private int calculateDefendStrength(Order order, boolean optimistic, Collection<Order> orders) {

        return 1+tallySuccessfulSupports(order, optimistic, orders);

    }

    private int calculatePreventStrength(Order order, boolean optimistic, Collection<Order> orders) {

        if (!pathSuccessful(order, optimistic, orders))
            return 0;

        Order headToHead = Orders.locateHeadToHead(order, orders);
        if (headToHead != null) {
            if (resolve(headToHead, optimistic))
                return 0;
        }

        return 1+tallySuccessfulSupports(order, optimistic, orders);

    }

    private int calculateHoldStrength(Province pos, boolean optimistic, Collection<Order> orders) {

        Order occupant = Orders.unitAtPosition(pos, orders);
        if (occupant == null)
            return 0;

        if (occupant.orderType == OrderType.MOVE) {
            if (resolve(occupant, optimistic))
                return 0;
            else
                return 1;
        }

        return 1+tallySuccessfulSupports(occupant, optimistic, orders);

    }

}