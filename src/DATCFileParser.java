import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DATCFileParser extends DATCParser {


    public static final String TESTGAMES_DIR_PATH = "src/testgames/";
    public static final String TESTGAMES_SOLUTIONS_DIR_PATH = "src/testgames_solutions/";

    public String dirPath;
    public String solutionsDirPath;


    public DATCFileParser() {
        super();
        this.dirPath = TESTGAMES_DIR_PATH;
        this.solutionsDirPath = TESTGAMES_SOLUTIONS_DIR_PATH;
    }


    @Override
    public TestCase parse(String path) {

        if (path.isBlank() || this.dirPath.isBlank())
            return null;

        Path fullPath;
        try {
            fullPath = Paths.get(this.dirPath, path);
        } catch (InvalidPathException ex) {
            //ex.printStackTrace();
            System.err.printf("`%s`: Tried to read test case from %s, file not found\n", "DATCFileParser.java", this.dirPath+"/"+path);
            return null;
        }

        String contents;
        try {
            contents = Files.readString(Paths.get(fullPath.toUri()));
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        Path solutionFullPath;
        try {
            solutionFullPath = Paths.get(this.solutionsDirPath, path);
        } catch (InvalidPathException ex) {
            //ex.printStackTrace();
            System.err.printf("`%s`: Tried to read solution from %s, file not found\n", "DATCFileParser.java", this.solutionsDirPath+"/"+path);
            // Continue with execution
            solutionFullPath = null;
        }

        if (solutionFullPath == null)
            return super.parse(contents);
        else {

            String solutionContents;
            try {
                solutionContents = Files.readString(Paths.get(solutionFullPath.toUri()));
            } catch (IOException ex) {
                //ex.printStackTrace();
                return super.parse(contents);
            }

            TestCase testCase = super.parse(contents);
            testCase.setExpectedFields(this.parseSolution(solutionContents));
            return testCase;

        }

    }

    private boolean[] parseSolution(String solution) {

        String[] sourceLines = solution.lines().toArray(String[]::new);
        boolean[] verdicts = new boolean[sourceLines.length];
        for (int i = 0; i < sourceLines.length; i++)
            verdicts[i] = Boolean.parseBoolean(sourceLines[i]);

        return verdicts;

    }

    public List<TestCase> parseMany() {

        if (this.dirPath.isBlank())
            return null;

        Path fullPath;
        try {
            fullPath = Paths.get(this.dirPath);
        } catch (InvalidPathException ex) {
            //ex.printStackTrace();
            System.err.printf("`%s`: Tried to read test cases from %s, file(s) not found\n", "DATCFileParser.java", this.dirPath);
            return null;
        }

        File testCasesDir = new File(fullPath.toUri());
        File[] testCaseFiles = testCasesDir.listFiles();

        if (testCaseFiles == null)
            return null;

        List<TestCase> testCases = new ArrayList<>();
        for (File testCaseFile : testCaseFiles) {
            System.out.println("Reading from...\t\t["+testCaseFile.getPath()+"]");
            testCases.add(this.parse(testCaseFile.getName()));
        }

        return testCases;

    }


}
