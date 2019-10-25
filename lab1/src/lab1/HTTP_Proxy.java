package lab1;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

public class HTTP_Proxy {

  static File filter_file = new File("src/file/filter_file.txt");
  ServerSocket serverSocket;
  int server_port;

  public HTTP_Proxy(int server_port) throws IOException {
    this.server_port = server_port;
    this.serverSocket = new ServerSocket(server_port);
  }

  public HTTP_Proxy(int server_port, String filter_path) throws IOException {
    this.server_port = server_port;
    this.serverSocket = new ServerSocket(server_port);
    filter_file = new File(filter_path);
  }

  public static void main(String[] args) throws IOException {
    try {
      HTTP_Proxy http_Proxy = new HTTP_Proxy(10240, "src/file/filter_file.txt");
      System.out.println("代理服务器正在运行，监听端口" + http_Proxy.server_port);
      while (true) {
        new CommunicateThread(http_Proxy.serverSocket.accept()).start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}