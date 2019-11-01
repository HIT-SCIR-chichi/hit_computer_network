class GBN:
    # 规定发送数据格式：[seq_num data]
    # 规定发送确认格式：[exp_num-1 0]
    # 规定发送结束格式：[0 0]
    host_address_1 = ('127.0.0.1', 12340)
    host_address_2 = ('127.0.0.1', 12341)

    # 用于配置主机地址
    @staticmethod
    def config_gbn(config_path='../file/config_file.txt'):
        with open(config_path, 'r', encoding='utf-8') as f:
            line = f.readline()
            while len(line) > 0:
                if line.startswith('host_address_1'):
                    GBN.host_address_1 = (
                        line[line.index('=') + 1: line.index(' ')],
                        int(line[line.index(' ') + 1:len(line) - 1]))
                    print(GBN.host_address_1)
                elif line.startswith('host_address_2'):
                    GBN.host_address_2 = (
                        line[line.index('=') + 1: line.index(' ')],
                        int(line[line.index(' ') + 1:len(line) - 1]))
                line = f.readline()

    @staticmethod
    def make_pkt(pkt_num, data):
        return (str(pkt_num) + ' ' + str(data)).encode(encoding='utf-8')
