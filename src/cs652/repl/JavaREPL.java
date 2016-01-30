package cs652.repl;

import com.sun.source.util.JavacTask;
import javax.tools.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

class HandlingCompilerOperations {
    DiagnosticCollector dg;
    JavacTask javaTask;
    public HandlingCompilerOperations(DiagnosticCollector d, JavacTask jT){
        dg = d;
        javaTask = jT;
    }
}
public class JavaREPL {

    public static final String tempDir = System.getProperty("java.io.tmpdir");

    public static void main(String[] args) throws IOException {
        exec(new InputStreamReader(System.in));
    }

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
                String printStatement = "System.out.println(" + printStat.substring("print ".length()) + ");";
                readStr = printStatement;
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
            }
            else{
                exec(nameClass);
            }
            classNumber++;
        }
    }

    public static String getCode(String nameClass, String extend, String superClass, String definition, String statement)
    {
        String javaCode = "import java.io.*;\n" + "import java.util.*;\n"+
                "public class " + nameClass + extend + superClass + "{\n" + definition + "\n" +
                "public static void exec() {\n" + statement + "\n" + "}\n" + "}\n";

        return javaCode;
    }

    public static String errorMessage(HandlingCompilerOperations hc, String fileName) throws IOException {
        StringBuilder sbuf = new StringBuilder(100);
        if(hc.dg.getDiagnostics().size() == 0)
            return null;
        List<Diagnostic> al = hc.dg.getDiagnostics();
        for(Diagnostic d: al){
            sbuf.append("line ").append(d.getLineNumber()).append(": ").append(d.getMessage(Locale.ENGLISH)).append("\n");
        }

        return sbuf.toString();
    }

    public static String compileCode(String fileName, String option) throws IOException{
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector diagnosticsCollector = new DiagnosticCollector();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticsCollector, null, null);
        String dirFile = tempDir + File.separator + fileName;
        Iterable fileObjects = fileManager.getJavaFileObjectsFromStrings(Arrays.asList(dirFile));
        String classPath = "." + ";" + tempDir;
        Iterable<String> options = Arrays.asList("-d", tempDir, "-cp", classPath);
        JavacTask task = (JavacTask) compiler.getTask(null, fileManager, diagnosticsCollector, options, null, fileObjects);
        HandlingCompilerOperations hc = new HandlingCompilerOperations(diagnosticsCollector, task);
        if(option.equals("parse"))
            hc.javaTask.parse();
        else
            hc.javaTask.call();

        return errorMessage(hc, fileName);
    }

    public static void writeToFile(String fileName, String code){
        String dirFile = tempDir + File.separator + fileName;
        try(BufferedWriter bufWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dirFile))))
        {
            bufWriter.write(code);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void exec(String nameClass) {
        try{
            ClassLoader parentClassLoading = ClassLoader.getSystemClassLoader();
            URL url = new File(tempDir).toURI().toURL();
            ClassLoader classLoader = new URLClassLoader(new URL[] {url}, parentClassLoading);
            Class actualClass = classLoader.loadClass(nameClass);
            Method decMeth = actualClass.getDeclaredMethod("exec", (Class[])null);
            decMeth.invoke(null, (Object[]) null);
        }catch(Exception e){
            e.printStackTrace(System.err);
        }
    }
}