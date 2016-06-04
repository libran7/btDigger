import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Noodle {
    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        System.out.print("[o] Initializing configuration...");
        //初始化配置文件
        ConfigLoader.getInstance();
        System.out.println("OK");
        //初始化目标主页并自动开始第一轮任务
        PageParser.getInstance();
    }
}
