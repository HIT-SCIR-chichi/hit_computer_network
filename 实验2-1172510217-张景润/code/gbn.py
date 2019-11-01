import threading


class GBN:
    # 规定发送数据格式：[seq_num data]
    # 规定发送确认格式：[exp_num-1 0]
    # 规定发送结束格式：[0 0]
    host_address_1 = ('127.0.0.1', 12340)
    host_address_2 = ('127.0.0.1', 12341)

    @staticmethod
    def make_pkt(pkt_num, data):
        return (str(pkt_num) + ' ' + str(data)).encode(encoding='utf-8')
