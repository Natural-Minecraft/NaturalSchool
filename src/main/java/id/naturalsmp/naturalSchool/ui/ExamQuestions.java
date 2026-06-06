package id.naturalsmp.naturalSchool.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExamQuestions {

    public enum QuestionType {
        MULTIPLE_CHOICE,
        TRUE_FALSE,
        COMPLEX_MULTIPLE_CHOICE
    }

    public static class Question {
        private final QuestionType type;
        private final String text;
        private final String[] options; // MCQ: 4 options, T/F: "Benar"/"Salah", Complex: 3 statements
        private final Object correctAnswer; // MCQ: String ("A"/"B"/"C"/"D"), T/F: Boolean, Complex: boolean[] (size 3)

        public Question(QuestionType type, String text, String[] options, Object correctAnswer) {
            this.type = type;
            this.text = text;
            this.options = options;
            this.correctAnswer = correctAnswer;
        }

        public QuestionType getType() {
            return type;
        }

        public String getText() {
            return text;
        }

        public String[] getOptions() {
            return options;
        }

        public Object getCorrectAnswer() {
            return correctAnswer;
        }
    }

    public static class QuestionSet {
        private final List<Question> questions;

        public QuestionSet(List<Question> questions) {
            this.questions = questions;
        }

        public List<Question> getQuestions() {
            return questions;
        }

        public Question getQuestion(int index) {
            if (index < 0 || index >= questions.size()) {
                return null;
            }
            return questions.get(index);
        }
    }

    private static final Map<String, QuestionSet> SUBJECTS = new HashMap<>();

    private static Question mc(String text, String a, String b, String c, String d, String correct) {
        return new Question(QuestionType.MULTIPLE_CHOICE, text, new String[]{a, b, c, d}, correct);
    }

    private static Question tf(String text, boolean correct) {
        return new Question(QuestionType.TRUE_FALSE, text, new String[]{"Benar", "Salah"}, correct);
    }

    private static Question complex(String text, String s1, String s2, String s3, boolean c1, boolean c2, boolean c3) {
        return new Question(QuestionType.COMPLEX_MULTIPLE_CHOICE, text, new String[]{s1, s2, s3}, new boolean[]{c1, c2, c3});
    }

    static {
        // Pengetahuan Umum (pengetahuan_umum)
        List<Question> pu = new ArrayList<>();
        pu.add(mc("Siapa pencipta NaturalSMP?", "Saya", "Jopeh", "AnakTentara", "Gua", "C"));
        pu.add(mc("Di benua manakah negara Indonesia berada?", "Asia", "Eropa", "Afrika", "Amerika", "A"));
        pu.add(mc("Apa nama ibukota Indonesia saat ini?", "Surabaya", "Jakarta", "Bandung", "Medan", "B"));
        pu.add(mc("Gunung tertinggi di Indonesia adalah?", "Gunung Semeru", "Gunung Kerinci", "Gunung Rinjani", "Puncak Jaya", "D"));
        pu.add(mc("Lagu kebangsaan Indonesia adalah?", "Hari Merdeka", "Indonesia Raya", "Bagimu Negeri", "Satu Nusa Satu Bangsa", "B"));
        pu.add(mc("Mata uang resmi Indonesia adalah?", "Ringgit", "Rupiah", "Dollar", "Yen", "B"));
        pu.add(tf("Apakah NaturalSMP didirikan pada tahun 2026?", true));
        pu.add(tf("Apakah bendera Indonesia terdiri dari warna Merah dan Putih?", true));
        pu.add(complex("Manakah dari pernyataan berikut yang benar mengenai NaturalSchool?", "NaturalSchool adalah plugin sekolah", "1 Semester = 14 hari real-life", "Grade pemain naik otomatis tanpa ujian", true, true, false));
        pu.add(complex("Manakah dari berikut ini yang merupakan pulau besar di Indonesia?", "Sumatra", "Jawa", "Madagaskar", true, true, false));
        SUBJECTS.put("pengetahuan_umum", new QuestionSet(pu));

        // IPA (ipa)
        List<Question> ipa = new ArrayList<>();
        ipa.add(mc("Berapakah jumlah planet di tata surya kita?", "7 Planet", "8 Planet", "9 Planet", "10 Planet", "B"));
        ipa.add(mc("Pusat tata surya kita adalah?", "Bumi", "Bulan", "Matahari", "Mars", "C"));
        ipa.add(mc("Zat hijau daun pada tumbuhan disebut?", "Klorofil", "Oksigen", "Karbon", "Nitrogen", "A"));
        ipa.add(mc("Hewan yang memakan tumbuhan saja disebut?", "Karnivora", "Herbivora", "Omnivora", "Insektivora", "B"));
        ipa.add(mc("Gas yang dihirup manusia saat bernapas adalah?", "Karbondioksida", "Oksigen", "Nitrogen", "Helium", "B"));
        ipa.add(mc("Suhu air mendidih pada tekanan normal adalah?", "50 °C", "80 °C", "100 °C", "120 °C", "C"));
        ipa.add(tf("Apakah matahari mengelilingi bumi?", false));
        ipa.add(tf("Apakah air murni menghantarkan listrik dengan sangat baik?", false));
        ipa.add(complex("Manakah makhluk hidup berikut yang merupakan mamalia?", "Paus", "Elang", "Lumba-lumba", true, false, true));
        ipa.add(complex("Manakah dari berikut ini yang merupakan sumber energi terbarukan?", "Matahari", "Angin", "Batu Bara", true, true, false));
        SUBJECTS.put("ipa", new QuestionSet(ipa));

        // IPS (ips)
        List<Question> ips = new ArrayList<>();
        ips.add(mc("Candi Borobudur terletak di provinsi mana?", "Jawa Barat", "Jawa Timur", "Jawa Tengah", "D.I. Yogyakarta", "C"));
        ips.add(mc("Benua terbesar di dunia adalah?", "Afrika", "Asia", "Amerika", "Eropa", "B"));
        ips.add(mc("Samudra terluas di dunia adalah?", "Samudra Pasifik", "Samudra Atlantik", "Samudra Hindia", "Samudra Arktik", "A"));
        ips.add(mc("Negara tetangga Indonesia yang berbatasan darat di pulau Kalimantan adalah?", "Malaysia", "Singapura", "Filipina", "Brunei Darussalam", "A"));
        ips.add(mc("Rumah adat tongkonan berasal dari provinsi?", "Sumatra Barat", "Sulawesi Selatan", "Papua", "Jawa Tengah", "B"));
        ips.add(mc("Siapakah presiden pertama Republik Indonesia?", "Soeharto", "Soekarno", "B.J. Habibie", "Gus Dur", "B"));
        ips.add(tf("Apakah Indonesia memproklamasikan kemerdekaan pada tahun 1945?", true));
        ips.add(tf("Apakah benua Australia berada di sebelah utara Indonesia?", false));
        ips.add(complex("Manakah dari negara berikut yang merupakan pendiri organisasi ASEAN?", "Indonesia", "Singapura", "Vietnam", true, true, false));
        ips.add(complex("Manakah dari berikut ini yang merupakan pahlawan nasional Indonesia?", "Pangeran Diponegoro", "Ki Hajar Dewantara", "Gajah Mada", true, true, false));
        SUBJECTS.put("ips", new QuestionSet(ips));

        // MTK (mtk)
        List<Question> mtk = new ArrayList<>();
        mtk.add(mc("Berapakah hasil dari operasi matematika 5 + 3 * 2?", "16", "11", "13", "10", "B"));
        mtk.add(mc("Berapakah hasil dari 12 * 8?", "86", "96", "76", "106", "B"));
        mtk.add(mc("Nilai dari 150 - 75 + 25 adalah?", "50", "100", "75", "125", "B"));
        mtk.add(mc("Sebuah segitiga memiliki alas 10 cm dan tinggi 5 cm. Luasnya adalah?", "50 cm²", "25 cm²", "15 cm²", "30 cm²", "B"));
        mtk.add(mc("Berapakah akar kuadrat dari 144?", "10", "11", "12", "13", "C"));
        mtk.add(mc("Berapa banyak rusuk pada sebuah kubus?", "6", "8", "12", "16", "C"));
        mtk.add(tf("Apakah angka 2 merupakan satu-satunya bilangan prima yang genap?", true));
        mtk.add(tf("Apakah jumlah sudut dalam sebuah segitiga adalah 180 derajat?", true));
        mtk.add(complex("Manakah dari angka berikut yang merupakan bilangan genap?", "12", "15", "180", true, false, true));
        mtk.add(complex("Manakah dari bangun datar berikut yang memiliki 4 sudut siku-siku?", "Persegi", "Persegi Panjang", "Segitiga siku-siku", true, true, false));
        SUBJECTS.put("mtk", new QuestionSet(mtk));

        // B. Indo (b_indo)
        List<Question> bIndo = new ArrayList<>();
        bIndo.add(mc("Manakah penulisan kata baku yang benar menurut PUEBI?", "Apotik", "Apotek", "Raport", "Praktek", "B"));
        bIndo.add(mc("Lawan kata (antonim) dari kata 'luas' adalah?", "Sempit", "Lebar", "Besar", "Tinggi", "A"));
        bIndo.add(mc("Persamaan kata (sinonim) dari kata 'bohong' adalah?", "Jujur", "Dusta", "Nyata", "Benar", "B"));
        bIndo.add(mc("Kalimat yang memerlukan tanggapan atau jawaban disebut?", "Kalimat perintah", "Kalimat tanya", "Kalimat berita", "Kalimat seru", "B"));
        bIndo.add(mc("Pikiran utama dalam suatu paragraf disebut?", "Gagasan pokok", "Kalimat penjelas", "Latar", "Alur", "A"));
        bIndo.add(mc("Manakah penulisan huruf kapital yang benar?", "saya pergi ke Bandung.", "Saya pergi ke bandung.", "Saya pergi ke Bandung.", "saya pergi ke bandung.", "C"));
        bIndo.add(tf("Apakah kata 'cepat' merupakan antonim dari kata 'lambat'?", true));
        bIndo.add(tf("Apakah gurindam merupakan salah satu bentuk puisi modern?", false));
        bIndo.add(complex("Manakah kalimat di bawah ini yang berbentuk kalimat pasif?", "Budi membaca buku sejarah", "Buku sejarah dibaca oleh Budi", "Apel itu dimakan dengan lahap oleh Rina", false, true, true));
        bIndo.add(complex("Manakah yang merupakan kata depan (preposisi) dalam bahasa Indonesia?", "Di", "Ke", "Dan", true, true, false));
        SUBJECTS.put("b_indo", new QuestionSet(bIndo));

        // PKN (pkn)
        List<Question> pkn = new ArrayList<>();
        pkn.add(mc("Apakah lambang dari sila ke-3 dalam Pancasila?", "Bintang", "Rantai Emas", "Pohon Beringin", "Kepala Banteng", "C"));
        pkn.add(mc("Dasar negara Republik Indonesia adalah?", "UUD 1945", "Pancasila", "Bhinneka Tunggal Ika", "Proklamasi", "B"));
        pkn.add(mc("Lambang negara Indonesia adalah?", "Burung Garuda", "Bendera Merah Putih", "Lagu Indonesia Raya", "Monas", "A"));
        pkn.add(mc("Semboyan 'Bhinneka Tunggal Ika' memiliki arti?", "Bersatu kita teguh", "Berbeda-beda tetapi tetap satu jua", "Adil dan makmur", "Satu nusa satu bangsa", "B"));
        pkn.add(mc("UUD 1945 disahkan pada tanggal?", "17 Agustus 1945", "18 Agustus 1945", "1 Juni 1945", "22 Juni 1945", "B"));
        pkn.add(mc("Lembaga negara yang bertugas membuat undang-undang adalah?", "Presiden", "DPR", "MA", "MK", "B"));
        pkn.add(tf("Apakah Pancasila dirumuskan pertama kali dalam sidang BPUPKI?", true));
        pkn.add(tf("Apakah mematuhi tata tertib sekolah hanya kewajiban guru saja?", false));
        pkn.add(complex("Manakah perilaku di bawah ini yang mencerminkan musyawarah?", "Menghargai pendapat orang lain", "Memaksakan kehendak pribadi", "Menerima keputusan bersama dengan lapang dada", true, false, true));
        pkn.add(complex("Manakah yang merupakan hak seorang anak di rumah?", "Mendapatkan kasih sayang", "Membantu orang tua bekerja", "Mendapatkan makanan bergizi", true, false, true));
        SUBJECTS.put("pkn", new QuestionSet(pkn));

        // Bahasa Inggris (b_inggris)
        List<Question> bInggris = new ArrayList<>();
        bInggris.add(mc("What is the past tense form of the verb 'go'?", "Goed", "Went", "Gone", "Going", "B"));
        bInggris.add(mc("What is the plural form of the word 'child'?", "Childs", "Children", "Childes", "Childrens", "B"));
        bInggris.add(mc("Choose the correct pronoun: '___ is my sister. Her name is Sarah.'", "He", "She", "It", "They", "B"));
        bInggris.add(mc("What is the opposite of the word 'hot'?", "Cold", "Warm", "Dry", "Wet", "A"));
        bInggris.add(mc("Complete the sentence: 'They ___ playing football in the field.'", "Is", "Am", "Are", "Was", "C"));
        bInggris.add(mc("What is the capital letter of 'i'?", "l", "I", "j", "Y", "B"));
        bInggris.add(tf("Is the word 'apple' classified as a noun in English grammar?", true));
        bInggris.add(tf("Is the word 'run' an adjective?", false));
        bInggris.add(complex("Which of the following words are classified as adjectives?", "Beautiful", "Run", "Happy", true, false, true));
        bInggris.add(complex("Which of the following are pronouns in English?", "He", "They", "Book", true, true, false));
        SUBJECTS.put("b_inggris", new QuestionSet(bInggris));
    }

    public static QuestionSet getQuestions(String subject) {
        return SUBJECTS.get(subject.toLowerCase());
    }

    public static String getSubjectDisplayName(String subject) {
        switch (subject.toLowerCase()) {
            case "pengetahuan_umum":
                return "Pengetahuan Umum";
            case "ipa":
                return "IPA";
            case "ips":
                return "IPS";
            case "mtk":
                return "Matematika";
            case "b_indo":
                return "Bahasa Indonesia";
            case "pkn":
                return "PKN";
            case "b_inggris":
                return "Bahasa Inggris";
            default:
                return subject;
        }
    }

    public static int[] evaluateExam(ExamSession session) {
        QuestionSet questions = getQuestions(session.getSubject());
        if (questions == null) {
            return new int[]{0, 0}; // [benar, salah]
        }

        int benar = 0;
        int salah = 0;

        for (int i = 0; i < 10; i++) {
            Question q = questions.getQuestion(i);
            if (q == null) {
                continue;
            }

            if (q.getType() == QuestionType.MULTIPLE_CHOICE) {
                // Questions 1 to 6 (index 0 to 5)
                String playerAns = session.getMcAnswer(i);
                if (playerAns != null && playerAns.equalsIgnoreCase((String) q.getCorrectAnswer())) {
                    benar++;
                } else {
                    salah++;
                }
            } else if (q.getType() == QuestionType.TRUE_FALSE) {
                // Questions 7 to 8 (index 6 to 7)
                Boolean playerAns = session.getTfAnswer(i - 6);
                if (playerAns != null && playerAns.equals(q.getCorrectAnswer())) {
                    benar++;
                } else {
                    salah++;
                }
            } else if (q.getType() == QuestionType.COMPLEX_MULTIPLE_CHOICE) {
                // Questions 9 to 10 (index 8 to 9)
                boolean[] playerAns = session.getComplexAnswer(i - 8);
                boolean[] correctAns = (boolean[]) q.getCorrectAnswer();
                if (playerAns != null && playerAns.length == 3 && correctAns.length == 3
                        && playerAns[0] == correctAns[0]
                        && playerAns[1] == correctAns[1]
                        && playerAns[2] == correctAns[2]) {
                    benar++;
                } else {
                    salah++;
                }
            }
        }

        return new int[]{benar, salah};
    }
}
