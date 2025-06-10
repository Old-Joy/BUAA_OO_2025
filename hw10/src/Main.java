import com.oocourse.spec2.main.Runner;
import java.io.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // 重定向输入输出
        System.setIn(new FileInputStream("input.txt"));
        PrintStream ps = new PrintStream(new FileOutputStream("output.txt"));
        System.setOut(ps);
        
        // 保持原有Runner调用
        Runner runner = new Runner(Person.class, Network.class, Tag.class);
        runner.run();
        
        ps.close(); // 关闭输出流
    }
}