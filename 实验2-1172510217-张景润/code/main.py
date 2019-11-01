import threading

from code.client import Client
from code.server import Server


def main():
    server = Server()
    client = Client()
    threading.Thread(target=server.run).start()  # 注意这里函数一定不能带括号
    threading.Thread(target=client.run).start()  # 注意这里函数一定不能带括号


if __name__ == '__main__':
    main()
