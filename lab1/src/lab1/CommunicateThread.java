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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CommunicateThread extends Thread {

  Socket client_socket;// 与客户端通信的代理服务器的套接字
  Socket server_socket;// 与服务器端通信的代理服务器的套接字
  String request_gram = "";// 接收来自客户端的请求报文
  String respose_gram = "";// 接受来自服务器的响应报文
  int socket_time_out = 1000;// socket的超时时间，设置为1000ms
  byte[] respose_byte;// 用于储存接受的响应报文字节流信息
  int port = 80;// 默认与服务器的连接端口为80
  String URL;// 头部行中的URL
  String host;// 头部行中的host

  /**
   * to:设置client_socket与客户端建立连接通信.
   * 
   * @param client_socket 与客户端通信的socket
   */
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
        new BufferedReader(new FileReader(HTTP_Proxy.configuraion_file));
    String line_filter = "";
    while ((line_filter = bfr_filter.readLine()) != null) {
      if (HTTP_Proxy.user_filter// 若开启了用户过滤且客户端用户为被限制用户，则返回false
          && line_filter.contains(client_socket.getInetAddress().getHostAddress())
          && line_filter.contains("user_filter")) {
        System.out.println("用户受限:\t" + client_socket.getInetAddress().getHostAddress());
        bfr_filter.close();
        return false;
      } else if (HTTP_Proxy.web_filter && line_filter.contains(this.host)
          && line_filter.contains("web_filter")) {// 若开启了网站过滤且目的主机为被过滤的主机，则返回
        System.out.println("网站受限:\t" + this.host);
        bfr_filter.close();
        return false;
      } else if (HTTP_Proxy.phishing && line_filter.contains(this.host + " ")
          && line_filter.contains("phishing")) {// 若开启了网站引导且目的主机为被引导的主机则将头部行替换
        String old_host = this.host;
        this.host = line_filter.split(" ")[1];
        this.port = 80;
        this.URL = this.URL.replace(old_host, this.host);
        request_gram = request_gram.replace(old_host, this.host);// 将请求报文中的头部URL替换为引导网站的URL
        System.out.println("网站引导:\t" + this.host);
        bfr_filter.close();
        return true;
      }
    }
    bfr_filter.close();
    return true;
  }

  /**
   * to:判断是否有缓存文件.
   */
  public void cache() throws IOException {
    if (!new File("src/file/" + this.host).exists()) {
      new File("src/file/" + this.host).mkdir();
    }
    File cache_file =
        new File("src/file/" + this.host + "/" + this.URL.hashCode() + ".txt");
    PrintWriter proxy_out = new PrintWriter(server_socket.getOutputStream());// 向服务器发送的流
    InputStream proxy_server_in = server_socket.getInputStream();// 服务器向客户端返回响应的流
    OutputStream proxy_client_out = client_socket.getOutputStream();// 向客户端发送的流
    if (!cache_file.exists()) {// 若对应的缓存文件不存在，则创建该文件用于记录新返回的响应报文
      System.out.println("缓存文件不存在，需转发请求，文件名:" + this.URL.hashCode());
      FileOutputStream cache_file_out = new FileOutputStream(cache_file);// 写缓存文件的流
      proxy_out.write(request_gram); // 向服务器转发原请求
      proxy_out.flush();
      while (true) {
        try {
          server_socket.setSoTimeout(this.socket_time_out);// 设置超时时间用于跳出阻塞状态
          int b = proxy_server_in.read();// 字节流读取响应报文
          if (b == -1) {
            break;
          } else {
            cache_file_out.write(b);// 写入到缓存文件中
            proxy_client_out.write(b);// 写入到向客户端发送响应的流
            server_socket.setSoTimeout(0);
          }
        } catch (SocketTimeoutException e) {
          break;
        }
      }
      System.out.println("响应报文来源:服务器端\t新建缓存:是\t文件名:" + this.URL.hashCode() + ".txt");
      cache_file_out.close();
    } else {// 文件已经存在，则需要判断：若存在DATA且已更新，则返回，否则直接将缓存作为响应
      DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);
      df.setTimeZone(TimeZone.getTimeZone("GMT"));
      this.request_gram = this.request_gram.replace("\r\n\r\n",
          "\r\nIf-Modified-Since: " + df.format(cache_file.lastModified()) + "\r\n\r\n");
      proxy_out.write(request_gram);// 发送构造的新的请求报文，添加了文件的最后修改时间
      proxy_out.flush();
      // 接收服务器的请求
      List<Byte> out_bytes = new ArrayList<>();
      while (true) {
        try {
          server_socket.setSoTimeout(this.socket_time_out);// 设置超时时间用于跳出流阻塞
          int b = proxy_server_in.read();// 读取响应报文
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
      int count = 0;// 用于构造响应字节报文
      for (Byte byte1 : out_bytes) {
        this.respose_byte[count++] = byte1;
      }
      this.respose_gram = new String(respose_byte, 0, count);// 构造文本响应报文
      if (this.respose_gram.split("\r\n")[0].contains("304")) {// 响应报文头含304，则缓存可用
        System.out.println("缓存命中数: " + (++HTTP_Proxy.cache_hit) + "\t命中: " + this.URL
            + "\t文件名: " + this.URL.hashCode());
        FileInputStream cache_file_read = new FileInputStream(cache_file);
        int b;// 直接将缓存报文发送给客户端
        while ((b = cache_file_read.read()) != -1) {
          proxy_client_out.write(b);// 写入客户端的流
        }
        cache_file_read.close();
        System.out.println("响应报文来源:缓存文件\t更新缓存:否\t文件名:" + this.URL.hashCode() + ".txt");
      } else if (this.respose_gram.split("\r\n")[0].contains("200")) {// 响应报文头含200，更新缓存
        System.out.println("缓存文件存在，但需更新，文件名:" + this.URL.hashCode());
        FileOutputStream cache_file_out = new FileOutputStream(cache_file);// 写缓存文件的流
        proxy_client_out.write(this.respose_byte);// 将从服务器读取到的转发给客户端
        cache_file_out.write(this.respose_byte);// 更新本地缓存
        cache_file_out.close();
        System.out.println("响应报文来源:服务器端\t更新缓存:是\t文件名:" + this.URL.hashCode() + ".txt");
      }
    }
  }

  /**
   * to:多线程的一个线程的run函数，在这个函数里实现代理服务器的所有功能:转发、过滤、引导、缓存.
   */
  @Override public void run() {
    try {
      BufferedReader bfReader =
          new BufferedReader(new InputStreamReader(client_socket.getInputStream()));// 用于读取客户端发出的请求报文
      String proxy_line = bfReader.readLine();
      if (proxy_line == null) {
        return;
      }
      this.parse_request(proxy_line);// 解析头部行，并设置对象的属性值
      while (proxy_line != null) {
        try {
          request_gram += proxy_line + "\r\n";// 获取请求报文的信息
          client_socket.setSoTimeout(this.socket_time_out);// 设置超时时间，用于跳出流的阻塞状态
          proxy_line = bfReader.readLine();
          client_socket.setSoTimeout(0);
        } catch (SocketTimeoutException e) {
          break;
        }
      }
      if (!this.filter_and_phishing()) {// 网站过滤，用户过滤，钓鱼
        return;
      }
      server_socket = new Socket(this.host, this.port);// 建立与服务器通信的套接字
      this.cache();// 判断请求报文是否可以由缓存文件给出，并进行相应的操作
      server_socket.close();
      client_socket.close();
    } catch (IOException e) {
      System.out.println("\n" + e.getMessage());
    }
  }

  /**
   * in order to:获取目的主机URL以及请求类型；初始化host和port(如果可以)
   */
  public void parse_request(String head_line) {
    this.URL = head_line.split("[ ]")[1];// 获取请求的目的URL
    int index = -1;
    this.host = this.URL;// 下面用于获取请求的主机名
    if ((index = this.host.indexOf("http://")) != -1) {// 去掉URL中的http://
      this.host = this.host.substring(index + 7);
    }
    if ((index = this.host.indexOf("https://")) != -1) {// 去掉URL中的https://
      this.host = this.host.substring(index + 8);
    }
    if ((index = this.host.indexOf("/")) != -1) {// 去掉URL中的/
      this.host = this.host.substring(0, index);
    }
    if ((index = this.host.indexOf(":")) != -1) {// 去掉URL中的:
      this.port = Integer.valueOf(this.host.substring(index + 1));
      this.host = this.host.substring(0, index);
    }
  }

}
