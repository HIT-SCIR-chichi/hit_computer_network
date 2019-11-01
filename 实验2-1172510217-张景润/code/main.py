import threading

from code.gbn import GBN
from code.host import Host


def main():
    GBN.config_gbn()
    host_1 = Host(GBN.host_address_1, GBN.host_address_2)
    host_2 = Host(GBN.host_address_2, GBN.host_address_1)
    threading.Thread(target=host_1.server_run).start()  # 注意这里函数一定不能带括号
    threading.Thread(target=host_2.client_run).start()  # 注意这里函数一定不能带括号


if __name__ == '__main__':
    main()
