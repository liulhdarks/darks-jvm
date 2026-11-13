public class P0Checks
{
    static int checks = 0;
    static int failures = 0;

    static void mark(String name, boolean ok)
    {
        checks = checks + 1;
        if (!ok)
        {
            failures = failures + 1;
            System.out.println("FAIL " + name);
        }
    }

    static int divI(int a, int b) { return a / b; }
    static int remI(int a, int b) { return a % b; }
    static long divL(long a, long b) { return a / b; }
    static long remL(long a, long b) { return a % b; }

    static class Base
    {
        int x = 1;
        int ax() { return x; }
        void setX(int v) { x = v; }
    }

    static class Derived extends Base
    {
        int x = 2;
        int bx() { return x; }
        void setBx(int v) { x = v; }
    }

    static class Holder
    {
        Object value;
        Object get() { return value; }
        void set(Object v) { value = v; }
    }

    static class Const
    {
        int zero() { return 0; }
        int five() { return 5; }
        long big() { return 1L; }
        String nul() { return null; }
        void nop() { }
    }

    static int wideLocals()
    {
        int v0 = 1;
        int v1 = 2;
        int v2 = 3;
        int v3 = 4;
        int v4 = 5;
        int v5 = 6;
        int v6 = 7;
        int v7 = 8;
        int v8 = 9;
        int v9 = 10;
        int v10 = 11;
        int v11 = 12;
        int v12 = 13;
        int v13 = 14;
        int v14 = 15;
        int v15 = 16;
        int v16 = 17;
        int v17 = 18;
        int v18 = 19;
        int v19 = 20;
        int v20 = 21;
        int v21 = 22;
        int v22 = 23;
        int v23 = 24;
        int v24 = 25;
        int v25 = 26;
        int v26 = 27;
        int v27 = 28;
        int v28 = 29;
        int v29 = 30;
        int v30 = 31;
        int v31 = 32;
        int v32 = 33;
        int v33 = 34;
        int v34 = 35;
        int v35 = 36;
        int v36 = 37;
        int v37 = 38;
        int v38 = 39;
        int v39 = 40;
        int v40 = 41;
        int v41 = 42;
        int v42 = 43;
        int v43 = 44;
        int v44 = 45;
        int v45 = 46;
        int v46 = 47;
        int v47 = 48;
        int v48 = 49;
        int v49 = 50;
        int v50 = 51;
        int v51 = 52;
        int v52 = 53;
        int v53 = 54;
        int v54 = 55;
        int v55 = 56;
        int v56 = 57;
        int v57 = 58;
        int v58 = 59;
        int v59 = 60;
        int v60 = 61;
        int v61 = 62;
        int v62 = 63;
        int v63 = 64;
        int v64 = 65;
        int v65 = 66;
        int v66 = 67;
        int v67 = 68;
        int v68 = 69;
        int v69 = 70;
        int v70 = 71;
        int v71 = 72;
        int v72 = 73;
        int v73 = 74;
        int v74 = 75;
        int v75 = 76;
        int v76 = 77;
        int v77 = 78;
        int v78 = 79;
        int v79 = 80;
        int v80 = 81;
        int v81 = 82;
        int v82 = 83;
        int v83 = 84;
        int v84 = 85;
        int v85 = 86;
        int v86 = 87;
        int v87 = 88;
        int v88 = 89;
        int v89 = 90;
        int v90 = 91;
        int v91 = 92;
        int v92 = 93;
        int v93 = 94;
        int v94 = 95;
        int v95 = 96;
        int v96 = 97;
        int v97 = 98;
        int v98 = 99;
        int v99 = 100;
        int v100 = 101;
        int v101 = 102;
        int v102 = 103;
        int v103 = 104;
        int v104 = 105;
        int v105 = 106;
        int v106 = 107;
        int v107 = 108;
        int v108 = 109;
        int v109 = 110;
        int v110 = 111;
        int v111 = 112;
        int v112 = 113;
        int v113 = 114;
        int v114 = 115;
        int v115 = 116;
        int v116 = 117;
        int v117 = 118;
        int v118 = 119;
        int v119 = 120;
        int v120 = 121;
        int v121 = 122;
        int v122 = 123;
        int v123 = 124;
        int v124 = 125;
        int v125 = 126;
        int v126 = 127;
        int v127 = 128;
        int v128 = 129;
        int v129 = 130;
        int v130 = 131;
        int v131 = 132;
        int v132 = 133;
        int v133 = 134;
        int v134 = 135;
        int v135 = 136;
        int v136 = 137;
        int v137 = 138;
        int v138 = 139;
        int v139 = 140;
        int v140 = 141;
        int v141 = 142;
        int v142 = 143;
        int v143 = 144;
        int v144 = 145;
        int v145 = 146;
        int v146 = 147;
        int v147 = 148;
        int v148 = 149;
        int v149 = 150;
        int v150 = 151;
        int v151 = 152;
        int v152 = 153;
        int v153 = 154;
        int v154 = 155;
        int v155 = 156;
        int v156 = 157;
        int v157 = 158;
        int v158 = 159;
        int v159 = 160;
        int v160 = 161;
        int v161 = 162;
        int v162 = 163;
        int v163 = 164;
        int v164 = 165;
        int v165 = 166;
        int v166 = 167;
        int v167 = 168;
        int v168 = 169;
        int v169 = 170;
        int v170 = 171;
        int v171 = 172;
        int v172 = 173;
        int v173 = 174;
        int v174 = 175;
        int v175 = 176;
        int v176 = 177;
        int v177 = 178;
        int v178 = 179;
        int v179 = 180;
        int v180 = 181;
        int v181 = 182;
        int v182 = 183;
        int v183 = 184;
        int v184 = 185;
        int v185 = 186;
        int v186 = 187;
        int v187 = 188;
        int v188 = 189;
        int v189 = 190;
        int v190 = 191;
        int v191 = 192;
        int v192 = 193;
        int v193 = 194;
        int v194 = 195;
        int v195 = 196;
        int v196 = 197;
        int v197 = 198;
        int v198 = 199;
        int v199 = 200;
        int v200 = 201;
        int v201 = 202;
        int v202 = 203;
        int v203 = 204;
        int v204 = 205;
        int v205 = 206;
        int v206 = 207;
        int v207 = 208;
        int v208 = 209;
        int v209 = 210;
        int v210 = 211;
        int v211 = 212;
        int v212 = 213;
        int v213 = 214;
        int v214 = 215;
        int v215 = 216;
        int v216 = 217;
        int v217 = 218;
        int v218 = 219;
        int v219 = 220;
        int v220 = 221;
        int v221 = 222;
        int v222 = 223;
        int v223 = 224;
        int v224 = 225;
        int v225 = 226;
        int v226 = 227;
        int v227 = 228;
        int v228 = 229;
        int v229 = 230;
        int v230 = 231;
        int v231 = 232;
        int v232 = 233;
        int v233 = 234;
        int v234 = 235;
        int v235 = 236;
        int v236 = 237;
        int v237 = 238;
        int v238 = 239;
        int v239 = 240;
        int v240 = 241;
        int v241 = 242;
        int v242 = 243;
        int v243 = 244;
        int v244 = 245;
        int v245 = 246;
        int v246 = 247;
        int v247 = 248;
        int v248 = 249;
        int v249 = 250;
        int v250 = 251;
        int v251 = 252;
        int v252 = 253;
        int v253 = 254;
        int v254 = 255;
        int v255 = 256;
        int v256 = 257;
        int v257 = 258;
        int v258 = 259;
        int v259 = 260;
        int v260 = 261;
        int v261 = 262;
        int v262 = 263;
        int v263 = 264;
        int v264 = 265;
        int v265 = 266;
        int v266 = 267;
        int v267 = 268;
        int v268 = 269;
        int v269 = 270;
        int v270 = 271;
        int v271 = 272;
        int v272 = 273;
        int v273 = 274;
        int v274 = 275;
        int v275 = 276;
        int v276 = 277;
        int v277 = 278;
        int v278 = 279;
        int v279 = 280;
        int v280 = 281;
        int v281 = 282;
        int v282 = 283;
        int v283 = 284;
        int v284 = 285;
        int v285 = 286;
        int v286 = 287;
        int v287 = 288;
        int v288 = 289;
        int v289 = 290;
        int v290 = 291;
        int v291 = 292;
        int v292 = 293;
        int v293 = 294;
        int v294 = 295;
        int v295 = 296;
        int v296 = 297;
        int v297 = 298;
        int v298 = 299;
        int v299 = 300;
        v298 = v298 + 5;
        v299 = v299 + 5;
        long wl = 7L;
        double wd = 2.5;
        return v0 + v255 + v298 + v299 + (int)(wl + wd);
    }

    public static void main(String[] args)
    {
        int[] a = new int[2];
        boolean threw = false;
        try { a[a.length] = 1; } catch (ArrayIndexOutOfBoundsException e) { threw = true; }
        mark("aioobe-store", threw);

        threw = false;
        try { int v = a[-1]; } catch (ArrayIndexOutOfBoundsException e) { threw = true; }
        mark("aioobe-load", threw);

        threw = false;
        try { int[] b = null; int n = b.length; } catch (NullPointerException e) { threw = true; }
        mark("npe-array-length", threw);

        threw = false;
        try { Object[] b = null; b[0] = null; } catch (NullPointerException e) { threw = true; }
        mark("npe-astore", threw);

        threw = false;
        try { int[] b = new int[-3]; } catch (NegativeArraySizeException e) { threw = true; }
        mark("negative-array", threw);

        Object[] strings = new String[1];
        threw = false;
        try { strings[0] = new Object(); } catch (ArrayStoreException e) { threw = true; }
        mark("aastore-check", threw);
        strings[0] = "ok";
        mark("aastore-ok", "ok".equals(strings[0]));

        Object[] src = new Object[] { new Integer(3) };
        String[] dst = new String[1];
        threw = false;
        try { System.arraycopy(src, 0, dst, 0, 1); } catch (ArrayStoreException e) { threw = true; }
        mark("arraycopy-check", threw);

        String[] good = new String[] { "z" };
        Object[] out = new Object[1];
        System.arraycopy(good, 0, out, 0, 1);
        mark("arraycopy-ok", "z".equals(out[0]));

        mark("wide-locals", wideLocals() == 875);

        mark("int-min-div", divI(Integer.MIN_VALUE, -1) == Integer.MIN_VALUE);
        mark("int-min-rem", remI(Integer.MIN_VALUE, -1) == 0);
        mark("long-min-div", divL(Long.MIN_VALUE, -1L) == Long.MIN_VALUE);
        mark("long-min-rem", remL(Long.MIN_VALUE, -1L) == 0L);

        Runtime rt = Runtime.getRuntime();
        mark("runtime-singleton", rt != null && rt == Runtime.getRuntime());
        mark("runtime-cpus", rt.availableProcessors() > 0);
        long free = rt.freeMemory();
        long total = rt.totalMemory();
        long max = rt.maxMemory();
        mark("runtime-memory", free > 0L && total > 0L && max >= total);
        rt.gc();
        mark("runtime-gc", true);

        mark("simple-class", P0Checks.class.getSimpleName().equals("P0Checks"));
        mark("simple-inner", Base.class.getSimpleName().equals("Base"));
        mark("simple-int-array", int[].class.getSimpleName().equals("int[]"));
        mark("simple-ref-array", String[].class.getSimpleName().equals("String[]"));

        Thread fresh = new Thread();
        mark("thread-new", fresh.getState() == Thread.State.NEW);
        mark("thread-current", Thread.currentThread().getState() == Thread.State.RUNNABLE);

        Derived d = new Derived();
        mark("hidden-getters", d.ax() == 1 && d.bx() == 2);
        d.setX(10);
        d.setBx(20);
        mark("hidden-setters", d.ax() == 10 && d.bx() == 20);

        Holder h = new Holder();
        h.set("hello");
        mark("ref-getter", "hello".equals(h.get()));
        h.set(null);
        mark("ref-getter-null", h.get() == null);

        Const c = new Const();
        mark("inline-const", c.zero() == 0 && c.five() == 5 && c.big() == 1L && c.nul() == null);
        c.nop();
        mark("inline-empty", true);

        System.out.println("p0 checks=" + checks + " failures=" + failures);
        if (failures == 0)
        {
            System.out.println("p0 checks: ok");
        }

    }
}
