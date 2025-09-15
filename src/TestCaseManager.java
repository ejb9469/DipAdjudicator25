import java.util.ArrayList;
import java.util.List;

public class TestCaseManager {


    private final List<TestCase> testCases;
    private boolean prints;


    public TestCaseManager() {
        this.testCases = new ArrayList<>();
        this.prints = true;
    }

    public TestCaseManager(boolean isPrinting) {
        this.testCases = new ArrayList<>();
        this.prints = isPrinting;
    }


    public List<TestCase> getTestCases() {
        return this.testCases;
    }

    public boolean prints() {
        return this.prints;
    }

    public void togglePrint() {
        this.prints = !prints;
    }


    public void addTestCaseWithFields(TestCase testCase, boolean evalNow, boolean... expectedFields) {
        testCase.setExpectedFields(expectedFields);
        this.testCases.add(testCase);
        if (evalNow)
            testCase.eval(this.prints);
    }

    public void addTestCaseWithFields(TestCase testCase, boolean evalNow, boolean[]... expectedFields) {
        testCase.setExpectedFields(expectedFields);
        this.testCases.add(testCase);
        if (evalNow)
            testCase.eval(this.prints);
    }


    public int score() {
        int score = 0;
        for (TestCase testCase : this.testCases)
            score += testCase.getScore();
        return score;
    }

    public int size() {
        int size = 0;
        for (TestCase testCase : this.testCases)
            size += testCase.getOrders().size();
        return size;
    }


    public static void main(String[] args) {

        //example();
        System.out.println("----------------------------------------\n\n");

        TestCaseManager manager = new TestCaseManager(true);
        DATCFileParser fileParser = new DATCFileParser();  // Will grab from "src/testgames/" directory by default

        List<TestCase> testCases = fileParser.parseMany();
        manager.testCases.addAll(testCases);
        System.out.println("\n");
        for (TestCase testCase : manager.testCases)
            testCase.eval(manager.prints);

    }

    public static void example() {

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
        //testCaseC.eval(true);
        //manager.testCases.add(testCaseC);

        TestCase testCaseD = parser.parse("Germany: \n" +
                "A Berlin - Sweden\n" +
                "F Baltic Sea Convoys A Berlin - Sweden\n" +
                "F Prussia Supports F Baltic Sea\n" +
                "\n" +
                "Russia: \n" +
                "F Livonia - Baltic Sea\n" +
                "F Gulf of Bothnia Supports F Livonia - Baltic Sea");
        testCaseD.setExpectedFields(true, true, true, false, true);
        //testCaseD.eval(true);
        //manager.testCases.add(testCaseD);

        TestCase testCase_6C3 = parser.parse("Austria: \n" +
                "A Trieste - Serbia\n" +
                "A Serbia - Bulgaria\n" +
                "\n" +
                "Turkey: \n" +
                "A Bulgaria - Trieste\n" +
                "F Aegean Sea Convoys A Bulgaria - Trieste\n" +
                "F Ionian Sea Convoys A Bulgaria - Trieste\n" +
                "F Adriatic Sea Convoys A Bulgaria - Trieste\n" +
                "\n" +
                "Italy: \n" +
                "F Naples - Ionian Sea");
        testCase_6C3.setExpectedFields(true, true, true, true, true, true, false);
        testCase_6C3.eval(true);
        manager.testCases.add(testCase_6C3);

        TestCase testCase_6A7 = parser.parse("England: \n" +
                "F London - Belgium\n" +
                "F North Sea Convoys A London - Belgium");
        testCase_6A7.setExpectedFields(false, true);
        testCase_6A7.eval(true);
        manager.testCases.add(testCase_6A7);

        TestCase testCase_6A11 = parser.parse("Austria: \n" +
                "A Vienna - Tyrolia\n" +
                "\n" +
                "Germany: \n" +
                "A Munich - Tyrolia\n" +
                "\n" +
                "Italy: \n" +
                "A Venice - Tyrolia");
        testCase_6A11.setExpectedFields(false, false, false);
        testCase_6A11.eval(true);
        manager.testCases.add(testCase_6A11);

        TestCase testCase_6C1 = parser.parse("Turkey: \n" +
                "F Ankara - Constantinople\n" +
                "A Constantinople - Smyrna\n" +
                "A Smyrna - Ankara");
        testCase_6C1.setExpectedFields(true, true, true);
        testCase_6C1.eval(true);
        manager.testCases.add(testCase_6C1);

        for (TestCase tc : manager.testCases)
            tc.printNameAndScore();

    }


}