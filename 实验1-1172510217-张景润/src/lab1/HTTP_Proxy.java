package lab1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;

public class HTTP_Proxy {

  // 默认的完成过滤网页、过滤用户和网站引导的配置文件
  static File configuraion_file = new File("src/file/configuration_file.txt");
  static int socket_time_out = 1000;// socket的超时时间，设置为1000ms
  static boolean user_filter = true;// 是否开启用户过滤
  static boolean web_filter = true;// 是否开启网页过滤
  static boolean phishing = true;// 是否开启网站引导
  static ServerSocket server_socket;// 用于监听客户端请求的套接字
  static int server_port = 10240;// 默认的监听端口号
  static int cache_hit = 0;// 用于记录缓存命中的次数

  /**
   * to:启动代理服务器，开始处理用户端的HTTP请求.
   */
  public static void start_proxy() {
    try {
      server_socket = new ServerSocket(server_port);// 新建一个与客户端通信的套接字
      System.out.println("代理服务器:\t运行\n监听端口:\t" + server_port);
      if (user_filter) {
        System.out.println("用户过滤:\t开启");
      }
      if (web_filter) {
        System.out.println("网页过滤:\t开启");
      }
      if (phishing) {
        System.out.println("网站引导:\t开启");
      }
      System.out.println("服务器缓存:\t开启");
      System.out.println("***************************************************");
      while (true) {// 不断监听来自客户端的请求
        new CommunicateThread(server_socket.accept()).start();// 新建子线程处理连接请求
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  /**
   * to:读取配置文件，用于配置套接字超时时间、网页过滤、网站引导、用户过滤开否开启，默认缓存开启.
   * 
   * @param file_path 配置文件路径名
   */
  public static void configurate_proxy(String file_path) {
    try {
      if (file_path != null) {
        HTTP_Proxy.configuraion_file = new File(file_path);
      }
      BufferedReader configure_reader =
          new BufferedReader(new FileReader(HTTP_Proxy.configuraion_file));
      String configure_line = "";
      while ((configure_line = configure_reader.readLine()) != null) {
        if (configure_line.contains("socket_time_out=")) {
          HTTP_Proxy.socket_time_out = Integer.parseInt(configure_line.substring(16));
        } else if (configure_line.contains("web_filter=")) {
          HTTP_Proxy.web_filter = Boolean.valueOf(configure_line.substring(11));
        } else if (configure_line.contains("user_filter=")) {
          HTTP_Proxy.user_filter = Boolean.valueOf(configure_line.substring(12));
        } else if (configure_line.contains("phishing=")) {
          HTTP_Proxy.phishing = Boolean.valueOf(configure_line.substring(9));
        }
      }
      configure_reader.close();
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }

  }

  public static void main(String[] args) {
    HTTP_Proxy.configurate_proxy(null);// 配置附加功能，参数为null，表示配置文件为默认
    HTTP_Proxy.start_proxy();// 启动代理服务器
  }
}
