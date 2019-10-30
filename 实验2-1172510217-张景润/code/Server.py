import os
import socket
import threading


class Server(threading.Thread):
    client_ip = '0.0.0.0'
    client_port = 10

    def __init__(self):
        super().__init__()
        self.window_size = 5  # 窗口尺寸
        self.send_base = 0  # 最小的被发送的分组序号
        self.next_seq = 0  # 当前未被利用的序号
        self.time_count = 0  # 设置超时时间
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.socket.bind(('127.0.0.1', 100))  # 绑定套接字的本地IP地址和端口号
        self.data = []  # 缓存发送数据
        return

    def make_pkt(self, data):
        return (str(self.next_seq) + str(data) + str(0)).encode()

    def send_data(self):
        if self.next_seq < self.send_base + self.window_size:  # 窗口中仍有可用空间
            self.socket.sendto(self.make_pkt(self.data[self.next_seq]),
                               (self.client_port, self.client_port))
            if self.send_base == self.next_seq:
                self.time_count = 0
            self.next_seq += 1
            print('成功发送数据')
            return True
        else:  # 窗口中无可用空间
            print('窗口已满，暂不发送数据')
            return False

    # 超时处理函数：计时器置0
    def time_out(self):
        print('超时，开始重传')
        self.time_count = 0
        for i in range(self.send_base,
                       self.next_seq if self.next_seq > self.send_base
                       else self.next_seq + self.window_size):
            self.socket.sendto(self.make_pkt(self.data[(self.send_base + i)]),
                               (self.client_port, self.client_port))
            print('数据已重发')

    # 处理收到的客户端ACK报文
    def rcv_pkt(self, data):
        rcv_num = int(data.decode().split()[0])
        self.send_base = (rcv_num + 1)  # 收到ACK，更新窗口起点
        self.time_count = 0  # 重置定时器


class Client(threading.Thread):
    def __init__(self):
        super().__init__()
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        # socket.create_connection(('127.0.0.1', 1000))
        self.exp_seq = 0  # 当前期望收到该序号的数据
        self.save_path = '../file/config_file.txt'

    # 接收到服务器端发送的数据：若为期待的数据，保存到本地文件；否则，直接丢弃。发送期待的ACK
    def rcv_data(self, data):
        [rcv_seq, rcv_data] = data.decode().split()[0]
        if rcv_seq == self.exp_seq:
            print('收到服务器发来的期望数据')
            self.save_data(rcv_data)  # 保存服务器端发送的数据到本地文件中
            self.exp_seq += 1  # 期望数据的序号更新
        else:
            print('服务器数据非期望数据')
        self.send_ack_pkt()

    def make_ack_pkt(self):
        return (str(self.exp_seq - 1) + str()).encode()

    def send_ack_pkt(self):
        self.socket.send(self.make_ack_pkt())

    def save_data(self, data):
        with open(self.save_path, mode='a', encoding='utf-8') as f:
            f.write(data)
        return


client = Client()
with open(client.save_path, mode='a', encoding='utf-8') as f:
    f.write("测试")
