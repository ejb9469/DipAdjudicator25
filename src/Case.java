import java.util.List;

public interface Case {

    public      void                evaluate();
    public      void                evaluate(boolean prints);

    public      List<Order>         getOrders();
    public      String              getName();
    public      List<?>             getActualFields();

    //public      Object              getEval();

}
