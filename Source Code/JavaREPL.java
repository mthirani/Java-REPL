
import com.sun.source.util.JavacTask;
import javax.tools.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class JavaREPL {

    public static final String tempDir = System.getProperty("java.io.tmpdir");
    public static ClassLoader classLoader;

    /*
        This is the main method
     */
    public static void main(String[] args) throws IOException {
        exec(new InputStreamReader(System.in));
    }

    /*
        This method reads each and every line inputted by the user and performs the execution of statement accordingly
        Each statement (if valid)  is converted to java source code and stored in the file else storing the blank java source file
     */
    public static void exec(Reader r) throws IOException {
        BufferedReader stdin = new BufferedReader(r);
        NestedReader reader = new NestedReader(stdin);
        int classNumber = 0;
        while (true) {
            System.out.print("> ");
            String readStr = reader.getNestedString();
            if (readStr == null)
                return;
            readStr = readStr.trim();
            if(readStr.startsWith("print") && readStr.endsWith(";")){
                int index = readStr.lastIndexOf(";");
                String printStat = readStr.substring(0, index);
                int length = "print ".length();
                String printStatement = "System.out.println(" + printStat.substring(length) + ");";
                readStr = printStatement;
            }
            else if(readStr.startsWith("print")){
                int length = "print ".length();
                readStr = "System.out.println(" + readStr.substring(length) + ");";
            }
            if(readStr.equals(""))
                continue;
            String tempFileName = "Test.java";
            String definition = "public static " + readStr;
            String statement = "";
            String code = getCode("Test", "", "", definition, "");
            writeToFile(tempFileName, code);
            String errorParse = compileCode(tempFileName, "parse");
            if(errorParse != null){
                statement = readStr;
                definition = "";
            }
            String nameClass = "Interp_" + classNumber;
            int superClassNumber = classNumber - 1;
            String superClass = "";
            if(classNumber > 0) {
                superClass = "Interp_" + superClassNumber;
                code = getCode(nameClass, " extends ", superClass, definition, statement);
            }
            else
                code = getCode(nameClass, "", "", definition, statement);
            String fileName = nameClass + ".java";
            writeToFile(fileName, code);
            String errorCompile = compileCode(fileName, "compile");
            if(errorCompile != null){
                System.err.print(errorCompile);
                if(classNumber > 0)
                    code = getCode(nameClass, " extends ", superClass, "", "");
                else
                    code = getCode(nameClass, "", "", "", "");
                writeToFile(fileName, code);
            }
            else{
                exec(nameClass, classNumber);
            }
            //System.out.println("Code is: \n" + code);
            classNumber++;
        }
    }

    /*
        This method provides the class definition for each statement
     */
    public static String getCode(String nameClass, String extend, String superClass, String definition, String statement)
    {
        String javaCode = "import java.io.*;\n" + "import java.util.*;\n"+
                "public class " + nameClass + extend + superClass + "\n{\n" + definition + "\n" +
                "public static void exec() {\n" + statement + "\n" + "}\n" + "}\n";

        return javaCode;
    }

    /*
        This method compiles or parses the java code and returns the error message
        Initial lines (Lines 1 - 6) of code in this method is taken from: http://www.javabeat.net/the-java-6-0-compiler-api/
     */
    public static String compileCode(String fileName, String option) throws IOException{
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector diagnosticsCollector = new DiagnosticCollector();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticsCollector, null, null);
        String dirFile = tempDir + File.separator + fileName;
        Iterable fileObjects = fileManager.getJavaFileObjectsFromStrings(Arrays.asList(dirFile));
        String classPath = "." + System.getProperty("path.separator") + tempDir;
        Iterable<String> options = Arrays.asList("-d", tempDir, "-cp", classPath);
        JavacTask task = (JavacTask) compiler.getTask(null, fileManager, diagnosticsCollector, options, null, fileObjects);
        if(option.equals("parse"))
            task.parse();
        else
            task.call();
        StringBuilder sbuf = new StringBuilder(100);
        if(diagnosticsCollector.getDiagnostics().size() == 0)
            return null;
        List<Diagnostic> al = diagnosticsCollector.getDiagnostics();
        for(Diagnostic d: al){
            sbuf.append("line ").append(d.getLineNumber()).append(": ").append(d.getMessage(Locale.ENGLISH)).append("\n");
        }

        return sbuf.toString();
    }

    /*
        This method writes the class definition in a java file stored in the temporary directory
     */
    public static void writeToFile(String fileName, String code){
        String dirFile = tempDir + File.separator + fileName;
        try(BufferedWriter bufWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dirFile))))
        {
            bufWriter.write(code);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /*
        This method invokes the exec() method of each class file created
        Initial Lines of code (Lines 1 - 4) in this method were taken from http://tutorials.jenkov.com/java-reflection/dynamic-class-loading-reloading.html
     */
    public static void exec(String nameClass, int classNum) {
        try{
            if(classNum == 0){
                ClassLoader parentClassLoading = ClassLoader.getSystemClassLoader();
                URL url = new File(tempDir).toURI().toURL();
                classLoader = new URLClassLoader(new URL[] {url}, parentClassLoading);  //should be one only; instead of creating class loader everytime
            }
            Class actualClass = classLoader.loadClass(nameClass);
            Method decMeth = actualClass.getDeclaredMethod("exec", (Class[])null);
            decMeth.invoke(null, (Object[]) null);
        }catch(Exception e){
            e.printStackTrace(System.err);
        }
    }
}