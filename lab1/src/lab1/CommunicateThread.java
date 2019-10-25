package lab1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class CommunicateThread extends Thread {

  Socket client_socket;// 与客户端通信的代理服务器的套接字
  Socket server_socket;// 与服务器端通信的代理服务器的套接字
  String request_gram = "";// 接收来自客户端的请求报文
  int port = 80;
  String method;
  String URL;
  String host;

  public CommunicateThread(Socket client_socket) {
    this.client_socket = client_socket;
  }

  public boolean filter_and_phishing() throws IOException {
    if (!HTTP_Proxy.user_filter && !HTTP_Proxy.web_filter) {
      return true;
    }
    BufferedReader bfr_filter =
        new BufferedReader(new FileReader(HTTP_Proxy.filter_file));
    String line_filter = "";
    while ((line_filter = bfr_filter.readLine()) != null) {
      if (HTTP_Proxy.user_filter
          && line_filter.contains(client_socket.getInetAddress().getHostAddress())
          && line_filter.contains("user_filter")) {
        System.out.println(
            "你不能访问该网站，因为你是被限制用户:" + client_socket.getInetAddress().getHostAddress());
        bfr_filter.close();
        return false;
      } else if (HTTP_Proxy.web_filter && line_filter.contains(this.host)
          && line_filter.contains("web_filter")) {
        System.out.println("你不能访问该网站，因为目标网站被过滤:" + this.host);
        bfr_filter.close();
        return false;
      } else if (HTTP_Proxy.phishing && line_filter.contains(this.host)
          && line_filter.contains("phishing")) {
        this.host = line_filter.split(" ")[1];
        String old_URL = this.URL;
        this.URL = "http://" + this.host + "/";
        this.port = 80;
        request_gram = request_gram.replace(old_URL, this.URL);
        System.out.println("你不能访问目标网站，因为该网站已被引导向:" + this.host);
        bfr_filter.close();
        return true;
      }
    }
    bfr_filter.close();
    return true;
  }

  @Override public void run() {
    try {
      BufferedReader bfReader =
          new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
      String proxy_in_line = bfReader.readLine();
      this.parse_request(proxy_in_line);
      while (proxy_in_line != null) {
        try {
          request_gram += proxy_in_line + "\r\n";
          client_socket.setSoTimeout(500);
          proxy_in_line = bfReader.readLine();
          client_socket.setSoTimeout(0);
        } catch (SocketTimeoutException e) {
          break;
        }
      }
      if (!this.filter_and_phishing()) {// 网站过滤，用户过滤，钓鱼
        return;
      }
      server_socket = new Socket(this.host, this.port);
      PrintWriter proxy_out = new PrintWriter(server_socket.getOutputStream());
      System.out.print(request_gram);
      proxy_out.write(request_gram);
      proxy_out.flush();
      System.out.println(
          "目的主机:" + this.host + "\n目的端口号:" + this.port + "\n服务类型:" + this.method);
      System.out.println(this.host + ":向服务器转发数据结束");
      // 上面已经转发给服务器端，下面开始从服务器取数据并将其转发给客户端
      InputStream proxy_server_in = server_socket.getInputStream();
      OutputStream proxy_client_out = client_socket.getOutputStream();
      while (true) {
        try {
          server_socket.setSoTimeout(500);
          int b = proxy_server_in.read();
          if (b == -1) {
            break;
          } else {
            proxy_client_out.write(b);
            server_socket.setSoTimeout(0);
          }
        } catch (SocketTimeoutException e) {
          break;
        }
      }
      System.out.println(this.host + ":客户端接收数据完毕");
      server_socket.close();
      client_socket.close();
    } catch (IOException e) {
      System.out.println("\n" + e.getMessage() + "\n");
    }
  }

  /**
   * in order to:获取目的主机URL以及请求类型；初始化host和port(如果可以)
   */
  public void parse_request(String head_line) {
    this.method = head_line.split("[ ]")[0];
    this.URL = head_line.split("[ ]")[1];
    int index = -1;
    this.host = this.URL;
    if ((index = this.host.indexOf("http://")) != -1) {
      this.host = this.host.substring(index + 7);
    }
    if ((index = this.host.indexOf("/")) != -1) {
      this.host = this.host.substring(0, index);
    }
    if ((index = this.host.indexOf(":")) != -1) {
      this.port = Integer.valueOf(this.host.substring(index + 1));
      this.host = this.host.substring(0, index);
    }
  }

}
