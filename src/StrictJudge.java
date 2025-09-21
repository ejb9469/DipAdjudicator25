import java.util.Collection;

public class StrictJudge extends Judge {

    protected Collection<Order> voidedOrders = null;

    @Override
    public void judge() {

        this.cleanseOrders();
        super.judge();

    }

    protected void cleanseOrders() {
        // `Orders.cleanse()` is a mutator method,
        // It returns the invalid orders now removed from `this.orders`
        this.voidedOrders = Orders.cleanse(this.orders);
    }

}