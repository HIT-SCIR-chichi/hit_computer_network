package lab1;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class CommunicateThread extends Thread {

  Socket client_socket;// 与客户端通信的代理服务器的套接字
  Socket server_socket;// 与服务器端通信的代理服务器的套接字
  int port = 80;
  String type;
  String URL;
  String host;

  public CommunicateThread(Socket client_socket) {
    this.client_socket = client_socket;
  }

  @Override public void run() {
    try {
      BufferedReader bfReader =
          new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
      String proxy_in_line = bfReader.readLine();
      this.parse_request(proxy_in_line);
      // 网站过滤
      BufferedReader bfr_filter =
          new BufferedReader(new FileReader(HTTP_Proxy.filter_file));
      String line_filter = "";
      while ((line_filter = bfr_filter.readLine()) != null) {
        if (line_filter.contains(this.host)) {
          System.out.println("你不能访问该网站，因为目标网站被过滤:" + this.host);
          bfr_filter.close();
          return;
        }
      }
      bfr_filter.close();
      server_socket = new Socket(this.host, this.port);
      PrintWriter proxy_out = new PrintWriter(server_socket.getOutputStream());
      while (proxy_in_line != null) {
        try {
          System.out.println(proxy_in_line);
          client_socket.setSoTimeout(500);
          proxy_out.write(proxy_in_line + "\r\n");
          proxy_in_line = bfReader.readLine();
          client_socket.setSoTimeout(0);
        } catch (SocketTimeoutException e) {
          break;
        }
      }
      proxy_out.write("\r\n");
      proxy_out.flush();
      System.out
          .println("目的主机:" + this.host + "\n目的端口号:" + this.port + "\n服务类型:" + this.type);
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
        } catch (SocketException e) {
          System.out.println("\n" + e.getMessage() + "\n");
        }
      }
      System.out.println(this.host + ":客户端接收数据完毕");
      server_socket.close();
      client_socket.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * in order to:获取目的主机URL以及请求类型；初始化host和port(如果可以)
   */
  public void parse_request(String head_line) {
    this.type = head_line.split("[ ]")[0];
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
