/**
 * Created by Mayank on 1/28/2016.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Stack;

public class NestedReader {
    StringBuilder buf;
    BufferedReader input;
    char c;

    public NestedReader(BufferedReader input) {
        this.input = input;
    }

    /*
        This method checks for the valid character inputted from the user and appends it to the string
     */
    public String getNestedString() throws IOException {
        buf = new StringBuilder();
        Stack<Character> inputChars = new Stack<Character>();
        c = (char )input.read();
        while(true){
            if(c == (char)-1)
                return null;
            if(c == '{' || c == '(' || c == '[') {
                inputChars.push(c);
                buf.append(c);
            }
            else if (c == '/'){
                c = (char )input.read();
                if (c != '/')
                {
                    buf.append('/');
                    continue;
                }
                ignoreAllChars(c);
                if(inputChars.size() == 0)
                    return buf.toString();
            }
            else if (c == '}') {
                if (inputChars.size() == 0 || inputChars.lastElement() != '{') {
                    appendAllChars(c);
                    return buf.toString();
                }
                buf.append(c);
                inputChars.pop();
            }
            else if (c == ']') {
                if (inputChars.size() == 0 || inputChars.lastElement() != '[') {
                    appendAllChars(c);
                    return buf.toString();
                }
                buf.append(c);
                inputChars.pop();
            }
            else if (c == ')') {
                if (inputChars.size() == 0 || inputChars.lastElement() != '(') {
                    appendAllChars(c);
                    return buf.toString();
                }
                buf.append(c);
                inputChars.pop();
            }
            else if (c == '"'){
                if(inputChars.size() == 0) {
                    appendAllChars(c);
                    return buf.toString();
                }
                buf.append(c);
            }
            else if (c == '\n'){
                if(inputChars.size() == 0){
                    return buf.toString();
                }
                buf.append(c);
            }
            else{
                buf.append(c);
            }
            c = (char )input.read();
        }
    }

    /*
        This method handles the closing brackets and quotations which appends all the characters from now on to the string
     */
    public void appendAllChars(char c) throws IOException{
        while(c != '\n') {
            if(c == '/'){
                c = (char )input.read();
                if(c == '/'){
                    ignoreAllChars(c);
                    break;
                }
                buf.append('/');
            }
            buf.append(c);
            c = (char )input.read();
        }
    }

    /*
        This method ignores all the characters inputted from now on until a new line has been observed, mainly used for ignoring comments
     */
    public void ignoreAllChars(char c) throws IOException{
        while(c != '\n') {
            c = (char )input.read();
        }
    }
}