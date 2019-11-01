import threading


class Host:
    # 规定发送数据格式：[seq_num data]
    # 规定发送确认格式：[exp_num-1 0]
    # 规定发送结束格式：[0 0]
    host_address_1 = ('127.0.0.1', 12340)
    host_address_2 = ('127.0.0.1', 12341)

    # 用于配置主机地址
    @staticmethod
    def config(config_path='../file/config_file.txt'):
        with open(config_path, 'r', encoding='utf-8') as f:
            line = f.readline()
            while len(line) > 0:
                if line.startswith('host_address_1'):
                    Host.host_address_1 = (
                        line[line.index('=') + 1: line.index(' ')],
                        int(line[line.index(' ') + 1:len(line) - 1]))
                elif line.startswith('host_address_2'):
                    Host.host_address_2 = (
                        line[line.index('=') + 1: line.index(' ')],
                        int(line[line.index(' ') + 1:len(line) - 1]))
                line = f.readline()

    @staticmethod
    def make_pkt(pkt_num, data):
        return (str(pkt_num) + ' ' + str(data)).encode(encoding='utf-8')


def run_gbn():
    Host.config()
    from lab_code.gbn import GBN
    host_1 = GBN(Host.host_address_1, Host.host_address_2)
    host_2 = GBN(Host.host_address_2, Host.host_address_1)
    threading.Thread(target=host_1.server_run).start()  # 注意这里函数一定不能带括号
    threading.Thread(target=host_2.client_run).start()  # 注意这里函数一定不能带括号


def run_sr():
    Host.config()
    from lab_code.sr import SR
    host_1 = SR(Host.host_address_1, Host.host_address_2)
    host_2 = SR(Host.host_address_2, Host.host_address_1)
    threading.Thread(target=host_1.server_run).start()  # 注意这里函数一定不能带括号
    threading.Thread(target=host_2.client_run).start()  # 注意这里函数一定不能带括号


if __name__ == '__main__':
    run_gbn()
    # run_sr()
