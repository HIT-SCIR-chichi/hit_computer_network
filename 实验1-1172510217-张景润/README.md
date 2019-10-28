# 实验1：代理服务器的设计与实现
运行环境
    运行软件：eclipse，2018-12
    项目信息：.classpath和.project
文件说明
    src文件：代码文件夹
        file文件夹
            configuration.txt
                功能：配置文件，用于配置文本代码的附加功能
                web_filter=true                             #用于配置网页过滤功能打开，若要关闭可改为false
                user_filter=true                            #用于配置用户过滤功能打开，若要关闭可改为false
                phishing=true                               #用于配置钓鱼功能打开，若要关闭可改为false
                web_filter:cs.hit.edu.cn                    #用于设置网页过滤对象为cs.hit.edu.cn，可修改
                user_filter:127.0.0.2                       #用于设置用户过滤对象为127.0.0.2，可修改
                phishing:jwes.hit.edu.cn today.hit.edu.cn   #用于设置钓鱼网站，由jwes引导向今日哈工大(必须中间有空格)
            其余.txt文件
                生成：实验时生成的代理服务器缓存文件
                文件组织方式：通常一个host对应的所有文本文件都在与host同名的文件夹下
        lab1文件夹：代码文件
            CommunicateThread.java
                一个线程类
                用于处理一个socket连接
            HTTP_Proxy.java
                可运行主类，用于启动代理服务器
                同时该类对外提供了API接口，可以通过静态顺序调用两个方法启动代理服务器：
                    HTTP_Proxy.configurate_proxy(null);//用于配置附加功能，参数为null则读取默认的配置文件
                    HTTP_Proxy.start_proxy();//用于启动代理服务器