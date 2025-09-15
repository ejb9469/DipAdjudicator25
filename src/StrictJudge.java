import java.util.Collection;

public class StrictJudge extends Judge {

    @Override
    public void judge() {

        this.cleanseOrders();
        super.judge();

    }

    protected void cleanseOrders() {

        Collection<Order> cleanOrders = Orders.cleanse(this.orders);
        this.orders.clear();
        this.orders.addAll(cleanOrders);

    }

}