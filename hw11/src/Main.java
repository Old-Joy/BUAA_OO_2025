import com.oocourse.spec3.main.Runner;
import java.io.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // 重定向输入输出
        System.setIn(new FileInputStream("input.txt"));
        PrintStream ps = new PrintStream(new FileOutputStream("output.txt"));
        System.setOut(ps);
        
        // 保持原有Runner调用
        Runner runner = new Runner(
                Person.class,          // 您的 Person 实现类
                Network.class,         // 您的 Network 实现类
                Tag.class,             // 您的 Tag 实现类
                Message.class,         // 您的 Message 实现类
                EmojiMessage.class,    // 您的 EmojiMessage 实现类
                ForwardMessage.class,  // 您的 ForwardMessage 实现类
                RedEnvelopeMessage.class // 您的 RedEnvelopeMessage 实现类
        );
        runner.run();
        
        ps.close(); // 关闭输出流
    }
}