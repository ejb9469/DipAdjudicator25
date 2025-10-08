import java.util.Collection;

public class RetreatsJudge extends Judge implements HomogeneousState {


    public RetreatsJudge() { super(); }

    public RetreatsJudge(Collection<Order> orders) { super(orders); }


    @Override
    protected boolean adjudicate(Order order, boolean optimistic) {

        enforceStasis();  // Called for all Orders, for each Order adjudication

        // Any T/F adjudication of this Order will un-dislodge it
        // We save the old value to (potentially) re-dislodge if an error / exception occurs (i.e. no simple T/F adjudication)
        boolean dislodged_Old = order.dislodged;
        order.dislodged = false;

        // Handle RETREAT orders (retreats phase)
        if (order.orderType == OrderType.RETREAT) {

            // Retreated off the board; 'pulled'
            // technically successful order; SPECIAL CASE, will be handled at the unit removal level
            if (order.pos1 == null) {
                pull(order);
                return true;
            }

            // Also retreated off the board, but involuntarily; 'piffed'
            // technically a failed Order; SPECIAL CASE, will be handled at the unit removal level
            if (Retreats.suffocated(order, this.orders)) {
                piff(order);
                return false;
            }

            // The retreat fails 'if & only if' there is another (dislodged) unit retreating there
            Collection<Order> bouncers = Orders.locateUnitsMovingToPosition(order.pos1, orders);
            return (bouncers.size() > 1);

        }

        else if (order.orderType == OrderType.DESTROY) {

            // All destroys are automatically successful
            // The implications of the destroy will be handled at the unit removal level
            pull(order);
            return true;

        }

        // If we have got to this point, we must insist on the old `Order.dislodged` value
        order.dislodged = dislodged_Old;

        // Since `RetreatsJudge` enforces homogeneity (only Retreats & Destroys) (ATM), ...
        // ... calling `Judge::adjudicate(...)` will likely proceed to its IllegalStateEx throw (again, ATM)
        // It's better to call Judge's method as a failsafe than to duplicate the code here
        return super.adjudicate(order, optimistic);

    }


    @Override
    public void enforceStasis() throws IllegalStateException {

        for (Order order : orders) {
            if (!order.dislodged || (order.orderType != OrderType.RETREAT && order.orderType != OrderType.DESTROY))
                throw new IllegalStateException(String.format(
                        "%s\n`%s:enforceStasis()`: OrderType is %s but only %s are allowed",
                        order.toString(),
                        this.getClass().getSimpleName(), order.orderType,
                        OrderType.RETREAT.name() + " and " + OrderType.DESTROY.name())
                );
        }

    }


    private void pull(Order order) {

        // TODO: Voluntary

    }

    private void piff(Order order) {

        // TODO: Involuntary

    }


}
