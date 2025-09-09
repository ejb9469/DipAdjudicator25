import java.util.ArrayList;
import java.util.List;

public class TestCaseManager {

    private final List<TestCase> testCases;


    public TestCaseManager() {
        this.testCases = new ArrayList<>();
    }


    public static void main(String[] args) {

        TestCaseManager manager = new TestCaseManager();

        TestCase testCaseA = new TestCase("Example 1",
                new Order(Nation.ENGLAND, UnitType.FLEET, Province.ENG, OrderType.MOVE, Province.Bre),
                new Order(Nation.FRANCE, UnitType.FLEET, Province.Bre, OrderType.MOVE, Province.MAO),
                new Order(Nation.ITALY, UnitType.FLEET, Province.MAO, OrderType.MOVE, Province.ENG),
                new Order(Nation.AUSTRIA, UnitType.FLEET, Province.Wal, OrderType.MOVE, Province.ENG),
                new Order(Nation.GERMANY, UnitType.FLEET, Province.IRI, OrderType.SUPPORT, Province.Wal, Province.ENG));

        testCaseA.setExpectedFields(false, false, false, true, true);
        testCaseA.eval(true);
        manager.testCases.add(testCaseA);

        TestCase testCaseB = new TestCase("Example 2",
                new Order(Nation.AUSTRIA, UnitType.FLEET, Province.Tri, OrderType.HOLD),
                new Order(Nation.ITALY, UnitType.ARMY, Province.Ven, OrderType.MOVE, Province.Tri),
                new Order(Nation.ITALY, UnitType.ARMY, Province.Tyr, OrderType.SUPPORT, Province.Ven, Province.Tri),
                new Order(Nation.GERMANY, UnitType.ARMY, Province.Mun, OrderType.MOVE, Province.Tyr),
                new Order(Nation.RUSSIA, UnitType.ARMY, Province.Sil, OrderType.MOVE, Province.Mun),
                new Order(Nation.RUSSIA, UnitType.ARMY, Province.Ber, OrderType.SUPPORT, Province.Sil, Province.Mun));

        testCaseB.setExpectedFields(true, false, false, false, true, true);
        testCaseB.eval(true);
        manager.testCases.add(testCaseB);


        TestCaseParser parser = new DATCParser();
        TestCase testCaseC = parser.parse(
                "Germany: \n" +
                "A Berlin Supports A Munich - Silesia\n" +
                "F Kiel Supports A Berlin\n" +
                "A Munich - Silesia\n" +
                "\n" +
                "Russia: \n" +
                "F Baltic Sea Supports A Prussia - Berlin\n" +
                "A Prussia - Berlin"
        );
        testCaseC.setExpectedFields(false, true, true, true, false);
        testCaseC.eval(true);
        manager.testCases.add(testCaseC);

        TestCase testCaseD = parser.parse("Germany: \n" +
                "A Berlin - Sweden\n" +
                "F Baltic Sea Convoys A Berlin - Sweden\n" +
                "F Prussia Supports F Baltic Sea\n" +
                "\n" +
                "Russia: \n" +
                "F Livonia - Baltic Sea\n" +
                "F Gulf of Bothnia Supports F Livonia - Baltic Sea");
        testCaseD.setExpectedFields(true, true, true, false, true);
        testCaseD.eval(true);
        manager.testCases.add(testCaseD);

        for (TestCase tc : manager.testCases)
            tc.printNameAndScore();

    }


    public List<TestCase> getTestCases() {
        return testCases;
    }

}