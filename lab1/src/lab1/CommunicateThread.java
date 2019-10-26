package lab1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class CommunicateThread extends Thread {

  Socket client_socket;// 与客户端通信的代理服务器的套接字
  Socket server_socket;// 与服务器端通信的代理服务器的套接字
  String request_gram = "";// 接收来自客户端的请求报文
  String respose_gram = "";// 接受来自服务器的响应报文
  byte[] respose_byte;
  int port = 80;
  String method;
  String URL;
  String host;

  public CommunicateThread(Socket client_socket) {
    this.client_socket = client_socket;
  }

  /**
   * in order to:用于网站过滤、用户过滤、网站引导
   */
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
      } else if (HTTP_Proxy.phishing && line_filter.contains(this.host + " ")
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

  public void in_cache_new() throws IOException {
    if (!new File("src/file/" + this.host).exists()) {
      new File("src/file/" + this.host).mkdir();
    }
    File cache_file =
        new File("src/file/" + this.host + "/" + this.URL.hashCode() + ".dat");
    if (!cache_file.exists()) {
      cache_file.createNewFile();
      // 向服务器转发原请求
      PrintWriter proxy_out = new PrintWriter(server_socket.getOutputStream());
      proxy_out.write(request_gram);
      proxy_out.flush();
      InputStream proxy_server_in = server_socket.getInputStream();
      OutputStream proxy_client_out = client_socket.getOutputStream();
      List<Byte> out_bytes = new ArrayList<>();
      while (true) {
        try {
          server_socket.setSoTimeout(500);
          int b = proxy_server_in.read();
          if (b == -1) {
            break;
          } else {
            out_bytes.add((byte) (b));
            proxy_client_out.write(b);
            server_socket.setSoTimeout(0);
          }
        } catch (SocketTimeoutException e) {
          break;
        }
      }
      this.respose_byte = new byte[out_bytes.size()];
      int count = 0;
      for (Byte byte1 : out_bytes) {
        this.respose_byte[count++] = byte1;
      }
      FileOutputStream cache_file_out = new FileOutputStream(cache_file);
      cache_file_out.write(this.respose_byte);
      cache_file_out.close();
    } else {// 文件已经存在，则需要判断：若存在DATA且已更新，则返回，否则直接将缓存作为响应
      BufferedReader cache_reader =
          new BufferedReader(new InputStreamReader(new FileInputStream(cache_file)));
      String line_cache = "";
      while ((line_cache = cache_reader.readLine()) != null) {
        this.respose_gram += line_cache + "\r\n";
        if (line_cache.startsWith("Date:")) {
          this.request_gram = this.request_gram.replace("\r\n\r\n",
              "\r\n" + "If-Modified-Since: " + line_cache.substring(6) + "\r\n\r\n");
        }
      }
      // 向服务器发送新的修改请求报文
      PrintWriter proxy_out = new PrintWriter(server_socket.getOutputStream());
      proxy_out.write(request_gram);
      proxy_out.flush();
      cache_reader.close();
      // 接收服务器的请求
      InputStream proxy_server_in = server_socket.getInputStream();
      List<Byte> out_bytes = new ArrayList<>();
      while (true) {
        try {
          server_socket.setSoTimeout(500);
          int b = proxy_server_in.read();
          if (b == -1) {
            break;
          } else {
            out_bytes.add((byte) (b));
            server_socket.setSoTimeout(0);
          }
        } catch (SocketTimeoutException e) {
          break;
        }
      }
      this.respose_byte = new byte[out_bytes.size()];
      int count = 0;
      for (Byte byte1 : out_bytes) {
        this.respose_byte[count++] = byte1;
      }
      this.respose_gram = new String(respose_byte, 0, count);
      if (this.respose_gram.split("\r\n")[0].contains("304")) {
        // System.out.println("服务器报文未更新，可以直接请求缓存\n该条请求报文在缓存中\n" + this.request_gram);
        System.out.println("缓存命中数: " + ++HTTP_Proxy.cache_hit + "\t命中: " + this.URL);
        // 直接将缓存报文发送给客户端
        FileInputStream cache_file_read = new FileInputStream(cache_file);
        OutputStream proxy_client_out = client_socket.getOutputStream();
        int b;
        while ((b = cache_file_read.read()) != -1) {
          proxy_client_out.write(b);
        }
        cache_file_read.close();
      } else if (this.respose_gram.split("\r\n")[0].contains("200")) {
        // 将从服务器读取到的转发给客户端，并更新缓存
        OutputStream proxy_client_out = client_socket.getOutputStream();
        proxy_client_out.write(this.respose_byte);
      }
    }
  }

  @Override public void run() {
    try {
      BufferedReader bfReader =
          new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
      String proxy_line = bfReader.readLine();
      this.parse_request(proxy_line);
      while (proxy_line != null) {
        try {
          if (!proxy_line.contains("Cache-Control")) {
            request_gram += proxy_line + "\r\n";
          }
          client_socket.setSoTimeout(500);
          proxy_line = bfReader.readLine();
          client_socket.setSoTimeout(0);
        } catch (SocketTimeoutException e) {
          break;
        }
      }
      if (!this.filter_and_phishing()) {// 网站过滤，用户过滤，钓鱼
        return;
      }
      server_socket = new Socket(this.host, this.port);
      this.in_cache_new();
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
