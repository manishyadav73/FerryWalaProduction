package backend.concierge;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reply text catalogue: English, Hindi, Tamil. English is the fallback. Placeholders are {0}, {1}...
 * Hindi and Tamil should be reviewed by native speakers before launch (same note as the app UI).
 */
public final class Messages {

    private static final Map<String, String[]> M = new HashMap<>();
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d)\\}");

    private static void put(String key, String en, String hi, String ta) {
        M.put(key, new String[]{en, hi, ta});
    }

    static {
        put("greet.customer",
                "Hi {0}! I'm the FerryWala AI Concierge, an automated assistant. I can help with your requests, meeting the vendor, delays and problems. What do you need?",
                "नमस्ते {0}! मैं FerryWala AI सहायक हूँ, एक स्वचालित असिस्टेंट। मैं आपके अनुरोध, विक्रेता से मिलने, देरी और समस्याओं में मदद कर सकता हूँ। आपको क्या चाहिए?",
                "வணக்கம் {0}! நான் FerryWala AI உதவியாளர், ஒரு தானியங்கி உதவியாளர். உங்கள் கோரிக்கைகள், விற்பனையாளரைச் சந்திப்பது, தாமதங்கள் மற்றும் சிக்கல்களில் உதவ முடியும். உங்களுக்கு என்ன வேண்டும்?");
        put("greet.vendor",
                "Hi {0}! I'm the FerryWala AI Concierge, an automated assistant. I can help with customer requests, meeting customers, delays and problems. What do you need?",
                "नमस्ते {0}! मैं FerryWala AI सहायक हूँ, एक स्वचालित असिस्टेंट। मैं ग्राहकों के अनुरोध, ग्राहकों से मिलने, देरी और समस्याओं में मदद कर सकता हूँ। आपको क्या चाहिए?",
                "வணக்கம் {0}! நான் FerryWala AI உதவியாளர், ஒரு தானியங்கி உதவியாளர். வாடிக்கையாளர் கோரிக்கைகள், அவர்களைச் சந்திப்பது, தாமதங்கள் மற்றும் சிக்கல்களில் உதவ முடியும். உங்களுக்கு என்ன வேண்டும்?");
        put("noOrder",
                "I can't see a request to work with right now. Open a request from your list and ask me again, or tell me what you need.",
                "अभी मुझे कोई अनुरोध नहीं दिख रहा। अपनी सूची से कोई अनुरोध खोलकर दोबारा पूछें, या बताएँ कि आपको क्या चाहिए।",
                "இப்போது பார்க்க எந்தக் கோரிக்கையும் இல்லை. உங்கள் பட்டியலிலிருந்து ஒரு கோரிக்கையைத் திறந்து மீண்டும் கேளுங்கள், அல்லது உங்களுக்கு என்ன வேண்டும் என்று சொல்லுங்கள்.");
        put("status.line",
                "Request #{0} with {1} is {2}.",
                "{1} के साथ अनुरोध #{0} की स्थिति: {2}।",
                "{1} உடனான கோரிக்கை #{0}: {2}.");
        put("st.PENDING", "waiting for the vendor to respond", "विक्रेता के जवाब का इंतज़ार", "விற்பனையாளரின் பதிலுக்காகக் காத்திருக்கிறது");
        put("st.ACCEPTED", "accepted", "स्वीकार किया गया", "ஏற்றுக்கொள்ளப்பட்டது");
        put("st.ON_THE_WAY", "on the way", "रास्ते में", "வழியில் உள்ளது");
        put("st.COMPLETED", "completed", "पूरा हुआ", "முடிந்தது");
        put("st.REJECTED", "declined by the vendor", "विक्रेता ने मना किया", "விற்பனையாளர் மறுத்தார்");
        put("st.CANCELLED", "cancelled", "रद्द किया गया", "ரத்து செய்யப்பட்டது");
        put("vendor.counts",
                "You have {0} new and {1} open requests.",
                "आपके {0} नए और {1} खुले अनुरोध हैं।",
                "உங்களுக்கு {0} புதிய மற்றும் {1} திறந்த கோரிக்கைகள் உள்ளன.");
        put("meeting.info",
                "The meeting point for request #{0} is {1}. You can open it on the map from the meeting page.",
                "अनुरोध #{0} के लिए मिलने की जगह {1} है। आप मीटिंग पेज से इसे नक्शे पर खोल सकते हैं।",
                "கோரிக்கை #{0} க்கான சந்திப்பு இடம் {1}. சந்திப்புப் பக்கத்திலிருந்து வரைபடத்தில் திறக்கலாம்.");
        put("meeting.areaUnknown", "the spot pinned on the map", "नक्शे पर चिह्नित जगह", "வரைபடத்தில் குறிக்கப்பட்ட இடம்");
        put("meeting.closed",
                "Meeting tools (arrival updates and location sharing) open once the vendor accepts the request. Right now it is {0}. You can still send messages.",
                "मीटिंग टूल (आने की जानकारी और लोकेशन साझा करना) विक्रेता के अनुरोध स्वीकार करने के बाद खुलते हैं। अभी स्थिति: {0}। आप संदेश भेज सकते हैं।",
                "சந்திப்பு கருவிகள் (வருகை தகவல் மற்றும் இருப்பிடப் பகிர்வு) விற்பனையாளர் கோரிக்கையை ஏற்ற பிறகு திறக்கும். இப்போதைய நிலை: {0}. நீங்கள் செய்தி அனுப்பலாம்.");
        put("meeting.help",
                "You can send {0} an arrival update, message them, or start time-limited location sharing. Location is shared only if you choose it and you can stop at any time.",
                "आप {0} को आने की जानकारी भेज सकते हैं, संदेश लिख सकते हैं, या सीमित समय के लिए लोकेशन साझा कर सकते हैं। लोकेशन तभी साझा होती है जब आप चुनें, और आप कभी भी रोक सकते हैं।",
                "நீங்கள் {0} க்கு வருகை தகவல் அனுப்பலாம், செய்தி அனுப்பலாம், அல்லது குறிப்பிட்ட நேரத்திற்கு இருப்பிடத்தைப் பகிரலாம். நீங்கள் தேர்ந்தெடுத்தால் மட்டுமே இருப்பிடம் பகிரப்படும்; எப்போது வேண்டுமானாலும் நிறுத்தலாம்.");
        put("late.ask",
                "I can tell {0} you're running late. Choose how long below. Nothing is sent until you confirm.",
                "मैं {0} को बता सकता हूँ कि आपको देर हो रही है। नीचे समय चुनें। आपके पुष्टि करने तक कुछ नहीं भेजा जाएगा।",
                "நீங்கள் தாமதமாகிறீர்கள் என்று {0} க்கு நான் தெரிவிக்கலாம். கீழே நேரத்தைத் தேர்ந்தெடுங்கள். நீங்கள் உறுதிப்படுத்தும் வரை எதுவும் அனுப்பப்படாது.");
        put("cant.find",
                "Let's get you to the meeting point. 1) Open the saved meeting point and directions. 2) If you're both comfortable, share your location for a short time. 3) Message {0}. 4) If none of that works, I can open a support case for a human to help.",
                "चलिए मिलने की जगह तक पहुँचते हैं। 1) सहेजी गई जगह और रास्ता खोलें। 2) दोनों सहज हों तो थोड़े समय के लिए लोकेशन साझा करें। 3) {0} को संदेश भेजें। 4) इनसे बात न बने तो मैं मदद के लिए सपोर्ट केस खोल सकता हूँ।",
                "சந்திப்பு இடத்தை அடைவோம். 1) சேமித்த சந்திப்பு இடத்தையும் வழியையும் திறக்கவும். 2) இருவரும் விரும்பினால் குறுகிய நேரம் இருப்பிடத்தைப் பகிரவும். 3) {0} க்கு செய்தி அனுப்பவும். 4) எதுவும் பயனளிக்கவில்லை என்றால் மனிதரின் உதவிக்காக ஆதரவு வழக்கு திறக்கலாம்.");
        put("cant.dist",
                "Based on locations you both chose to share, you are about {0} m apart in a straight line (not a walking route).",
                "आप दोनों ने जो लोकेशन साझा की है उसके अनुसार आप सीधी रेखा में लगभग {0} मीटर दूर हैं (पैदल रास्ता नहीं)।",
                "நீங்கள் இருவரும் பகிர்ந்த இருப்பிடங்களின்படி நேர்கோட்டில் சுமார் {0} மீ தூரத்தில் உள்ளீர்கள் (நடைப் பாதை அல்ல).");
        put("cant.noshare",
                "I can't verify where {0} is, because they are not sharing their location.",
                "{0} लोकेशन साझा नहीं कर रहे हैं, इसलिए मैं उनकी जगह की पुष्टि नहीं कर सकता।",
                "{0} இருப்பிடத்தைப் பகிரவில்லை, அதனால் அவர் எங்கே இருக்கிறார் என்பதை என்னால் உறுதிப்படுத்த முடியாது.");
        put("noresp",
                "I can't tell whether {0} has seen your messages or can be reached. Send a message or an update, and if there's still no response I can open a support case.",
                "मैं नहीं बता सकता कि {0} ने आपके संदेश देखे या वे उपलब्ध हैं। संदेश या अपडेट भेजें, फिर भी जवाब न मिले तो मैं सपोर्ट केस खोल सकता हूँ।",
                "{0} உங்கள் செய்திகளைப் பார்த்தாரா அல்லது தொடர்பில் உள்ளாரா என்பதை என்னால் சொல்ல முடியாது. செய்தி அல்லது தகவல் அனுப்புங்கள்; இன்னும் பதில் இல்லையெனில் ஆதரவு வழக்கு திறக்கலாம்.");
        put("otherLate.reported",
                "{0} told you they're running late (about {1} min).",
                "{0} ने बताया कि उन्हें देर हो रही है (लगभग {1} मिनट)।",
                "{0} தாமதமாகிறேன் என்று தெரிவித்தார் (சுமார் {1} நிமிடம்).");
        put("otherLate.none",
                "{0} hasn't sent a delay or arrival update. I can't see where they are unless they choose to share their location.",
                "{0} ने देरी या आने की कोई जानकारी नहीं भेजी है। जब तक वे लोकेशन साझा नहीं करते, मैं उनकी जगह नहीं देख सकता।",
                "{0} தாமதம் அல்லது வருகை பற்றிய தகவல் அனுப்பவில்லை. அவர் இருப்பிடத்தைப் பகிராதவரை அவர் எங்கே இருக்கிறார் என்று என்னால் பார்க்க முடியாது.");
        put("otherLate.next",
                "You can message them or ask for an update. If it has been long, I can open a support case.",
                "आप उन्हें संदेश भेज सकते हैं। काफ़ी देर हो गई हो तो मैं सपोर्ट केस खोल सकता हूँ।",
                "நீங்கள் அவருக்கு செய்தி அனுப்பலாம். நீண்ட நேரம் ஆகிவிட்டால் ஆதரவு வழக்கு திறக்கலாம்.");
        put("arrived.ask",
                "I can tell {0} you've arrived. It will only be sent after you confirm.",
                "मैं {0} को बता सकता हूँ कि आप पहुँच गए हैं। आपके पुष्टि करने के बाद ही भेजा जाएगा।",
                "நீங்கள் வந்துவிட்டீர்கள் என்று {0} க்கு நான் தெரிவிக்கலாம். நீங்கள் உறுதிப்படுத்திய பிறகே அனுப்பப்படும்.");
        put("directions",
                "I can open directions to the saved meeting point in your maps app. The route comes from your maps app, not from me.",
                "मैं सहेजी गई मिलने की जगह के लिए आपके मैप ऐप में रास्ता खोल सकता हूँ। रास्ता आपके मैप ऐप से आता है, मुझसे नहीं।",
                "சேமித்த சந்திப்பு இடத்திற்கான வழியை உங்கள் வரைபட செயலியில் திறக்கலாம். வழி உங்கள் வரைபட செயலியிலிருந்து வருகிறது, என்னிடமிருந்து அல்ல.");
        put("cancel.customer",
                "You can cancel a request while it is Pending or Accepted, from My requests. FerryWala does not process payments, so any money is settled directly with the vendor. If you and the vendor can't agree, I can open a support case.",
                "आप अपने अनुरोध में से लंबित या स्वीकार किए गए अनुरोध को रद्द कर सकते हैं। FerryWala भुगतान नहीं लेता, इसलिए पैसे का लेन-देन सीधे विक्रेता से होता है। सहमति न बने तो मैं सपोर्ट केस खोल सकता हूँ।",
                "நிலுவையில் அல்லது ஏற்றுக்கொள்ளப்பட்ட கோரிக்கையை உங்கள் கோரிக்கைகளிலிருந்து ரத்து செய்யலாம். FerryWala பணம் கையாளாது; பணம் நேரடியாக விற்பனையாளருடன் தீர்க்கப்படும். உடன்பாடு ஏற்படவில்லை என்றால் ஆதரவு வழக்கு திறக்கலாம்.");
        put("cancel.vendor",
                "You can cancel an accepted request from Requests. Please message the customer first so they know why. I can't issue refunds. FerryWala does not process payments. If there's a dispute, I can open a support case.",
                "आप अनुरोध पेज से स्वीकार किया हुआ अनुरोध रद्द कर सकते हैं। पहले ग्राहक को कारण बताएँ। मैं रिफंड नहीं कर सकता; FerryWala भुगतान नहीं लेता। विवाद हो तो मैं सपोर्ट केस खोल सकता हूँ।",
                "ஏற்றுக்கொண்ட கோரிக்கையை கோரிக்கைகள் பக்கத்திலிருந்து ரத்து செய்யலாம். முதலில் காரணத்தை வாடிக்கையாளருக்குத் தெரிவியுங்கள். நான் பணத்தைத் திருப்ப முடியாது; FerryWala பணம் கையாளாது. சர்ச்சை இருந்தால் ஆதரவு வழக்கு திறக்கலாம்.");
        put("payment",
                "FerryWala does not take payments in the app: customers pay vendors directly. That means I can't see, confirm or refund any payment. If a payment is disputed, I can open a support case with your details.",
                "FerryWala ऐप में भुगतान नहीं लेता: ग्राहक सीधे विक्रेता को भुगतान करते हैं। इसलिए मैं कोई भुगतान देख, पुष्टि या वापस नहीं कर सकता। भुगतान पर विवाद हो तो मैं सपोर्ट केस खोल सकता हूँ।",
                "FerryWala செயலியில் பணம் பெறுவதில்லை: வாடிக்கையாளர்கள் நேரடியாக விற்பனையாளருக்குப் பணம் செலுத்துகிறார்கள். அதனால் எந்தக் கட்டணத்தையும் என்னால் பார்க்க, உறுதிப்படுத்த அல்லது திருப்ப முடியாது. கட்டணத்தில் சர்ச்சை இருந்தால் ஆதரவு வழக்கு திறக்கலாம்.");
        put("mismatch",
                "I'm sorry the request didn't match. I can't see what was actually handed over, so I can't decide who is right. Message {0} first. If you can't resolve it, I can open a support case.",
                "खेद है कि अनुरोध के अनुसार नहीं हुआ। असल में क्या दिया गया, यह मैं नहीं देख सकता, इसलिए सही-गलत तय नहीं कर सकता। पहले {0} को संदेश भेजें। हल न हो तो मैं सपोर्ट केस खोल सकता हूँ।",
                "கோரிக்கை பொருந்தவில்லை என்பதற்கு வருந்துகிறேன். உண்மையில் என்ன வழங்கப்பட்டது என்பதை என்னால் பார்க்க முடியாது, அதனால் யார் சரி என்று தீர்மானிக்க முடியாது. முதலில் {0} க்கு செய்தி அனுப்புங்கள். தீரவில்லை என்றால் ஆதரவு வழக்கு திறக்கலாம்.");
        put("access",
                "Thanks for telling me. I can pass your need for special assistance to {0} in a message and open a support case so a person on the FerryWala team can follow up. I can't promise a specific accommodation.",
                "बताने के लिए धन्यवाद। मैं आपकी विशेष सहायता की ज़रूरत {0} को संदेश में बता सकता हूँ और सपोर्ट केस खोल सकता हूँ ताकि FerryWala टीम का कोई व्यक्ति संपर्क करे। मैं किसी विशेष व्यवस्था का वादा नहीं कर सकता।",
                "சொன்னதற்கு நன்றி. உங்கள் சிறப்பு உதவித் தேவையை {0} க்கு செய்தியாக அனுப்பலாம்; FerryWala குழுவின் ஒருவர் தொடர்புகொள்ள ஆதரவு வழக்கும் திறக்கலாம். குறிப்பிட்ட ஏற்பாட்டை என்னால் உறுதியளிக்க முடியாது.");
        put("app",
                "Sorry the app isn't working well. Try reloading the page first. If it keeps happening, I can open a support case describing the problem.",
                "खेद है कि ऐप ठीक से काम नहीं कर रहा। पहले पेज दोबारा लोड करें। फिर भी समस्या रहे तो मैं समस्या के साथ सपोर्ट केस खोल सकता हूँ।",
                "செயலி சரியாக வேலை செய்யவில்லை என்பதற்கு வருந்துகிறேன். முதலில் பக்கத்தை மீண்டும் ஏற்றிப் பாருங்கள். தொடர்ந்தால் சிக்கலை விவரித்து ஆதரவு வழக்கு திறக்கலாம்.");
        put("human",
                "I'm an automated assistant. I can open a support case so a person on the FerryWala team can review it. Please confirm below.",
                "मैं एक स्वचालित असिस्टेंट हूँ। मैं सपोर्ट केस खोल सकता हूँ ताकि FerryWala टीम का कोई व्यक्ति इसे देखे। कृपया नीचे पुष्टि करें।",
                "நான் ஒரு தானியங்கி உதவியாளர். FerryWala குழுவின் ஒருவர் பார்க்க ஆதரவு வழக்கு திறக்கலாம். கீழே உறுதிப்படுத்துங்கள்.");
        put("human.contact", "You can also reach FerryWala support at {0}.", "आप FerryWala सपोर्ट से यहाँ भी संपर्क कर सकते हैं: {0}।", "FerryWala ஆதரவை இங்கும் தொடர்பு கொள்ளலாம்: {0}.");
        put("draft",
                "Here is a draft message. It will be sent as YOUR message, not mine, and you can edit it first.",
                "यह एक संदेश का मसौदा है। यह आपके नाम से जाएगा, मेरे नहीं, और आप इसे पहले बदल सकते हैं।",
                "இது ஒரு செய்தி வரைவு. இது உங்கள் செய்தியாகவே அனுப்பப்படும், என்னுடையதாக அல்ல; முதலில் மாற்றலாம்.");
        put("draft.customer",
                "Hello {0}, this is {1}. I'm waiting at the meeting point for request #{2}. Could you tell me roughly when you'll arrive? Thank you.",
                "नमस्ते {0}, मैं {1} हूँ। मैं अनुरोध #{2} के लिए मिलने की जगह पर इंतज़ार कर रहा/रही हूँ। क्या आप बता सकते हैं कि लगभग कब पहुँचेंगे? धन्यवाद।",
                "வணக்கம் {0}, நான் {1}. கோரிக்கை #{2} க்கான சந்திப்பு இடத்தில் காத்திருக்கிறேன். நீங்கள் எப்போது வருவீர்கள் என்று சொல்ல முடியுமா? நன்றி.");
        put("draft.vendor",
                "Hello {0}, this is {1} from your request #{2}. I'm on my way to the meeting point. Please let me know if you'd like me to meet somewhere else nearby. Thank you.",
                "नमस्ते {0}, मैं {1} हूँ, आपके अनुरोध #{2} के लिए। मैं मिलने की जगह की ओर आ रहा हूँ। कहीं आस-पास और मिलना चाहें तो बताइए। धन्यवाद।",
                "வணக்கம் {0}, நான் {1}, உங்கள் கோரிக்கை #{2} தொடர்பாக. சந்திப்பு இடத்தை நோக்கி வருகிறேன். அருகில் வேறு இடத்தில் சந்திக்க விரும்பினால் சொல்லுங்கள். நன்றி.");
        put("draft.access",
                "Hello {0}, this is {1}. For request #{2} I may need some extra assistance. Could you let me know how we can make the meeting easy? Thank you.",
                "नमस्ते {0}, मैं {1} हूँ। अनुरोध #{2} के लिए मुझे थोड़ी अतिरिक्त सहायता की ज़रूरत हो सकती है। मिलना आसान बनाने के लिए क्या करें, बताइए। धन्यवाद।",
                "வணக்கம் {0}, நான் {1}. கோரிக்கை #{2} க்கு எனக்குக் கூடுதல் உதவி தேவைப்படலாம். சந்திப்பை எளிதாக்க என்ன செய்யலாம் என்று சொல்லுங்கள். நன்றி.");
        put("noDraftOrder",
                "I need a request in context to draft a message. Open a request, then ask me again.",
                "संदेश का मसौदा बनाने के लिए एक अनुरोध चाहिए। कोई अनुरोध खोलकर दोबारा पूछें।",
                "செய்தி வரைவு எழுத ஒரு கோரிக்கை தேவை. ஒரு கோரிக்கையைத் திறந்து மீண்டும் கேளுங்கள்.");
        put("findVendor.customer",
                "I can take you to nearby vendors. Availability and prices are shown on each vendor's page, which is the live source.",
                "मैं आपको पास के विक्रेताओं तक ले जा सकता हूँ। उपलब्धता और कीमतें हर विक्रेता के पेज पर लाइव दिखती हैं।",
                "அருகிலுள்ள விற்பனையாளர்களுக்கு உங்களை அழைத்துச் செல்லலாம். கிடைப்பும் விலையும் ஒவ்வொரு விற்பனையாளர் பக்கத்திலும் நேரடியாகக் காட்டப்படும்.");
        put("findVendor.vendor",
                "Customers find you from the vendor list when your stall is online with a location. Use the Today screen to see where demand is.",
                "आपका स्टॉल लोकेशन के साथ ऑनलाइन हो तो ग्राहक आपको विक्रेता सूची में ढूँढते हैं। माँग कहाँ है यह देखने के लिए आज वाला स्क्रीन देखें।",
                "உங்கள் கடை இருப்பிடத்துடன் ஆன்லைனில் இருக்கும்போது வாடிக்கையாளர்கள் விற்பனையாளர் பட்டியலில் உங்களைக் காண்பார்கள். தேவை எங்கே என்று பார்க்க இன்று திரையைப் பயன்படுத்துங்கள்.");
        put("unknown",
                "I'm not sure I understood. I can help with: request status, the meeting point, running late, not finding each other, cancellations and payments, or opening a support case. Which one is closest?",
                "मुझे पूरी तरह समझ नहीं आया। मैं इनमें मदद कर सकता हूँ: अनुरोध की स्थिति, मिलने की जगह, देर होना, एक-दूसरे को न ढूँढ पाना, रद्द करना और भुगतान, या सपोर्ट केस खोलना। इनमें से कौन सा सबसे पास है?",
                "எனக்கு முழுமையாகப் புரியவில்லை. இவற்றில் உதவ முடியும்: கோரிக்கை நிலை, சந்திப்பு இடம், தாமதம், ஒருவரை ஒருவர் கண்டுபிடிக்க முடியாதது, ரத்து மற்றும் கட்டணம், அல்லது ஆதரவு வழக்கு. எது நெருக்கமாக உள்ளது?");
        // action labels
        put("act.late", "Tell {0}: about {1} min late", "{0} को बताएँ: लगभग {1} मिनट देर", "{0} க்குத் தெரிவி: சுமார் {1} நிமிடம் தாமதம்");
        put("act.arrived", "Tell {0} I've arrived", "{0} को बताएँ कि मैं पहुँच गया/गई", "{0} க்குத் தெரிவி: நான் வந்துவிட்டேன்");
        put("act.cant", "Tell {0} I can't find the spot", "{0} को बताएँ कि जगह नहीं मिल रही", "{0} க்குத் தெரிவி: இடம் கிடைக்கவில்லை");
        put("act.directions", "Open directions", "रास्ता खोलें", "வழியைத் திறக்கவும்");
        put("act.share", "Share my location for 15 min", "15 मिनट के लिए लोकेशन साझा करें", "15 நிமிடம் இருப்பிடத்தைப் பகிரவும்");
        put("act.case", "Open a support case", "सपोर्ट केस खोलें", "ஆதரவு வழக்கைத் திறக்கவும்");
        put("act.message", "Review message to {0}", "{0} को संदेश देखें", "{0} க்கான செய்தியைப் பார்க்கவும்");
        put("act.vendors", "Find vendors nearby", "पास के विक्रेता खोजें", "அருகிலுள்ள விற்பனையாளர்களைத் தேடு");
        put("act.meeting", "Open meeting page", "मीटिंग पेज खोलें", "சந்திப்புப் பக்கத்தைத் திறக்கவும்");
        // card text
        put("card.order", "Request #{0}", "अनुरोध #{0}", "கோரிக்கை #{0}");
        put("card.meeting", "Meeting", "मिलना", "சந்திப்பு");
        put("card.status", "Status: {0}", "स्थिति: {0}", "நிலை: {0}");
        put("card.with", "With: {0}", "साथ: {0}", "உடன்: {0}");
        put("card.point", "Meeting point: {0}", "मिलने की जगह: {0}", "சந்திப்பு இடம்: {0}");
        put("card.share", "Location sharing: you {0}, {1} {2}", "लोकेशन साझा: आप {0}, {1} {2}", "இருப்பிடப் பகிர்வு: நீங்கள் {0}, {1} {2}");
        put("card.on", "on", "चालू", "இயக்கத்தில்");
        put("card.off", "off", "बंद", "நிறுத்தத்தில்");
        put("card.dist", "Straight-line distance from shared locations: about {0} m", "साझा लोकेशन से सीधी दूरी: लगभग {0} मीटर", "பகிர்ந்த இருப்பிடங்களின் நேர்கோட்டு தூரம்: சுமார் {0} மீ");
    }

    private Messages() {
    }

    public static String lang(String code) {
        if (code == null) {
            return "en";
        }
        String c = code.trim().toLowerCase();
        return c.equals("hi") || c.equals("ta") ? c : "en";
    }

    public static String t(String lang, String key, Object... args) {
        String[] row = M.get(key);
        if (row == null) {
            return key;
        }
        int idx = "hi".equals(lang) ? 1 : "ta".equals(lang) ? 2 : 0;
        String s = row[idx] != null && !row[idx].isEmpty() ? row[idx] : row[0];
        // Single pass, so a value that itself contains "{1}" is never substituted again.
        Matcher m = PLACEHOLDER.matcher(s);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            int i = Integer.parseInt(m.group(1));
            String rep = i < args.length ? String.valueOf(args[i]) : m.group();
            m.appendReplacement(out, Matcher.quoteReplacement(rep));
        }
        m.appendTail(out);
        return out.toString();
    }

    static boolean has(String key) {
        return M.containsKey(key);
    }
}
