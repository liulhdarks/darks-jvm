#pragma once


#define nop 0
#define aconst_null 1 /*(0x1)*/ 
#define iconst_m1 2 /*(0x2)*/ 
#define iconst_0 3 /*(0x3)*/
#define iconst_1 4 /*(0x4)*/ 
#define iconst_2 5 /*(0x5)*/ 
#define iconst_3 6 /*(0x6)*/ 
#define iconst_4 7 /*(0x7)*/ 
#define iconst_5 8 /*(0x8)*/

#define bipush 16 /*(0x10)*/
#define sipush 17 /*(0x11)*/

#define lconst_0 9 /*(0x9)*/ 
#define lconst_1 10 /*(0xa)*/
#define fconst_0 11 /*(0xb)*/ 
#define fconst_1 12 /*(0xc)*/
#define fconst_2 13 /*(0xd)*/
#define dconst_0 14 /*(0xe)*/ 
#define dconst_1 15 /*(0xf)*/

#define ldc 18 /* (0x12) */

#define ldc2_w 20 /*(0x14)*/

#define iload 21 /*(0x15)*/

#define lload 22 /*(0x16)*/

#define aload 25 /*(0x19)*/

#define iload_0 26 /*(0x1a)*/ 
#define iload_1 27 /*(0x1b)*/ 
#define iload_2 28 /*(0x1c)*/ 
#define iload_3 29 /*(0x1d)*/

#define lload_0 30 /*(0x1e) */
#define lload_1 31 /*(0x1f) */
#define lload_2 32 /*(0x20) */
#define lload_3 33 /*(0x21) */

#define fload_0 34 /*(0x22)*/ 
#define fload_1 35 /*(0x23) */
#define fload_2 36 /*(0x24) */
#define fload_3 37 /*(0x25)*/

#define aload_0 42 /* (0x2a) */
#define aload_1 43 /*(0x2b) */
#define aload_2 44 /*(0x2c) */
#define aload_3 45 /*(0x2d)*/

#define iaload 46 /*(0x2e)*/
#define laload 47
#define faload 48
#define daload 49
#define aaload 50
#define baload 51
#define caload 52
#define saload 53

#define istore 54 /*(0x36)*/
#define lstore 55 /*(0x37)*/
#define fstore 56 /*(0x38)*/
#define dstore 57 /*(0x39)*/
#define astore 58 /*(0x3a)*/

#define istore_0 59 /*(0x3b)*/ 
#define istore_1 60 /*(0x3c) */
#define istore_2 61 /*(0x3d) */
#define istore_3 62 /*(0x3e)*/

#define lstore_0 63 /*(0x3f) */
#define lstore_1 64 /*(0x40) */
#define lstore_2 65 /*(0x41) */
#define lstore_3 66 /*(0x42) */

#define fstore_0 67 /*(0x43) */
#define fstore_1 68 /*(0x44) */
#define fstore_2 69 /*(0x45) */
#define fstore_3 70 /*(0x46) */

#define dstore_0 71 /*(0x47) */
#define dstore_1 72 /*(0x48) */
#define dstore_2 73 /*(0x49) */
#define dstore_3 74 /*(0x50) */

#define astore_0 75 /*(0x4b) */
#define astore_1 76 /*(0x4c) */
#define astore_2 77 /*(0x4d) */
#define astore_3 78 /*(0x4e)*/

#define iastore 79 /*(0x4f)*/
#define lastore 80 /*(0x50)*/
#define fastore 81 /*(0x51)*/
#define dastore 82 /*(0x52)*/
#define aastore 83 /*(0x53)*/
#define bastore 84 /*(0x54)*/
#define castore 85 /*(0x55)*/
#define sastore 86 /*(0x56)*/

#define _pop 87 /*(0x57)*/
#define _pop2 88 /*(0x58)*/
#define dup 89 /*(0x59)*/
#define dup_x1 90 /*(0x5a)*/
#define dup_x2 91 /*(0x5b)*/
#define dup2 92 /*(0x5c)*/
#define dup2_x1 93 /*(0x5d)*/
#define dup2_x2 94 /*(0x5e)*/

#define iadd 96 /*(0x60)*/
#define ladd 97 /*(0x61)*/
#define fadd 98 /*(0x62)*/
#define dadd 99 /*(0x63)*/

#define isub 100 /*(0x64)*/
#define lsub 101 /*(0x65)*/
#define fsub 102 /*(0x66)*/
#define dsub 103 /*(0x67)*/
#define imul 104 /*(0x68)*/
#define lmul 105 /*(0x69)*/
#define fmul 106 /*(0x6a)*/
#define dmul 107 /*(0x6b)*/
#define idiv 108 /*(0x6c)*/
#define ldiv 109 /*(0x6d)*/
#define fdiv 110 /*(0x6e)*/
#define ddiv 111 /*(0x6f)*/
#define irem 112 /*(0x70)*/
#define lrem 113 /*(0x71)*/
#define frem 114 /*(0x72)*/
#define drem 115 /*(0x73)*/

#define ineg 116 /*(0x74)*/
#define lneg 117 /*(0x75)*/
#define fneg 118 /*(0x76)*/
#define dneg 119 /*(0x77)*/
#define ishl 120 /*(0x78)*/
#define lshl 121 /*(0x79)*/
#define ishr 122 /*(0x7a)*/
#define lshr 123 /*(0x7b)*/
#define iushr 124 /*(0x7c)*/
#define lushr 125 /*(0x7d)*/
#define iand 126 /*(0x7e)*/
#define land 127 /*(0x7f)*/
#define ior 128 /*(0x80)*/
#define lor 129 /*(0x81)*/
#define ixor 130 /*(0x82)*/
#define lxor 131 /*(0x83)*/
#define iinc 132 /*(0x84)*/
#define i2l 133 /*(0x85)*/
#define i2f 134 /*(0x86)*/
#define i2d 135 /*(0x87)*/
#define l2i 136 /*(0x88)*/
#define l2f 137 /*(0x89)*/
#define l2d 138 /*(0x8a)*/
#define f2i 139 /*(0x8b)*/
#define f2l 140 /*(0x8c)*/
#define f2d 141 /*(0x8d)*/
#define d2i 142 /*(0x8e)*/
#define d2l 143 /*(0x9f)*/
#define d2f 144 /*(0x90)*/
#define i2b 145 /*(0x91)*/
#define i2c 146 /*(0x92)*/
#define i2s 147 /*(0x93)*/

#define ifeq 153 /*(0x99) */
#define ifne 154 /*(0x9a) */
#define iflt 155 /*(0x9b) */
#define ifge 156 /*(0x9c) */
#define ifgt 157 /*(0x9d) */
#define ifle 158 /*(0x9e)*/


#define if_icmpeq 159 /*(0x9f) */
#define if_icmpne 160 /*(0xa0) */
#define if_icmplt 161 /*(0xa1) */
#define if_icmpge 162 /*(0xa2) */
#define if_icmpgt 163 /*(0xa3) */
#define if_icmple 164 /*(0xa4)*/


#define _goto 167 /*(0xa7)*/

#define tableswitch 170 /*(0xaa)*/
#define lookupswitch 171 /*(0xab)*/

#define ireturn 172 /*(0xac)*/
#define lreturn 173 /*(0xad)*/
#define freturn 174 /*(0xae)*/
#define dreturn 175 /*(0xaf)*/
#define areturn 176 /*(0xb0)*/
#define _return 177 /*(0xb1)*/

#define getstatic 178 /*(0xb2)*/
#define putstatic 179 /*(0xb3)*/
#define getfield 180 /*(0xb4)*/
#define putfield 181 /*(0xb5)*/

#define invokevirtual 182 /*(0xb6)*/
#define invokespecial  183 /*(0xb7) */
#define invokestatic 184 

#define _new 187 /*(0xbb)*/

#define newarray 188 /*(0xbc)*/
#define anewarray 189 /*(0xbd)*/
#define arraylength 190 /*(0xbe)*/

#define athrow 191 /* (0xbf) */
#define checkcast 192 /* (0xc0) */
#define instanceof 193 /* (0xc1) */ 
#define monitorenter 194 /* (0xc2) */
#define monitorexit 195 /* (0xc3) */
#define multianewarray 197 /* (0xc5) */
#define ifnull 198 /* (0xc6) */
#define ifnonnull 199 /* (0xc7) */
