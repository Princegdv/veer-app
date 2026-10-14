package com.veer.assistant.lang;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class Hindi {
    private static final Pattern DEVA = Pattern.compile("[\\u0900-\\u097F]");
    private static final Map<Character, String> C = new HashMap<>();
    private static final Map<Character, String> V = new HashMap<>();
    private static final Map<Character, String> M = new HashMap<>();
    private static final Map<String, String> WORDS = new HashMap<>();

    private static final String[] WORD_MAP = {
            "वीर=veer","बीर=veer","वीयर=veer","विर=veer","हेलो=hello","हैलो=hello",
            "नमस्ते=namaste","बाय=bye","मदद=madad","हेल्प=help","नाम=naam","बदलो=badlo",
            "अगला=agla","अगली=agli","अगले=agle","पिछला=pichla","पिछली=pichli","पिछले=pichle",
            "लाइक=like","बैक=back","वापस=wapas","होम=home","नीचे=neeche","ऊपर=upar",
            "स्क्रॉल=scroll","स्क्रीन=screen","लॉक=lock","बंद=band","स्क्रीनशॉट=screenshot",
            "नोटिफिकेशन=notification","रिसेंट=recent","मल्टीटास्क=multitask",
            "कॉल=call","फोन=phone","एसएमएस=sms","मैसेज=message","बैटरी=battery",
            "चार्ज=charge","टाइम=time","समय=samay","तारीख=tarikh","डेट=date",
            "जोक=joke","चुटकुला=chutkula","खोलो=kholo","खोल=khol","चलाओ=chalao",
            "चालू=chalu","यूट्यूब=youtube","व्हाट्सएप=whatsapp","इंस्टाग्राम=instagram",
            "क्रोम=chrome","कैमरा=camera","गैलरी=gallery","सेटिंग=settings",
            "करो=karo","कर=kar","जाओ=jao","कहो=kaho","बोलो=bolo","किसने=kisne",
            "कौन=kaun","कितना=kitna","कितने=kitne","भाव=bhav","दाम=dam","रेट=rate",
            "बताओ=batao","दिखाओ=dikhao","पोर्टफोलियो=portfolio","मेरा=mera","मेरे=mere"
    };

    static {
        load(C, "कk खkh गg घgh ङn चch छchh जj झjh ञn टt ठth डd ढdh णn तt थth दd धdh नn पp फph बb भbh मm यy रr लl वv शsh षsh सs हh");
        load(V, "अa आa इi ईee उu ऊoo ऋri एe ऐai ओo औau");
        load(M, "ाa िi ीee ुu ूoo ृri ेe ैai ोo ौau");
        for (String e : WORD_MAP) {
            int k = e.indexOf('=');
            if (k > 0) WORDS.put(Normalizer.normalize(e.substring(0, k), Normalizer.Form.NFC), e.substring(k + 1));
        }
    }

    private static void load(Map<Character, String> m, String src) {
        for (String t : src.split(" ")) if (t.length() > 1) m.put(t.charAt(0), t.substring(1));
    }

    public static String toLatin(String text) {
        if (text == null) return "";
        if (!DEVA.matcher(text).find()) return text;
        StringBuilder out = new StringBuilder();
        for (String tk : text.trim().split("\\s+")) {
            String clean = tk.replaceAll("[,।?!:;\"']+$", "");
            String v = WORDS.get(Normalizer.normalize(clean, Normalizer.Form.NFC));
            out.append(v != null ? v : translit(clean)).append(' ');
        }
        return out.toString().trim();
    }

    private static String translit(String w) {
        w = Normalizer.normalize(w, Normalizer.Form.NFC);
        StringBuilder out = new StringBuilder();
        int i = 0, n = w.length();
        while (i < n) {
            char ch = w.charAt(i);
            if (C.containsKey(ch)) {
                String c = C.get(ch);
                char nx = i + 1 < n ? w.charAt(i + 1) : 0;
                if (M.containsKey(nx)) { out.append(c).append(M.get(nx)); i += 2; }
                else if (nx == 0x094D) { out.append(c); i += 2; }
                else { out.append(c).append("\0"); i++; }
            } else if (V.containsKey(ch)) { out.append(V.get(ch)); i++; }
            else if (ch == 0x0902 || ch == 0x0901) { out.append('n'); i++; }
            else { out.append(ch); i++; }
        }
        String r = out.toString();
        if (r.endsWith("\0")) r = r.substring(0, r.length() - 1);
        return r.replace("\0", "a");
    }
}
