import struct
import sys

path = sys.argv[1]
data = bytearray(open(path, 'rb').read())
if data[:4] != b'\xca\xfe\xba\xbe':
    raise SystemExit('not a class file: ' + path)
cp_count = struct.unpack('>H', data[8:10])[0]
pos = 10
index = 1
while index < cp_count:
    tag = data[pos]
    pos += 1
    if tag in (7, 8, 16, 19, 20):
        pos += 2
    elif tag == 15:
        pos += 3
    elif tag == 1:
        length = struct.unpack('>H', data[pos:pos + 2])[0]
        pos += 2 + length
    elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
        pos += 4
    elif tag in (5, 6):
        pos += 8
        index += 1
    else:
        raise SystemExit('unknown constant tag %d' % tag)
    index += 1
flags = struct.unpack('>H', data[pos:pos + 2])[0]
data[pos:pos + 2] = struct.pack('>H', flags & ~0x0400)
open(path, 'wb').write(data)
