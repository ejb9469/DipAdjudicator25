import java.util.Collection;

public class StrictJudge extends Judge {

    @Override
    public void judge() {

        this.cleanse();
        super.judge();

    }

    protected void cleanse() {

        Collection<Order> cleanOrders = Orders.cleanse(this.orders);
        this.orders.clear();
        this.orders.addAll(cleanOrders);

    }

}