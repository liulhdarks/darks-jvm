import struct
import sys


def u1(value):
    return struct.pack('>B', value)


def u2(value):
    return struct.pack('>H', value)


def u4(value):
    return struct.pack('>I', value)


def utf8(text):
    raw = text.encode('utf-8')
    return u1(1) + u2(len(raw)) + raw


def build():
    code = bytearray()
    code += u1(0x04)                 # iconst_1
    code += u1(0x36) + u1(0x00)      # istore 0
    code += u1(0xa8) + u2(5)         # jsr +5 -> subroutine at pc 8
    code += u1(0x1a)                 # iload_0
    code += u1(0xac)                 # ireturn
    code += u1(0xc4) + u1(0x3a) + u2(260)  # wide astore 260
    code += u1(0x05)                 # iconst_2
    code += u1(0x36) + u1(0x00)      # istore 0
    code += u1(0xc4) + u1(0xa9) + u2(260)  # wide ret 260
    code = bytes(code)

    cp = []
    cp.append(utf8('WideRet'))                        # 1
    cp.append(u1(7) + u2(1))                          # 2 Class
    cp.append(utf8('java/lang/Object'))               # 3
    cp.append(u1(7) + u2(3))                          # 4 Class
    cp.append(utf8('run'))                            # 5
    cp.append(utf8('()I'))                            # 6
    cp.append(utf8('Code'))                           # 7
    body = u2(1) + u2(261) + u4(len(code)) + code + u2(0) + u2(0)
    method = u2(0x0009) + u2(5) + u2(6) + u2(1) + u2(7) + u4(len(body)) + body
    out = bytearray()
    out += b'\xca\xfe\xba\xbe' + u2(0) + u2(52)
    out += u2(len(cp) + 1)
    for entry in cp:
        out += entry
    out += u2(0x0021)   # ACC_PUBLIC | ACC_SUPER
    out += u2(2) + u2(4) + u2(0) + u2(0) + u2(1)
    out += method
    out += u2(0)
    return bytes(out)


if __name__ == '__main__':
    with open(sys.argv[1], 'wb') as handle:
        handle.write(build())
