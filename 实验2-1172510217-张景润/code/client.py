import random
import select
import socket

from code.gbn import GBN


class Client:

    def __init__(self):
        super().__init__()
        self.buf_size = 1678
        self.time_out = 10  # 超时时间
        self.time_count = 0  # 用于超时次数计数
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, 0)
        self.socket.bind(GBN.client_address)
        self.exp_seq = 0  # 当前期望收到该序号的数据
        self.save_path = '../file/config_file.txt'  # 保存数据的地址
        self.write_data_to_file('', mode='w')

        self.ack_loss = 0.05  # 返回的ack丢包率

    def make_ack_pkt(self):
        return str(self.exp_seq - 1).encode()

    # 保存来自服务器的合适的数据
    def write_data_to_file(self, data, mode='a'):
        with open(self.save_path, mode='a', encoding='utf-8') as f:
            f.write(data)

    # 主要执行函数，不断接收服务器发送的数据，若为期待序号的数据，则保存到本地，否则直接丢弃；并返回相应的ACK报文
    def run(self):
        while True:
            readable = select.select([self.socket], [], [], 1)[0]
            if len(readable) > 0:
                rcv_data = self.socket.recvfrom(self.buf_size)[0].decode()
                rcv_seq = rcv_data.split()[0]
                rcv_data = rcv_data.replace(rcv_seq + ' ', '')
                if rcv_seq is '0' and rcv_data is '':
                    print('客户端:传输数据结束')
                    break
                if int(rcv_seq) == self.exp_seq:
                    print('收到服务器发来的期望序号数据:' + str(rcv_seq))
                    self.write_data_to_file(rcv_data)  # 保存服务器端发送的数据到本地文件中
                    self.exp_seq += 1  # 期望数据的序号更新
                else:
                    print('客户端：收到数据非期望数据，期望:' + str(self.exp_seq) + '实际:' + str(rcv_seq))
                if random.random() >= self.ack_loss:
                    self.socket.sendto(self.make_ack_pkt(), GBN.server_address)
