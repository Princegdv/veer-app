package com.veer.assistant;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Hindi (Devanagari) -> Roman Hinglish. Pure Java, no Android classes. */
final class Hindi {
    private static final Map<Character, String> C = new HashMap<>();
    private static final Map<Character, String> V = new HashMap<>();
    private static final Map<Character, String> M = new HashMap<>();
    private static final Map<Character, String> NUKTA = new HashMap<>();
    private static final Map<String, String> WORDS = new HashMap<>();
    private static final Pattern DEVA = Pattern.compile("[\\u0900-\\u097F]");
    private static final Set<String> UNITS = new HashSet<>(Arrays.asList("\u092E\u093F\u0928\u091F", "\u0938\u0947\u0915\u0902\u0921", "\u0938\u0947\u0915\u0947\u0902\u0921", "\u0918\u0902\u091F\u093E", "\u0918\u0902\u091F\u0947", "\u092C\u091C\u0947"));

    private static final String RAW =
            "\u0935\u0940\u0930=veer|\u092C\u0940\u0930=veer|\u0935\u093F\u0930=veer|\u0935\u093F\u092F\u0930=veer|"
            + "\u0935\u0940\u0930\u093E=veer|\u0939\u0947\u0932\u094B=hello|\u0939\u0948\u0932\u094B=hello|"
            + "\u0939\u0932\u094B=hello|\u0939\u093E\u092F=hi|\u0928\u092E\u0938\u094D\u0924\u0947=namaste|"
            + "\u092C\u093E\u092F=bye|\u092E\u0926\u0926=madad|\u0939\u0947\u0932\u094D\u092A=help|"
            + "\u0928\u093E\u092E=naam|\u092C\u0926\u0932\u094B=badlo|\u0930\u0941\u0915\u094B=ruk|"
            + "\u0930\u0941\u0915=ruk|\u091C\u093E\u0913=jao|\u0915\u094C\u0928=kaun|\u0915\u094D\u092F\u093E=kya|"
            + "\u0915\u0948\u0938\u093E=kaisa|\u0915\u0948\u0938\u0940=kaisi|\u0915\u093F\u0924\u0928\u093E=kitna|"
            + "\u0915\u093F\u0924\u0928\u0940=kitni|\u0915\u093F\u0924\u0928\u0947=kitne|\u0924\u0941\u092E=tum|"
            + "\u0906\u092A=aap|\u0939\u0948=hai|\u0939\u0948\u0902=hain|\u0915\u093E=ka|\u0915\u0940=ki|"
            + "\u0915\u0947=ke|\u0915\u094B=ko|\u0938\u0947=se|\u092A\u0930=par|\u092E\u0947\u0902=mein|"
            + "\u0914\u0930=aur|\u092E\u0941\u091D\u0947=mujhe|\u092E\u0947\u0930\u093E=mera|"
            + "\u092E\u0947\u0930\u0947=mere|\u092E\u0947\u0930\u0940=meri|\u0905\u092D\u0940=abhi|\u0906\u091C=aaj|"
            + "\u0915\u0932=kal|\u092C\u093E\u0926=baad|\u092C\u091C\u0947=baje|"
            + "\u092F\u0942\u091F\u094D\u092F\u0942\u092C=youtube|\u092F\u0942\u091F\u092F\u0942\u092C=youtube|"
            + "\u0935\u094D\u0939\u093E\u091F\u094D\u0938\u090F\u092A=whatsapp|"
            + "\u0935\u094D\u0939\u093E\u091F\u094D\u0938\u0910\u092A=whatsapp|"
            + "\u0935\u093E\u091F\u094D\u0938\u090F\u092A=whatsapp|"
            + "\u0935\u0949\u091F\u094D\u0938\u0910\u092A=whatsapp|"
            + "\u0935\u094D\u0939\u093E\u091F\u094D\u0938\u0905\u092A=whatsapp|"
            + "\u0907\u0902\u0938\u094D\u091F\u093E\u0917\u094D\u0930\u093E\u092E=instagram|"
            + "\u0915\u094D\u0930\u094B\u092E=chrome|\u0915\u0948\u092E\u0930\u093E=camera|"
            + "\u0917\u0948\u0932\u0930\u0940=gallery|\u0938\u0947\u091F\u093F\u0902\u0917=settings|"
            + "\u0938\u0947\u091F\u093F\u0902\u0917\u094D\u0938=settings|"
            + "\u091F\u0947\u0932\u0940\u0917\u094D\u0930\u093E\u092E=telegram|"
            + "\u092B\u094B\u0928\u092A\u0947=phonepe|\u091C\u0940\u092A\u0947=gpay|"
            + "\u091A\u0932\u093E\u0928\u093E=chalao|\u091A\u0932\u093E\u0913=chalao|"
            + "\u091A\u0932\u093E\u0907\u090F=chalao|\u091A\u0932\u093E\u090F\u0902=chalao|"
            + "\u091A\u093E\u0932\u0942=chalu|\u091A\u093E\u0932\u0941=chalu|\u0936\u0941\u0930\u0942=shuru|"
            + "\u0936\u0941\u0930\u0941=shuru|\u0938\u094D\u091F\u093E\u0930\u094D\u091F=start|"
            + "\u0938\u094D\u091F\u0949\u092A=stop|\u092C\u0902\u0926=band|\u092C\u0928\u094D\u0926=band|"
            + "\u0916\u094B\u0932\u094B=kholo|\u0916\u094B\u0932\u093F\u090F=kholo|\u0916\u094B\u0932=khol|"
            + "\u0913\u092A\u0928=open|\u0915\u0930\u094B=karo|\u0915\u0930=kar|\u0915\u0930\u0928\u093E=karna|"
            + "\u091C\u0932\u093E\u0913=chalu|\u091C\u0932\u093E\u0928\u093E=chalu|"
            + "\u092C\u0941\u091D\u093E\u0913=band|\u0926\u093F\u0916\u093E\u0913=dikhao|"
            + "\u092C\u0924\u093E\u0913=batao|\u0932\u093F\u0916\u094B=likho|\u0938\u0941\u0928\u093E\u0913=sunao|"
            + "\u092C\u091C\u093E\u0913=bajao|\u092A\u094D\u0932\u0947=play|\u092F\u093E\u0926=yaad|"
            + "\u0926\u093F\u0932\u093E\u0913=dilao|\u0926\u093F\u0932\u093E=dilao|\u0905\u0917\u0932\u093E=agla|"
            + "\u0905\u0917\u0932\u0940=agla|\u0905\u0917\u0932\u0947=agla|\u0906\u0917\u0947=aage|"
            + "\u092A\u093F\u091B\u0932\u093E=pichla|\u092A\u093F\u091B\u0932\u0940=pichla|"
            + "\u092A\u0940\u091B\u0947=peeche|\u0930\u0940\u0932=reel|\u0930\u0940\u0932\u094D\u0938=reel|"
            + "\u0930\u093F\u0932=reel|\u0935\u0940\u0921\u093F\u092F\u094B=video|"
            + "\u0936\u0949\u0930\u094D\u091F=short|\u0936\u0949\u0930\u094D\u091F\u094D\u0938=short|"
            + "\u0932\u093E\u0907\u0915=like|\u092A\u0938\u0902\u0926=pasand|\u092C\u0948\u0915=back|"
            + "\u0935\u093E\u092A\u0938=wapas|\u0939\u094B\u092E=home|"
            + "\u0938\u094D\u0915\u094D\u0930\u0940\u0928=screen|"
            + "\u0938\u094D\u0915\u094D\u0930\u0940\u0928\u0936\u0949\u091F=screenshot|"
            + "\u0938\u094D\u0915\u094D\u0930\u0949\u0932=scroll|\u0928\u0940\u091A\u0947=neeche|"
            + "\u090A\u092A\u0930=upar|\u0930\u0940\u0938\u0947\u0902\u091F=recent|\u091F\u093E\u0907\u092A=type|"
            + "\u091F\u0948\u092A=tap|\u0932\u0949\u0915=lock|\u0906\u0935\u093E\u091C=volume|"
            + "\u0906\u0935\u093E\u091C\u093C=volume|\u0935\u0949\u0932\u094D\u092F\u0942\u092E=volume|"
            + "\u092C\u0922\u093C\u093E\u0913=badhao|\u092C\u0922\u093C\u093E=badhao|\u092C\u0922\u093E\u0913=badhao|"
            + "\u0918\u091F\u093E\u0913=ghatao|\u0915\u092E=kam|\u091C\u094D\u092F\u093E\u0926\u093E=zyada|"
            + "\u091C\u093C\u094D\u092F\u093E\u0926\u093E=zyada|"
            + "\u092C\u094D\u0930\u093E\u0907\u091F\u0928\u0947\u0938=brightness|"
            + "\u092C\u0948\u091F\u0930\u0940=battery|\u091A\u093E\u0930\u094D\u091C=charge|"
            + "\u091F\u0949\u0930\u094D\u091A=torch|\u092B\u094D\u0932\u0948\u0936=flash|"
            + "\u092B\u094D\u0932\u0948\u0936\u0932\u093E\u0907\u091F=torch|\u091F\u093E\u0907\u092E\u0930=timer|"
            + "\u0905\u0932\u093E\u0930\u094D\u092E=alarm|\u0930\u093F\u092E\u093E\u0907\u0902\u0921\u0930=reminder|"
            + "\u0928\u094B\u091F=note|\u0928\u094B\u091F\u094D\u0938=notes|\u092E\u094C\u0938\u092E=mausam|"
            + "\u091C\u094B\u0915=joke|\u091A\u0941\u091F\u0915\u0941\u0932\u093E=chutkula|"
            + "\u0939\u0902\u0938\u093E\u0913=hasao|\u092C\u0949\u091F=bot|\u091F\u094D\u0930\u0947\u0921=trade|"
            + "\u091F\u094D\u0930\u0947\u0921\u093F\u0902\u0917=trade|"
            + "\u092A\u094D\u0930\u0949\u092B\u093F\u091F=profit|\u092E\u0941\u0928\u093E\u092B\u093E=munafa|"
            + "\u0938\u094D\u091F\u0947\u091F\u0938=status|\u0938\u092E\u092F=samay|\u091F\u093E\u0907\u092E=time|"
            + "\u0924\u093E\u0930\u0940\u0916=tarikh|\u0921\u0947\u091F=date|\u092E\u093F\u0928\u091F=minute|"
            + "\u0938\u0947\u0915\u0902\u0921=second|\u0938\u0947\u0915\u0947\u0902\u0921=second|"
            + "\u0918\u0902\u091F\u093E=ghanta|\u0918\u0902\u091F\u0947=ghante|\u0917\u093E\u0928\u093E=gana|"
            + "\u0917\u093E\u0928\u0947=gana|\u0917\u0940\u0924=gana|\u0938\u0949\u0928\u094D\u0917=song|"
            + "\u0938\u094B\u0902\u0917=song|\u0915\u0949\u0932=call|\u092B\u094B\u0928=phone|"
            + "\u090F\u0938\u090F\u092E\u090F\u0938=sms|\u092E\u0948\u0938\u0947\u091C=message|"
            + "\u0938\u093F\u0902\u0939=singh|\u0905\u0930\u093F\u091C\u0940\u0924=arijit|"
            + "\u090F\u092A\u0940\u0906\u0908=api|\u090F\u0921\u0940\u092C\u0940=adb|"
            + "\u0915\u0928\u0947\u0915\u094D\u091F=connect|\u090F\u0915=1|\u0924\u0940\u0928=3|\u091A\u093E\u0930=4|"
            + "\u092A\u093E\u0902\u091A=5|\u092A\u093E\u0901\u091A=5|\u091B\u0939=6|\u091B\u0903=6|"
            + "\u0938\u093E\u0924=7|\u0906\u0920=8|\u0928\u094C=9|\u0926\u0938=10|"
            + "\u0917\u094D\u092F\u093E\u0930\u0939=11|\u092C\u093E\u0930\u0939=12|"
            + "\u092A\u0902\u0926\u094D\u0930\u0939=15|\u092C\u0940\u0938=20|\u092A\u091A\u094D\u091A\u0940\u0938=25|"
            + "\u0924\u0940\u0938=30|\u091A\u093E\u0932\u0940\u0938=40|"
            + "\u092A\u0948\u0902\u0924\u093E\u0932\u0940\u0938=45|\u092A\u091A\u093E\u0938=50|\u0938\u093E\u0920=60|"
            + "\u0924\u0941\u092E\u094D\u0939\u093E\u0930\u093E=tumhara";

    static {
        load(C, "\u0915k \u0916kh \u0917g \u0918gh \u0919n \u091Ach \u091Bchh \u091Cj \u091Djh \u091En \u091Ft \u0920th \u0921d \u0922dh \u0923n \u0924t \u0925th \u0926d \u0927dh \u0928n \u092Ap \u092Bph \u092Cb \u092Dbh \u092Em \u092Fy \u0930r \u0932l \u0935v \u0936sh \u0937sh \u0938s \u0939h \u0933l");
        load(V, "\u0905a \u0906a \u0907i \u0908ee \u0909u \u090Aoo \u090Bri \u090Fe \u0910ai \u0913o \u0914au \u0911o \u090De");
        load(M, "\u093Ea \u093Fi \u0940ee \u0941u \u0942oo \u0943ri \u0947e \u0948ai \u094Bo \u094Cau \u0949o \u0945e");
        load(NUKTA, "\u091Cz \u092Bf \u0921r \u0922rh");
        for (String e : RAW.split("\\|")) {
            int k = e.indexOf('=');
            if (k > 0) WORDS.put(Normalizer.normalize(e.substring(0, k), Normalizer.Form.NFC), e.substring(k + 1));
        }
    }

    private static void load(Map<Character, String> m, String src) {
        for (String t : src.split(" ")) {
            if (t.length() > 1) m.put(t.charAt(0), t.substring(1));
        }
    }

    private static String strip(String s) {
        return s.replaceAll("^[\u0964,.?!:;\"']+|[\u0964,.?!:;\"']+$", "");
    }

    static String toLatin(String text) {
        if (!DEVA.matcher(text).find()) return text;
        String[] toks = Normalizer.normalize(text, Normalizer.Form.NFC).trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < toks.length; i++) {
            String tk = toks[i];
            String w = strip(tk);
            String r;
            if (!DEVA.matcher(w).find()) {
                r = tk;
            } else if (w.equals("\u0926\u094B")) {
                String nxt = i + 1 < toks.length ? strip(toks[i + 1]) : "";
                r = UNITS.contains(nxt) ? "2" : "do";
            } else {
                String v = WORDS.get(w);
                r = v != null ? v : translit(w);
            }
            out.append(r).append(' ');
        }
        return out.toString().trim();
    }

    static String translit(String w) {
        w = Normalizer.normalize(w, Normalizer.Form.NFC);
        StringBuilder out = new StringBuilder();
        int n = w.length();
        int i = 0;
        while (i < n) {
            char ch = w.charAt(i);
            if (C.containsKey(ch)) {
                String c = C.get(ch);
                if (i + 1 < n && w.charAt(i + 1) == (char) 0x093C) {
                    String nk = NUKTA.get(ch);
                    if (nk != null) c = nk;
                    i++;
                }
                char nx = i + 1 < n ? w.charAt(i + 1) : (char) 0;
                if (M.containsKey(nx)) {
                    out.append(c).append(M.get(nx));
                    i += 2;
                } else if (nx == (char) 0x094D) {
                    out.append(c);
                    i += 2;
                } else {
                    out.append(c).append("\0");
                    i++;
                }
            } else if (V.containsKey(ch)) {
                out.append(V.get(ch));
                i++;
            } else if (ch == (char) 0x0902 || ch == (char) 0x0901) {
                out.append('n');
                i++;
            } else if (ch == (char) 0x0903) {
                out.append('h');
                i++;
            } else if (ch >= (char) 0x0966 && ch <= (char) 0x096F) {
                out.append((char) ('0' + (ch - (char) 0x0966)));
                i++;
            } else {
                out.append(ch);
                i++;
            }
        }
        String r = out.toString();
        if (r.endsWith("\0")) r = r.substring(0, r.length() - 1);
        return r.replace("\0", "a");
    }
}
