import java.util.ArrayList;
import java.util.List;

public class TestCaseReferee extends TestCase {


    public TestCaseReferee(String name, Order... orders) {
        super(name, orders);
    }

    public TestCaseReferee(String name, List<Order> orders) {
        super(name, orders);
    }

    public TestCaseReferee(TestCase testCase) {
        super(testCase);
    }

    @Override
    public void appointJudge() {

        Judge judge;
        if (!orders.isEmpty())
            judge = new Referee(new ArrayList<>(orders));
        else
            judge = new Referee();

        judge.judge();
        judge.orders = OrdersFactory.conformOrder(judge.orders, this.orders);

        for (Order order : judge.getOrders())
            actualFields.add(new boolean[]{order.verdict});  // Could expand with more fields later

    }


}
