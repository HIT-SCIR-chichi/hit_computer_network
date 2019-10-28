# 实验1：代理服务器的设计与实现
## 程序运行时可能出现问题说明
- 由于该说明文件同时用于我的私人GitHub仓库的README文件1，所以使用Markdown语言编写(配合在线Markdown编辑器食用本文件更佳)
- 可能出现网页加载不出来的情况
    - 可能是超时时间间隔设置过短，可在配置文件里增大超时时间
    - 可能是缓存文件的问题，可删除缓存文件，并重新访问(目录：src/file/host/URL.hashCode().txt)
- 可能出现console输出错误的问题
    - 可能是因为电脑代理服务器设置不完全，可以在代理服务器设置的过滤选项框内输入(https://)
## 运行环境
    运行软件：eclipse，2018-12  
    项目信息：.classpath和.project  
## 文件说明
### src文件
#### file文件夹
- configuration.txt
    - 功能：配置文件，用于配置文本代码的附加功能  
    - web_filter=true                             #用于配置网页过滤功能打开，若要关闭可改为false  
    - user_filter=true                            #用于配置用户过滤功能打开，若要关闭可改为false  
    - phishing=true                               #用于配置钓鱼功能打开，若要关闭可改为false  
    - web_filter:cs.hit.edu.cn                    #用于设置网页过滤对象为cs.hit.edu.cn，可修改  
    - user_filter:127.0.0.2                       #用于设置用户过滤对象为127.0.0.2，可修改  
    - phishing:jwes.hit.edu.cn today.hit.edu.cn   #用于设置钓鱼网站，由jwes引导向今日哈工大(必须中间有空格)  
- 其余.txt文件
    - 生成：实验时生成的代理服务器缓存文件  
    - 文件组织方式：通常一个host对应的所有文本文件都在与host同名的文件夹下  
#### lab1文件夹
- CommunicateThread.java
    - 一个线程类  
    - 用于处理一个socket连接  
- HTTP_Proxy.java
    - 可运行主类，用于启动代理服务器
    - 同时该类对外提供了API接口，可以通过静态顺序调用两个方法启动代理服务器
        - HTTP_Proxy.configurate_proxy(null);//用于配置附加功能，参数为null则读取默认的配置文件  
        - HTTP_Proxy.start_proxy();//用于启动代理服务器  