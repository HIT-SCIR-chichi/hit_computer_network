package lab1;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

public class HTTP_Proxy {

  static File filter_file = new File("src/file/filter_file.txt");
  static boolean user_filter = true;
  static boolean web_filter = true;
  static boolean phishing = true;
  static int cache_hit = 0;
  ServerSocket serverSocket;
  int server_port = 10240;

  public HTTP_Proxy() throws IOException {
    this.serverSocket = new ServerSocket(server_port);
  }

  public static void main(String[] args) throws IOException {
    try {
      HTTP_Proxy http_Proxy = new HTTP_Proxy();
      System.out.println("代理服务器:\t运行\n监听端口:\t" + http_Proxy.server_port);
      if (user_filter) {
        System.out.println("用户过滤:\t打开");
      }
      if (web_filter) {
        System.out.println("网页过滤:\t打开");
      }
      System.out.println("网页缓存:\t打开");
      System.out.println("**********************************************************");
      while (true) {
        new CommunicateThread(http_Proxy.serverSocket.accept()).start();
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }
}
