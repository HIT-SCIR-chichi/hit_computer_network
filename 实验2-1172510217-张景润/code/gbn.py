import threading


class GBN:
    server_address = ('127.0.0.1', 12340)
    client_address = ('127.0.0.1', 12341)

    @staticmethod
    def make_pkt(pkt_num, data):
        return (str(pkt_num) + ' ' + str(data)).encode(encoding='utf-8')
