import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

// "AI" that reads a complaint and suggests its category and priority.
//
// It works in two layers:
//  1. Keyword rules (English + Hinglish), e.g. "paani", "wifi", "khana"
//  2. A machine-learning model: TF-IDF features + Logistic Regression
//     (softmax), written in plain Java. It is trained on built-in example
//     complaints plus every complaint already saved in the database, so it
//     gets smarter as admins correct categories.
public final class SmartClassifier {

    // ================= RESULT =================

    public static class Prediction {
        public final String category;
        public final int confidence;        // 0 - 100
        public final String priority;
        public final String method;         // "ML model", "Keywords", ...
        public final List<String> reasons;  // words that triggered it

        Prediction(String category, int confidence, String priority,
                   String method, List<String> reasons) {
            this.category = category;
            this.confidence = confidence;
            this.priority = priority;
            this.method = method;
            this.reasons = reasons;
        }
    }

    // ================= KEYWORD RULES =================

    private static final Map<String, String[]> CATEGORY_KEYWORDS = new LinkedHashMap<>();

    static {
        CATEGORY_KEYWORDS.put("Electrical", new String[]{
            "light", "lights", "bulb", "tubelight", "tube light", "fan", "switch",
            "socket", "plug", "wire", "wiring", "electricity", "electric",
            "power", "current", "spark", "short circuit", "bijli", "mcb", "fuse",
            "ac", "cooler", "geyser", "inverter", "voltage", "switchboard", "charging point"
        });
        CATEGORY_KEYWORDS.put("Water", new String[]{
            "water", "paani", "pani", "tap", "nal", "leak", "leakage", "leaking",
            "pipe", "drain", "flush", "tank", "motor", "purifier", "ro",
            "drinking", "dripping", "tapak", "tapak raha", "seepage", "overflow", "shower", "bucket", "geyser water"
        });
        CATEGORY_KEYWORDS.put("Cleaning", new String[]{
            "clean", "cleaning", "dirty", "garbage", "kachra", "dustbin", "sweep",
            "safai", "smell", "badbu", "mosquito", "machhar", "cockroach", "pest",
            "rats", "chuhe", "stain", "mop", "dust", "trash", "washroom dirty", "unhygienic"
        });
        CATEGORY_KEYWORDS.put("Hostel", new String[]{
            "hostel", "warden", "roommate", "curfew", "laundry", "key", "lock",
            "door", "window", "noise", "allotment", "gate", "mattress", "room change",
            "visitor", "attendance", "late entry"
        });
        CATEGORY_KEYWORDS.put("Internet", new String[]{
            "wifi", "wi-fi", "internet", "network", "lan", "router", "net",
            "connection", "signal", "bandwidth", "speed", "portal", "ethernet",
            "hotspot", "disconnect", "buffering"
        });
        CATEGORY_KEYWORDS.put("Furniture", new String[]{
            "chair", "table", "desk", "bench", "cupboard", "almirah", "shelf",
            "drawer", "furniture", "stool", "rack", "bed frame", "bed broken",
            "wardrobe", "sofa", "kursi"
        });
        CATEGORY_KEYWORDS.put("Mess", new String[]{
            "mess", "food", "khana", "meal", "breakfast", "lunch", "dinner",
            "roti", "dal", "rice", "chawal", "canteen", "taste", "stale", "oily",
            "menu", "cook", "plate", "sabzi", "tiffin", "undercooked", "food poisoning",
            "kitchen", "keeda", "insect in food"
        });
    }

    private static final String[] URGENT_WORDS = {
        "fire", "aag", "smoke", "dhuan", "spark", "sparking", "short circuit",
        "shock", "current lag", "electrocut", "gas leak", "flood", "injury",
        "injured", "blood", "emergency", "unsafe", "danger", "dangerous",
        "collapse", "ceiling fall", "snake", "saanp", "faint", "unconscious"
    };

    private static final String[] HIGH_WORDS = {
        "leak", "leakage", "leaking", "tapak", "no water", "paani nahi", "pani nahi",
        "power cut", "bijli nahi", "no electricity", "no power", "overflow",
        "food poisoning", "keeda", "insect", "sick", "ill", "vomit", "theft", "chori", "security",
        "broken lock", "urgent", "jaldi", "asap", "since 3 days", "since many days",
        "kai dino", "not working"
    };

    private static final String[] LOW_WORDS = {
        "suggestion", "request", "minor", "small", "sometimes", "kabhi kabhi",
        "would like", "please add", "kindly add", "improve", "feedback", "thoda"
    };

    // ================= BUILT-IN TRAINING EXAMPLES =================

    private static final String[][] SEED = {
        {"Fan not working in my room", "Electrical"},
        {"Tube light is flickering since yesterday", "Electrical"},
        {"Switch board sparking near bed", "Electrical"},
        {"Room ki light nahi jal rahi", "Electrical"},
        {"Bijli nahi aa rahi hostel mein", "Electrical"},
        {"Power socket not working cannot charge laptop", "Electrical"},
        {"AC is not cooling at all", "Electrical"},
        {"Current lag raha hai switch chhune par", "Electrical"},
        {"Geyser not heating in bathroom", "Electrical"},
        {"Fan bahut awaaz kar raha hai", "Electrical"},
        {"Bulb fused in corridor", "Electrical"},
        {"Frequent power cut in block B", "Electrical"},

        {"No water in washroom since morning", "Water"},
        {"Paani nahi aa raha hai bathroom mein", "Water"},
        {"Tap is leaking continuously", "Water"},
        {"Water leakage from the ceiling", "Water"},
        {"Drinking water purifier not working", "Water"},
        {"Nal se paani tapak raha hai", "Water"},
        {"Pipe burst in second floor washroom", "Water"},
        {"Toilet flush is not working", "Water"},
        {"Drain is blocked water overflowing", "Water"},
        {"RO water is dirty and smells", "Water"},
        {"Water tank overflow on terrace", "Water"},
        {"Shower has no water pressure", "Water"},

        {"Washroom is very dirty not cleaned", "Cleaning"},
        {"Garbage not collected for two days", "Cleaning"},
        {"Corridor mein kachra pada hai", "Cleaning"},
        {"Room ki safai nahi hui", "Cleaning"},
        {"Bad smell coming from dustbin", "Cleaning"},
        {"Too many mosquitoes in the room", "Cleaning"},
        {"Cockroaches in the bathroom", "Cleaning"},
        {"Dustbin is full and overflowing", "Cleaning"},
        {"Stairs are dirty and dusty", "Cleaning"},
        {"Chuhe ghoom rahe hain hostel mein", "Cleaning"},

        {"Room door lock is broken", "Hostel"},
        {"Window glass broken in my room", "Hostel"},
        {"Need room change roommate issue", "Hostel"},
        {"Warden not available at night", "Hostel"},
        {"Laundry service not collecting clothes", "Hostel"},
        {"Lost my room key need duplicate", "Hostel"},
        {"Too much noise at night in hostel", "Hostel"},
        {"Hostel gate closes too early", "Hostel"},
        {"Mattress is torn and uncomfortable", "Hostel"},
        {"Room ka darwaza band nahi hota", "Hostel"},

        {"WiFi is very slow in the evening", "Internet"},
        {"Internet not working in hostel", "Internet"},
        {"Wifi baar baar disconnect ho raha hai", "Internet"},
        {"LAN port not working in room", "Internet"},
        {"Cannot login to college wifi portal", "Internet"},
        {"No network signal on third floor", "Internet"},
        {"Net bahut slow chal raha hai", "Internet"},
        {"Router is switched off in block C", "Internet"},
        {"Online class buffering due to bad internet", "Internet"},
        {"Wifi password not working", "Internet"},

        {"Chair leg is broken in library", "Furniture"},
        {"Study table is damaged", "Furniture"},
        {"Cupboard door broken cannot lock", "Furniture"},
        {"Bed frame is broken and shaking", "Furniture"},
        {"Bench in classroom is broken", "Furniture"},
        {"Almirah ka handle toot gaya", "Furniture"},
        {"Need a new chair for my room", "Furniture"},
        {"Shelf fell down from the wall", "Furniture"},
        {"Drawer is stuck and not opening", "Furniture"},
        {"Kursi toot gayi hai", "Furniture"},

        {"Food quality in mess is very bad", "Mess"},
        {"Mess ka khana thanda tha", "Mess"},
        {"Found insect in dinner", "Mess"},
        {"Roti is undercooked every day", "Mess"},
        {"Breakfast served late in mess", "Mess"},
        {"Food is too oily and spicy", "Mess"},
        {"Stale rice served at lunch", "Mess"},
        {"Mess plates are not washed properly", "Mess"},
        {"Food poisoning after eating in canteen", "Mess"},
        {"Mess menu same every day please change", "Mess"},
        {"Dal mein paani zyada hai", "Mess"},

        {"Need bus timing information", "Other"},
        {"Library closes too early", "Other"},
        {"Request for more sports equipment", "Other"},
        {"ID card not received yet", "Other"},
        {"Parking space is not enough", "Other"},
        {"Fee receipt not generated", "Other"},
        {"Stray dogs near main gate", "Other"},
        {"Lecture hall projector issue", "Other"}
    };

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "the", "is", "a", "an", "in", "on", "of", "and", "to", "my", "for",
        "at", "it", "this", "that", "are", "was", "be", "with", "from",
        "hai", "hain", "ka", "ki", "ke", "mein", "me", "se", "par", "ko",
        "i", "we", "our", "since", "very", "too", "please", "sir", "maam"
    ));

    // ================= MODEL STATE =================

    private static volatile Model model;
    private static volatile int trainingSize;

    private SmartClassifier() {
    }

    public static int getTrainingSize() {
        return trainingSize;
    }

    // Train using the built-in examples plus the complaints in the database.
    // Called once at startup, and again after an admin corrects a category.
    public static synchronized void train() {

        List<String> texts = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (String[] row : SEED) {
            texts.add(row[0]);
            labels.add(row[1]);
        }

        try (Connection con = DBConnection.getConnection()) {
            if (con != null) {
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT title, description, category FROM complaints");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String category = rs.getString("category");
                        if (indexOf(category) >= 0) {
                            texts.add(rs.getString("title") + " "
                                    + rs.getString("description"));
                            labels.add(category);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Classifier: using built-in examples only ("
                    + e.getMessage() + ")");
        }

        model = Model.fit(texts, labels);
        trainingSize = texts.size();
    }

    public static void trainInBackground() {
        Thread t = new Thread(SmartClassifier::train, "classifier-training");
        t.setDaemon(true);
        t.start();
    }

    // ================= PREDICT =================

    public static Prediction predict(String text) {

        String clean = normalise(text);

        if (clean.isBlank()) {
            return new Prediction("Other", 0, "Medium", "None", List.of());
        }

        // Layer 1: keywords
        Map<String, List<String>> hits = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : CATEGORY_KEYWORDS.entrySet()) {
            for (String word : e.getValue()) {
                if (containsWord(clean, word)) {
                    hits.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(word);
                }
            }
        }

        String keywordCategory = null;
        int bestHits = 0;
        for (Map.Entry<String, List<String>> e : hits.entrySet()) {
            if (e.getValue().size() > bestHits) {
                bestHits = e.getValue().size();
                keywordCategory = e.getKey();
            }
        }

        // Layer 2: ML model
        Model m = model;
        if (m == null) {
            train();
            m = model;
        }

        double[] probs = m.probabilities(clean);
        int best = 0;
        for (int i = 1; i < probs.length; i++) {
            if (probs[i] > probs[best]) {
                best = i;
            }
        }

        String mlCategory = UI.CATEGORIES[best];
        int mlConfidence = (int) Math.round(probs[best] * 100);

        String category;
        int confidence;
        String method;

        if (keywordCategory != null && keywordCategory.equals(mlCategory)) {
            category = mlCategory;
            confidence = Math.min(99, Math.max(mlConfidence, 60) + 15);
            method = "ML model + keywords";
        } else if (keywordCategory != null && mlConfidence <= 50) {
            category = keywordCategory;
            confidence = Math.min(90, 50 + bestHits * 10);
            method = "Keywords";
        } else {
            category = mlCategory;
            confidence = mlConfidence;
            method = "ML model";
        }

        List<String> reasons = new ArrayList<>(
                hits.getOrDefault(category, List.of()));

        String priority = detectPriority(clean, reasons);

        // Remove repeated words
        reasons = new ArrayList<>(new LinkedHashSet<>(reasons));

        return new Prediction(category, confidence, priority, method, reasons);
    }

    // Urgent / High / Low words decide the priority, otherwise Medium
    public static String detectPriority(String text, List<String> reasons) {

        String clean = normalise(text);

        for (String w : URGENT_WORDS) {
            if (containsWord(clean, w)) {
                reasons.add(w);
                return "Urgent";
            }
        }
        for (String w : HIGH_WORDS) {
            if (containsWord(clean, w)) {
                reasons.add(w);
                return "High";
            }
        }
        for (String w : LOW_WORDS) {
            if (containsWord(clean, w)) {
                reasons.add(w);
                return "Low";
            }
        }
        return "Medium";
    }

    // Which team handles each category
    public static String departmentFor(String category) {

        if (category == null) {
            return "Admin Office";
        }

        return switch (category) {
            case "Electrical" -> "Electrical Maintenance";
            case "Water" -> "Plumbing";
            case "Cleaning" -> "Housekeeping";
            case "Hostel" -> "Hostel Warden";
            case "Internet" -> "IT Cell";
            case "Furniture" -> "Carpentry";
            case "Mess" -> "Mess Committee";
            default -> "Admin Office";
        };
    }

    public static final String[] DEPARTMENTS = {
        "Electrical Maintenance", "Plumbing", "Housekeeping", "Hostel Warden",
        "IT Cell", "Carpentry", "Mess Committee", "Admin Office"
    };

    // ================= TEXT HELPERS =================

    private static String normalise(String text) {
        if (text == null) {
            return "";
        }
        return " " + text.toLowerCase().replaceAll("[^a-z0-9\\- ]", " ")
                .replaceAll("\\s+", " ").trim() + " ";
    }

    private static boolean containsWord(String normalisedText, String word) {
        return normalisedText.contains(" " + word + " ");
    }

    private static int indexOf(String category) {
        for (int i = 0; i < UI.CATEGORIES.length; i++) {
            if (UI.CATEGORIES[i].equals(category)) {
                return i;
            }
        }
        return -1;
    }

    // Words and word-pairs (bigrams), e.g. "paani", "nahi", "paani_nahi"
    private static List<String> tokens(String text) {

        List<String> words = new ArrayList<>();
        for (String w : normalise(text).trim().split(" ")) {
            if (w.length() > 1 && !STOP_WORDS.contains(w)) {
                words.add(w);
            }
        }

        List<String> result = new ArrayList<>(words);
        for (int i = 0; i + 1 < words.size(); i++) {
            result.add(words.get(i) + "_" + words.get(i + 1));
        }
        return result;
    }

    // ================= TF-IDF + LOGISTIC REGRESSION =================

    private static final class Model {

        private final Map<String, Integer> vocabulary = new HashMap<>();
        private double[] idf;
        private double[][] weights;   // [class][feature]
        private double[] bias;        // [class]

        static Model fit(List<String> texts, List<String> labels) {

            Model m = new Model();
            int n = texts.size();
            int classes = UI.CATEGORIES.length;

            // Build vocabulary and document frequency
            List<List<String>> docs = new ArrayList<>();
            Map<String, Integer> df = new HashMap<>();

            for (String text : texts) {
                List<String> t = tokens(text);
                docs.add(t);
                for (String token : new HashSet<>(t)) {
                    df.merge(token, 1, Integer::sum);
                }
            }

            for (String token : df.keySet()) {
                m.vocabulary.put(token, m.vocabulary.size());
            }

            m.idf = new double[m.vocabulary.size()];
            for (Map.Entry<String, Integer> e : df.entrySet()) {
                m.idf[m.vocabulary.get(e.getKey())] =
                        Math.log((1.0 + n) / (1.0 + e.getValue())) + 1.0;
            }

            // TF-IDF vectors (sparse)
            List<Map<Integer, Double>> x = new ArrayList<>();
            for (List<String> doc : docs) {
                x.add(m.vectorise(doc));
            }

            int[] y = new int[n];
            for (int i = 0; i < n; i++) {
                y[i] = Math.max(0, indexOf(labels.get(i)));
            }

            // Softmax logistic regression trained with gradient descent
            m.weights = new double[classes][m.vocabulary.size()];
            m.bias = new double[classes];

            double learningRate = 2.0;
            double l2 = 0.0001;
            int epochs = 400;

            for (int epoch = 0; epoch < epochs; epoch++) {

                double[][] gradW = new double[classes][m.vocabulary.size()];
                double[] gradB = new double[classes];

                for (int i = 0; i < n; i++) {
                    double[] p = m.softmax(x.get(i));
                    for (int c = 0; c < classes; c++) {
                        double error = p[c] - (y[i] == c ? 1 : 0);
                        gradB[c] += error;
                        for (Map.Entry<Integer, Double> f : x.get(i).entrySet()) {
                            gradW[c][f.getKey()] += error * f.getValue();
                        }
                    }
                }

                for (int c = 0; c < classes; c++) {
                    m.bias[c] -= learningRate * gradB[c] / n;
                    for (int j = 0; j < m.vocabulary.size(); j++) {
                        m.weights[c][j] -= learningRate
                                * (gradW[c][j] / n + l2 * m.weights[c][j]);
                    }
                }
            }

            return m;
        }

        Map<Integer, Double> vectorise(List<String> doc) {

            Map<Integer, Double> v = new HashMap<>();
            for (String token : doc) {
                Integer index = vocabulary.get(token);
                if (index != null) {
                    v.merge(index, 1.0, Double::sum);
                }
            }

            double norm = 0;
            for (Map.Entry<Integer, Double> e : v.entrySet()) {
                double value = e.getValue() * idf[e.getKey()];
                e.setValue(value);
                norm += value * value;
            }

            norm = Math.sqrt(norm);
            if (norm > 0) {
                for (Map.Entry<Integer, Double> e : v.entrySet()) {
                    e.setValue(e.getValue() / norm);
                }
            }
            return v;
        }

        double[] softmax(Map<Integer, Double> v) {

            double[] scores = new double[bias.length];
            double max = Double.NEGATIVE_INFINITY;

            for (int c = 0; c < bias.length; c++) {
                double s = bias[c];
                for (Map.Entry<Integer, Double> f : v.entrySet()) {
                    s += weights[c][f.getKey()] * f.getValue();
                }
                scores[c] = s;
                max = Math.max(max, s);
            }

            double sum = 0;
            for (int c = 0; c < scores.length; c++) {
                scores[c] = Math.exp(scores[c] - max);
                sum += scores[c];
            }
            for (int c = 0; c < scores.length; c++) {
                scores[c] /= sum;
            }
            return scores;
        }

        double[] probabilities(String text) {
            return softmax(vectorise(tokens(text)));
        }
    }
}
