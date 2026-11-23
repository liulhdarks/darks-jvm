package java.security;

/**
 * Message digest API backed by built-in SHA-256 and SHA-1 implementations.
 * The runtime has no security provider framework, so getInstance resolves the
 * common algorithms directly.
 */
public abstract class MessageDigest
{
    private final String algorithm;

    protected MessageDigest(String algorithm)
    {
        if (algorithm == null)
        {
            throw new NullPointerException("algorithm");
        }
        this.algorithm = algorithm;
    }

    public static MessageDigest getInstance(String algorithm) throws NoSuchAlgorithmException
    {
        if (algorithm == null)
        {
            throw new NullPointerException("algorithm");
        }
        String name = algorithm.toUpperCase();
        if (name.equals("MD5"))
        {
            return new MD5();
        }
        if (name.equals("SHA-256") || name.equals("SHA256"))
        {
            return new SHA256();
        }
        if (name.equals("SHA-1") || name.equals("SHA1") || name.equals("SHA"))
        {
            return new SHA1();
        }
        throw new NoSuchAlgorithmException(algorithm);
    }

    public final String getAlgorithm()
    {
        return algorithm;
    }

    public final int getDigestLength()
    {
        return engineGetDigestLength();
    }

    public void update(byte input)
    {
        engineUpdate(input);
    }

    public void update(byte[] input, int offset, int length)
    {
        if (input == null)
        {
            throw new NullPointerException("input");
        }
        if (offset < 0 || length < 0 || offset + length > input.length)
        {
            throw new ArrayIndexOutOfBoundsException();
        }
        engineUpdate(input, offset, length);
    }

    public void update(byte[] input)
    {
        update(input, 0, input.length);
    }

    public byte[] digest()
    {
        return engineDigest();
    }

    public byte[] digest(byte[] input)
    {
        update(input);
        return digest();
    }

    public int digest(byte[] buffer, int offset, int length) throws DigestException
    {
        byte[] result = engineDigest();
        if (length < result.length)
        {
            throw new DigestException("insufficient space");
        }
        System.arraycopy(result, 0, buffer, offset, result.length);
        return result.length;
    }

    public void reset()
    {
        engineReset();
    }

    public Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    protected abstract void engineUpdate(byte input);

    protected abstract void engineUpdate(byte[] input, int offset, int length);

    protected abstract byte[] engineDigest();

    protected abstract void engineReset();

    protected int engineGetDigestLength()
    {
        return 0;
    }

    private static final int[] SHA256_K = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
        0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
        0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
        0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    private static final class MD5 extends MessageDigest
    {
        private static final int[] K = {
            0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee,
            0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
            0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be,
            0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
            0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa,
            0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
            0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed,
            0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
            0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c,
            0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
            0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05,
            0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
            0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039,
            0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
            0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1,
            0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391
        };

        private static final int[] S = {
            7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
            5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
            4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
            6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
        };

        private final int[] state = new int[4];
        private final byte[] block = new byte[64];
        private int blockLength;
        private long totalLength;

        MD5()
        {
            super("MD5");
            engineReset();
        }

        protected void engineUpdate(byte input)
        {
            block[blockLength] = input;
            blockLength++;
            totalLength++;
            if (blockLength == 64)
            {
                transform();
                blockLength = 0;
            }
        }

        protected void engineUpdate(byte[] input, int offset, int length)
        {
            for (int i = 0; i < length; i++)
            {
                engineUpdate(input[offset + i]);
            }
        }

        protected byte[] engineDigest()
        {
            long bitLength = totalLength * 8L;
            engineUpdate((byte)0x80);
            while (blockLength != 56)
            {
                engineUpdate((byte)0);
            }
            for (int i = 0; i < 8; i++)
            {
                engineUpdate((byte)(bitLength >>> (i * 8)));
            }
            byte[] result = new byte[16];
            for (int i = 0; i < 4; i++)
            {
                result[i * 4] = (byte)state[i];
                result[i * 4 + 1] = (byte)(state[i] >>> 8);
                result[i * 4 + 2] = (byte)(state[i] >>> 16);
                result[i * 4 + 3] = (byte)(state[i] >>> 24);
            }
            engineReset();
            return result;
        }

        protected void engineReset()
        {
            state[0] = 0x67452301;
            state[1] = 0xefcdab89;
            state[2] = 0x98badcfe;
            state[3] = 0x10325476;
            blockLength = 0;
            totalLength = 0L;
        }

        protected int engineGetDigestLength()
        {
            return 16;
        }

        private void transform()
        {
            int[] m = new int[16];
            for (int i = 0; i < 16; i++)
            {
                m[i] = (block[i * 4] & 0xff)
                    | ((block[i * 4 + 1] & 0xff) << 8)
                    | ((block[i * 4 + 2] & 0xff) << 16)
                    | ((block[i * 4 + 3] & 0xff) << 24);
            }
            int a = state[0];
            int b = state[1];
            int c = state[2];
            int d = state[3];
            for (int i = 0; i < 64; i++)
            {
                int f;
                int g;
                if (i < 16)
                {
                    f = (b & c) | (~b & d);
                    g = i;
                }
                else if (i < 32)
                {
                    f = (d & b) | (~d & c);
                    g = (5 * i + 1) % 16;
                }
                else if (i < 48)
                {
                    f = b ^ c ^ d;
                    g = (3 * i + 5) % 16;
                }
                else
                {
                    f = c ^ (b | ~d);
                    g = (7 * i) % 16;
                }
                int temp = d;
                d = c;
                c = b;
                b = b + rotateLeft(a + f + K[i] + m[g], S[i]);
                a = temp;
            }
            state[0] += a;
            state[1] += b;
            state[2] += c;
            state[3] += d;
        }
    }

    private static final class SHA256 extends MessageDigest
    {
        private final int[] state;
        private final byte[] block = new byte[64];
        private int blockLength;
        private long totalLength;

        SHA256()
        {
            super("SHA-256");
            state = new int[] {
                0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
                0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
            };
        }

        protected void engineUpdate(byte input)
        {
            block[blockLength] = input;
            blockLength++;
            totalLength++;
            if (blockLength == 64)
            {
                transform();
                blockLength = 0;
            }
        }

        protected void engineUpdate(byte[] input, int offset, int length)
        {
            for (int i = 0; i < length; i++)
            {
                engineUpdate(input[offset + i]);
            }
        }

        protected byte[] engineDigest()
        {
            long bitLength = totalLength * 8L;
            engineUpdate((byte)0x80);
            while (blockLength != 56)
            {
                engineUpdate((byte)0);
            }
            for (int i = 7; i >= 0; i--)
            {
                engineUpdate((byte)(bitLength >>> (i * 8)));
            }
            byte[] result = new byte[32];
            for (int i = 0; i < 8; i++)
            {
                result[i * 4] = (byte)(state[i] >>> 24);
                result[i * 4 + 1] = (byte)(state[i] >>> 16);
                result[i * 4 + 2] = (byte)(state[i] >>> 8);
                result[i * 4 + 3] = (byte)state[i];
            }
            engineReset();
            return result;
        }

        protected void engineReset()
        {
            state[0] = 0x6a09e667;
            state[1] = 0xbb67ae85;
            state[2] = 0x3c6ef372;
            state[3] = 0xa54ff53a;
            state[4] = 0x510e527f;
            state[5] = 0x9b05688c;
            state[6] = 0x1f83d9ab;
            state[7] = 0x5be0cd19;
            blockLength = 0;
            totalLength = 0L;
        }

        protected int engineGetDigestLength()
        {
            return 32;
        }

        private void transform()
        {
            int[] w = new int[64];
            for (int i = 0; i < 16; i++)
            {
                w[i] = ((block[i * 4] & 0xff) << 24)
                    | ((block[i * 4 + 1] & 0xff) << 16)
                    | ((block[i * 4 + 2] & 0xff) << 8)
                    | (block[i * 4 + 3] & 0xff);
            }
            for (int i = 16; i < 64; i++)
            {
                int s0 = rotateRight(w[i - 15], 7) ^ rotateRight(w[i - 15], 18) ^ (w[i - 15] >>> 3);
                int s1 = rotateRight(w[i - 2], 17) ^ rotateRight(w[i - 2], 19) ^ (w[i - 2] >>> 10);
                w[i] = w[i - 16] + s0 + w[i - 7] + s1;
            }
            int a = state[0];
            int b = state[1];
            int c = state[2];
            int d = state[3];
            int e = state[4];
            int f = state[5];
            int g = state[6];
            int h = state[7];
            for (int i = 0; i < 64; i++)
            {
                int s1 = rotateRight(e, 6) ^ rotateRight(e, 11) ^ rotateRight(e, 25);
                int ch = (e & f) ^ (~e & g);
                int temp1 = h + s1 + ch + SHA256_K[i] + w[i];
                int s0 = rotateRight(a, 2) ^ rotateRight(a, 13) ^ rotateRight(a, 22);
                int maj = (a & b) ^ (a & c) ^ (b & c);
                int temp2 = s0 + maj;
                h = g;
                g = f;
                f = e;
                e = d + temp1;
                d = c;
                c = b;
                b = a;
                a = temp1 + temp2;
            }
            state[0] += a;
            state[1] += b;
            state[2] += c;
            state[3] += d;
            state[4] += e;
            state[5] += f;
            state[6] += g;
            state[7] += h;
        }
    }

    private static final class SHA1 extends MessageDigest
    {
        private final int[] state = new int[5];
        private final byte[] block = new byte[64];
        private int blockLength;
        private long totalLength;

        SHA1()
        {
            super("SHA-1");
            engineReset();
        }

        protected void engineUpdate(byte input)
        {
            block[blockLength] = input;
            blockLength++;
            totalLength++;
            if (blockLength == 64)
            {
                transform();
                blockLength = 0;
            }
        }

        protected void engineUpdate(byte[] input, int offset, int length)
        {
            for (int i = 0; i < length; i++)
            {
                engineUpdate(input[offset + i]);
            }
        }

        protected byte[] engineDigest()
        {
            long bitLength = totalLength * 8L;
            engineUpdate((byte)0x80);
            while (blockLength != 56)
            {
                engineUpdate((byte)0);
            }
            for (int i = 7; i >= 0; i--)
            {
                engineUpdate((byte)(bitLength >>> (i * 8)));
            }
            byte[] result = new byte[20];
            for (int i = 0; i < 5; i++)
            {
                result[i * 4] = (byte)(state[i] >>> 24);
                result[i * 4 + 1] = (byte)(state[i] >>> 16);
                result[i * 4 + 2] = (byte)(state[i] >>> 8);
                result[i * 4 + 3] = (byte)state[i];
            }
            engineReset();
            return result;
        }

        protected void engineReset()
        {
            state[0] = 0x67452301;
            state[1] = 0xefcdab89;
            state[2] = 0x98badcfe;
            state[3] = 0x10325476;
            state[4] = 0xc3d2e1f0;
            blockLength = 0;
            totalLength = 0L;
        }

        protected int engineGetDigestLength()
        {
            return 20;
        }

        private void transform()
        {
            int[] w = new int[80];
            for (int i = 0; i < 16; i++)
            {
                w[i] = ((block[i * 4] & 0xff) << 24)
                    | ((block[i * 4 + 1] & 0xff) << 16)
                    | ((block[i * 4 + 2] & 0xff) << 8)
                    | (block[i * 4 + 3] & 0xff);
            }
            for (int i = 16; i < 80; i++)
            {
                w[i] = rotateLeft(w[i - 3] ^ w[i - 8] ^ w[i - 14] ^ w[i - 16], 1);
            }
            int a = state[0];
            int b = state[1];
            int c = state[2];
            int d = state[3];
            int e = state[4];
            for (int i = 0; i < 80; i++)
            {
                int f;
                int k;
                if (i < 20)
                {
                    f = (b & c) | (~b & d);
                    k = 0x5a827999;
                }
                else if (i < 40)
                {
                    f = b ^ c ^ d;
                    k = 0x6ed9eba1;
                }
                else if (i < 60)
                {
                    f = (b & c) | (b & d) | (c & d);
                    k = 0x8f1bbcdc;
                }
                else
                {
                    f = b ^ c ^ d;
                    k = 0xca62c1d6;
                }
                int temp = rotateLeft(a, 5) + f + e + k + w[i];
                e = d;
                d = c;
                c = rotateLeft(b, 30);
                b = a;
                a = temp;
            }
            state[0] += a;
            state[1] += b;
            state[2] += c;
            state[3] += d;
            state[4] += e;
        }
    }

    private static int rotateRight(int value, int bits)
    {
        return (value >>> bits) | (value << (32 - bits));
    }

    private static int rotateLeft(int value, int bits)
    {
        return (value << bits) | (value >>> (32 - bits));
    }
}
