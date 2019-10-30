import socket


class Server:
    server_address = ('127.0.0.1', 100)

    def __init__(self):
        super().__init__()
        self.window_size = 5  # 窗口尺寸
        self.send_base = 0  # 最小的被发送的分组序号
        self.next_seq = 0  # 当前未被利用的序号
        self.time_count = 0  # 记录当前传输时间
        self.time_out = 10  # 设置超时时间
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.socket.bind(self.server_address)  # 绑定套接字的本地IP地址和端口号
        self.data = []  # 缓存发送数据
        self.read_path = '../file/read_file.txt'  # 需要发送的源文件数据
        self.buf_size = 1
        return

    def make_pkt(self, data):
        return (str(self.next_seq) + str(' ') + str(data)).encode()

    # 若仍剩余窗口空间，则构造数据报发送；否则拒绝发送数据
    def send_data(self):
        if self.next_seq < self.send_base + self.window_size:  # 窗口中仍有可用空间
            self.socket.sendto(self.make_pkt(self.data[self.next_seq]),
                               Client.client_address)
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
                               Client.client_address)
            print('数据已重发:' + str(self.send_base + i))

    # 处理收到的客户端ACK报文
    def rcv_pkt(self, data):
        rcv_num = int(data.decode().split()[0])
        self.send_base = (rcv_num + 1)  # 收到ACK，更新窗口起点
        self.time_count = 0  # 重置定时器

    def get_data_from_file(self):
        with open(self.read_path, 'r', encoding='utf-8') as f:
            f.read()
        return

    def run(self):
        while True:
            self.send_data()
            data = self.socket.recvfrom(self.buf_size)
            if len(data) > 0:
                break
            else:
                self.time_count += 1
                if self.time_count > self.time_out:
                    self.time_out()


class Client:
    client_address = ('127.0.0.1', 12300)

    def __init__(self):
        super().__init__()
        self.buf_size = 1026
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.socket.connect(Server.server_address)
        self.exp_seq = 0  # 当前期望收到该序号的数据
        self.save_path = '../file/config_file.txt'  # 保存数据的地址

    # 接收到服务器端发送的数据：若为期待的数据，保存到本地文件；否则，直接丢弃。发送期待的ACK
    def handle_rcv_data(self, data):
        [rcv_seq, rcv_data] = data.decode().split()[0]
        if rcv_seq == self.exp_seq:
            print('收到服务器发来的期望数据')
            self.write_data_to_file(rcv_data)  # 保存服务器端发送的数据到本地文件中
            self.exp_seq += 1  # 期望数据的序号更新
        else:
            print('服务器数据非期望数据')
        self.send_ack_pkt()

    def make_ack_pkt(self):
        return (str(self.exp_seq - 1) + str()).encode()

    def send_ack_pkt(self):
        self.socket.send(self.make_ack_pkt())

    # 保存来自服务器的合适的数据
    def write_data_to_file(self, data):
        with open(self.save_path, mode='a', encoding='utf-8') as f:
            f.write(data)
        return

    # 主要执行函数，不断接收服务器发送的数据，并返回相应的ACK报文
    def run(self):
        while True:
            data = self.socket.recvfrom(self.buf_size)
            self.handle_rcv_data(data)
