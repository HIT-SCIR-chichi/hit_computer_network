package lab1;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;

public class HttpProxy {

  public static String cachePath = "";
  public static OutputStream writeCache;
  public static int TIMEOUT = 5000;// response time out upper bound
  public static int RETRIEVE = 5;// retry connection 5 times
  public static int CONNECT_PAUSE = 5000;// waiting for connection

  public static void main(String[] args) throws IOException {
    ServerSocket serverSocket;
    Socket currsoket = null;
    /** users need to setup work space */

    System.out
        .println("==============请输入缓存的存储目录，输入 d 则设置为默认目录（程序同一目录下）=================");
    Scanner scanner = new Scanner(System.in);
    cachePath = scanner.nextLine();
    if (cachePath.equals("d")) {
      cachePath = "defaul_cache.txt";
    }
    /* 初始化缓存写对象 */
    writeCache = new FileOutputStream(cachePath, true);
    System.out.println(
        "=================================== 工作目录设置完毕====================================");

    try {
      serverSocket = new ServerSocket(8888);
      int i = 0;
      // 循环，持续监听从这个端口的所有请求
      while (true) {
        currsoket = serverSocket.accept();
        // 启动一个新的线程来处理这个请求
        i++;
        System.out.println("启动第" + i + "个线程");
      }
    } catch (IOException e) {
      if (currsoket != null) {
        currsoket.close();// 及时关闭这个socket
      }
      e.printStackTrace();
    }
    writeCache.close();// 关闭文件输出流
    scanner.close();
  }

  public class MyProxy extends Thread {

    Socket socket;// 这个socket是这个线程与浏览器的socket

    String targetHost = null;
    String targetPort;
    InputStream inputStream_client;// 这个输入流用来读取浏览器发过来的请求
    OutputStream outputStream_client;// 这个输出流用来将数据发送到浏览器
    PrintWriter outPrintWriter_client;// 这个writer用来向浏览器写入数据
    BufferedReader bufferedReader_client;// 这个缓冲用来缓存浏览器的请求

    Socket accessSocket;// 这个socket用来向网站连接

    InputStream inputStream_Web;// 这个输入流用来读取从网站发回的响应
    OutputStream outputStream_Web;// 这个输出流用来向网站发送请求
    PrintWriter outPrintWriter_Web;// 这个writer用来向网站发送请求
    BufferedReader bufferedReader_web;// 这个缓冲用来缓存想网站发送的请求

    String cacheFilePath;
    File file = null;
    FileInputStream fileInputStream;
    String url = "";
    ArrayList<String> cache;
    int cache_url_index = -1;
    boolean has_cache_no_timestamp = false;

    public MyProxy(Socket inputSocket) throws IOException {
      socket = inputSocket;
      /** 创建一个文件对象 */
      file = new File(HttpProxy.cachePath);
      if (!file.exists()) {// 文件不存在则新建一个文件
        file.createNewFile();
      }

      fileInputStream = new FileInputStream(HttpProxy.cachePath);

      System.out.print("代理服务器启动\n");
      System.out.print("获取的socket来自" + inputSocket.getInetAddress() + ":"
          + inputSocket.getPort() + "\n");

      inputStream_client = socket.getInputStream();// 创建从浏览器获取请求的输入流
      bufferedReader_client =
          new BufferedReader(new InputStreamReader(inputStream_client));
      outputStream_client = socket.getOutputStream();// 创建向浏览器发送响应的流
      outPrintWriter_client = new PrintWriter(outputStream_client);
      /** 读取缓存 */
      cache = readCache(fileInputStream);
      System.out.println("读到的缓存有" + cache.size() + "行");

      start();// 启动本线程
    }

    @Override public void run() {
      try {
        socket.setSoTimeout(HttpProxy.TIMEOUT);// 设置最大等待时间，超过则自动断开连接
        String buffer;
        // debug
        System.out.println("从浏览器读取第一行....");
        buffer = bufferedReader_client.readLine();// 从浏览器读取第一行请求
        System.out.println(buffer);

        /** 提取 URL */
        url = getURL(buffer);

        /** 将请求写入缓存文件,如果缓存中已经有相同的请求，就不再写入了 */
        boolean has_in_cache_already = false;
        for (String iter : cache) {
          if (iter.equals(buffer)) {
            has_in_cache_already = true;
            break;
          }
        }
        if (has_in_cache_already == false) {
          String temp = buffer + "\r\n";
          write_cache(temp.getBytes(), 0, temp.length());
        }

        /** 提取主机和端口 */
        String[] HostandPort = new String[2];
        if (buffer != null)
          HostandPort = findHostandPort(buffer);
        targetHost = HostandPort[0];
        targetPort = HostandPort[1];

        System.out.println("提取的主机名:" + targetHost + " 提取的端口号: " + targetPort);

        /** 尝试与目标主机连接 */
        int retry = HttpProxy.RETRIEVE;
        while (retry-- != 0 && (targetHost != null)) {
          try {
            accessSocket = new Socket(targetHost, Integer.parseInt(targetPort));
            break;
          } catch (Exception e) {
            e.printStackTrace();
          }
          Thread.sleep(HttpProxy.CONNECT_PAUSE);// 等待
        }
        if (accessSocket != null) {// 成功建立连接
          // debug
          System.out.println("请求将发送至:" + targetHost);
          accessSocket.setSoTimeout(HttpProxy.TIMEOUT);
          inputStream_Web = accessSocket.getInputStream();// 获取网站返回的响应
          bufferedReader_web = new BufferedReader(new InputStreamReader(inputStream_Web));
          outPrintWriter_Web = new PrintWriter(accessSocket.getOutputStream());// 准备好向网站发送请求

          /** 如果缓存文件为空 */
          if (cache.size() == 0) {
            /** 将请求直接发往网站，并获取响应，记录响应至缓存 */
            sendRequestToInternet(buffer);
            transmitResponseToClient();
          } else {// 缓存文件不为空，寻找之前有没有缓存过该请求
            String modifyTime;
            String info = "";
            modifyTime = findModifyTime(cache, buffer);// 提取modifytime
            System.out.println("提取到的modifytime：" + modifyTime);
            if (modifyTime != null || has_cache_no_timestamp) {
              /** 如果缓存的内容里面该请求是没有Last-Modify属性的，就不用向服务器查询If-Modify了，否则向服务器查询If-Modify */
              if (!has_cache_no_timestamp) {
                buffer += "\r\n";
                outPrintWriter_Web.write(buffer);
                System.out.print("向服务器发送确认修改时间请求:\n" + buffer);
                String str1 = "Host: " + targetHost + "\r\n";
                outPrintWriter_Web.write(str1);
                String str = "If-modified-since: " + modifyTime + "\r\n";
                outPrintWriter_Web.write(str);
                outPrintWriter_Web.write("\r\n");
                outPrintWriter_Web.flush();
                System.out.print(str1);
                System.out.print(str);

                info = bufferedReader_web.readLine();
                System.out.println("服务器发回的信息是：" + info);
              }

              if (info.contains("Not Modified") || has_cache_no_timestamp) {// 如果服务器给回的响应是304
                                                                            // Not
                                                                            // Modified，就将缓存的数据直接发送给浏览器
                String temp_response = "";
                System.out.println("使用缓存数据");
                if (cache_url_index != -1)
                  for (int i = cache_url_index + 1; i < cache.size(); i++) {
                    if (cache.get(i).contains("http://"))
                      break;
                    temp_response += cache.get(i);
                    temp_response += "\r\n";

                  }
                System.out.println("使用缓存：\n" + temp_response);
                outputStream_client.write(temp_response.getBytes(), 0,
                    temp_response.getBytes().length);
                outputStream_client.write("\r\n".getBytes(), 0, "\r\n".getBytes().length);
                outputStream_client.flush();
              } else {
                /** 服务器返回的不是304 Not Modified的话，就将服务器的响应直接转发到浏览器并记录缓存就好了 */
                System.out.println("有更新，使用新的数据");
                transmitResponseToClient();
              }
            } else {
              /** 缓存中没有找到之前的记录，直接将请求发送给网站，并接收响应，将响应写入缓存 */
              sendRequestToInternet(buffer);
              transmitResponseToClient();
            }

          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    /**
     * 将请求发送给网站
     * 
     * @param  buffer      请求的第一行报文
     * @throws IOException
     */
    private void sendRequestToInternet(String buffer) throws IOException {
      while (!buffer.equals("")) {
        buffer += "\r\n";
        outPrintWriter_Web.write(buffer);
        System.out.print("发送请求:" + buffer + "\n");
        buffer = bufferedReader_client.readLine();
      }
      outPrintWriter_Web.write("\r\n");
      outPrintWriter_Web.flush();
    }

    /**
     * 提取主机名和端口
     * 
     * @param  content 待提取的报文，这是请求的第一行
     * @return
     */
    private String[] findHostandPort(String content) {
      String host = null;
      String port = null;
      String[] result = new String[2];
      int index;
      int portIndex;
      String temp;

      StringTokenizer stringTokenizer = new StringTokenizer(content);
      stringTokenizer.nextToken();// 丢弃第一个字串 这是请求类型 比如GET POST
      temp = stringTokenizer.nextToken();// 这个字串里面有主机名和端口

      host = temp.substring(temp.indexOf("//") + 2);// 比如
                                                    // http://news.sina.com.cn/gov/2017-12-13/doc-ifypsqiz3904275.shtml
                                                    // ->
                                                    // news.sina.com.cn/gov/2017-12-13/doc-ifypsqiz3904275.shtml
      index = host.indexOf("/");
      if (index != -1) {
        host = host.substring(0, index);// 比如
                                        // news.sina.com.cn/gov/2017-12-13/doc-ifypsqiz3904275.shtml
                                        // -> news.sina.com.cn
        portIndex = host.indexOf(":");
        if (portIndex != -1) {
          port = host.substring(portIndex + 1);// 比如 www.ghostlwb.com:8080 -> 8080
          host = host.substring(0, portIndex);
        } else {// 没有找到端口号，则加上默认端口号80
          port = "80";
        }
      }
      result[0] = host;
      result[1] = port;
      return result;
    }

    /**
     * 提取URL
     * 
     * @param  firstline 请求报文的第一行
     * @return
     */
    private String getURL(String firstline) {
      StringTokenizer stringTokenizer = new StringTokenizer(firstline);
      stringTokenizer.nextToken();
      return stringTokenizer.nextToken();
    }

    /**
     * 这个函数做三件事：从网站接收响应，发送给浏览器，并将响应写入缓存
     * 
     * @throws IOException
     */
    private void transmitResponseToClient() throws IOException {

      byte[] bytes = new byte[2048];
      int length = 0;

      while (true) {
        if ((length = inputStream_Web.read(bytes)) > 0) {
          outputStream_client.write(bytes, 0, length);
          String show_response = new String(bytes, 0, bytes.length);
          System.out.println("服务器发回的消息是:\n---\n" + show_response + "\n---");
          write_cache(bytes, 0, length);
          write_cache("\r\n".getBytes(), 0, 2);
          continue;
        }
        break;
      }

      outPrintWriter_client.write("\r\n");
      outPrintWriter_client.flush();
    }

    /**
     * 从文件中读取缓存内容，按行读取
     * 
     * @param  fileInputStream1
     * @return
     */
    private ArrayList<String> readCache(FileInputStream fileInputStream1) {
      ArrayList<String> result = new ArrayList<>();
      String temp;
      BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream1));
      try {
        while ((temp = br.readLine()) != null) {
          result.add(temp);
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
      return result;
    }

    /**
     * 将内容写入缓存，这两段代码参考网上的
     * 
     * @param  c
     * @throws IOException
     */
    private void write_cache(int c) throws IOException {
      HttpProxy.writeCache.write((char) c);
    }

    private void write_cache(byte[] bytes, int offset, int len) throws IOException {
      for (int i = 0; i < len; i++)
        write_cache((int) bytes[offset + i]);
    }

    /**
     * 提取modifytime
     * 
     * @param  cache_temp
     * @param  request
     * @return
     */
    private String findModifyTime(ArrayList<String> cache_temp, String request) {
      String LastModifiTime = null;
      int startSearching = 0;
      has_cache_no_timestamp = false;

      System.out.println("将要比对的URL是" + request);
      for (int i = 0; i < cache_temp.size(); i++) {

        if (cache_temp.get(i).equals(request)) {
          startSearching = i;
          cache_url_index = i;
          for (int j = startSearching + 1; j < cache_temp.size(); j++) {
            if (cache_temp.get(j).contains("http://"))
              break;
            if (cache_temp.get(j).contains("Last-Modified:")) {
              LastModifiTime =
                  cacheFilePath.substring(cache_temp.get(j).indexOf("Last-Modified:"));
              return LastModifiTime;
            }
            if (cache_temp.get(j).contains("<html>")) {
              has_cache_no_timestamp = true;
              return LastModifiTime;
            }
          }
        }
      }

      return LastModifiTime;
    }

  }

}
