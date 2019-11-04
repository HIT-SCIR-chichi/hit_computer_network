import threading


class Host:
    # 规定发送数据格式：[seq_num data]
    # 规定发送确认格式：[exp_num-1 0]
    # 规定发送结束格式：[0 0]
    host_address_1 = ('127.0.0.1', 12340)  # 主机1的默认地址
    host_address_2 = ('127.0.0.1', 12341)  # 主机2的默认地址

    # 用于配置主机地址
    @staticmethod
    def config(config_path='../file/config_file.txt'):
        with open(config_path, 'r', encoding='utf-8') as f:  # 读取配置文件
            line = f.readline()
            while len(line) > 0:
                if line.startswith('host_address_1'):  # 配置主机1的地址
                    Host.host_address_1 = (line[line.index('=') + 1: line.index(' ')],
                                           int(line[line.index(' ') + 1:len(line) - 1]))
                elif line.startswith('host_address_2'):  # 配置主机2的地址
                    Host.host_address_2 = (line[line.index('=') + 1: line.index(' ')],
                                           int(line[line.index(' ') + 1:len(line) - 1]))
                line = f.readline()

    # 产生一个发送数据包或者确认数据包，数据包格式遵循本文件的规约
    @staticmethod
    def make_pkt(pkt_num, data):
        return (str(pkt_num) + ' ' + str(data)).encode(encoding='utf-8')


# 通过该方法运行GBN协议
def run_gbn():
    from lab_code.gbn import GBN
    host_1 = GBN(Host.host_address_1, Host.host_address_2)
    host_2 = GBN(Host.host_address_2, Host.host_address_1)
    threading.Thread(target=host_1.server_run).start()  # 注意这里函数一定不能带括号
    threading.Thread(target=host_2.client_run).start()  # 注意这里函数一定不能带括号


# 通过该方法运行SR协议
def run_sr():
    from lab_code.sr import SR
    host_1 = SR(Host.host_address_1, Host.host_address_2)
    host_2 = SR(Host.host_address_2, Host.host_address_1)
    threading.Thread(target=host_1.server_run).start()  # 注意这里函数一定不能带括号
    threading.Thread(target=host_2.client_run).start()  # 注意这里函数一定不能带括号


# 可运行代码块，用户输入协议进行选择，并运行相应的协议程序
if __name__ == '__main__':
    Host.config()  # 配置要运行的发送方和接收方信息
    choice = input('请选择要运行的协议:输入‘GBN’表示运行GBN，‘SR’表示运行SR协议')
    if choice == 'GBN':
        run_gbn()
    elif choice == 'SR':
        run_sr()
    else:
        print('输入非法字串，请重新运行程序')
