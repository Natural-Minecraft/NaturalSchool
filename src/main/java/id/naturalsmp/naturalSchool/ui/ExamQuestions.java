package id.naturalsmp.naturalSchool.ui;

import java.util.HashMap;
import java.util.Map;

public final class ExamQuestions {

    public static class QuestionSet {
        public final String q1Text;
        public final String q1A;
        public final String q1B;
        public final String q1C;
        public final String q1D;
        public final String q1Correct; // "A", "B", "C", or "D"

        public final String q2Text;
        public final boolean q2Correct; // true or false

        public final String q3Text;
        public final String q3Stmt1;
        public final String q3Stmt2;
        public final String q3Stmt3;
        public final boolean q3Correct1;
        public final boolean q3Correct2;
        public final boolean q3Correct3;

        public QuestionSet(
            String q1Text, String q1A, String q1B, String q1C, String q1D, String q1Correct,
            String q2Text, boolean q2Correct,
            String q3Text, String q3Stmt1, String q3Stmt2, String q3Stmt3,
            boolean q3Correct1, boolean q3Correct2, boolean q3Correct3
        ) {
            this.q1Text = q1Text;
            this.q1A = q1A;
            this.q1B = q1B;
            this.q1C = q1C;
            this.q1D = q1D;
            this.q1Correct = q1Correct;
            this.q2Text = q2Text;
            this.q2Correct = q2Correct;
            this.q3Text = q3Text;
            this.q3Stmt1 = q3Stmt1;
            this.q3Stmt2 = q3Stmt2;
            this.q3Stmt3 = q3Stmt3;
            this.q3Correct1 = q3Correct1;
            this.q3Correct2 = q3Correct2;
            this.q3Correct3 = q3Correct3;
        }
    }

    private static final Map<String, QuestionSet> SUBJECTS = new HashMap<>();

    static {
        // Pengetahuan Umum (p_umum)
        SUBJECTS.put("pengetahuan_umum", new QuestionSet(
            "Siapa pencipta NaturalSMP?",
            "Saya", "Jopeh", "AnakTentara", "Gua", "C",
            "Apakah NaturalSMP didirikan pada tahun 2026?", true,
            "Manakah dari pernyataan berikut yang benar mengenai NaturalSchool?",
            "NaturalSchool adalah plugin sekolah",
            "1 Semester = 14 hari real-life",
            "Grade pemain naik otomatis tanpa ujian",
            true, true, false
        ));

        // IPA (ipa)
        SUBJECTS.put("ipa", new QuestionSet(
            "Berapakah jumlah planet di tata surya kita?",
            "7 Planet", "8 Planet", "9 Planet", "10 Planet", "B",
            "Apakah air murni mendidih pada suhu 100 derajat Celsius di bawah tekanan atmosfer standar?", true,
            "Manakah makhluk hidup berikut yang merupakan mamalia?",
            "Paus",
            "Elang",
            "Lumba-lumba",
            true, false, true
        ));

        // IPS (ips)
        SUBJECTS.put("ips", new QuestionSet(
            "Candi Borobudur terletak di provinsi mana?",
            "Jawa Barat", "Jawa Timur", "Jawa Tengah", "D.I. Yogyakarta", "C",
            "Apakah Indonesia memproklamasikan kemerdekaan pada tahun 1945?", true,
            "Manakah dari negara berikut yang merupakan pendiri organisasi ASEAN?",
            "Indonesia",
            "Singapura",
            "Vietnam",
            true, true, false
        ));

        // MTK (mtk)
        SUBJECTS.put("mtk", new QuestionSet(
            "Berapakah hasil dari operasi matematika 5 + 3 * 2?",
            "16", "11", "13", "10", "B",
            "Apakah angka 2 merupakan satu-satunya bilangan prima yang genap?", true,
            "Manakah dari angka berikut yang merupakan bilangan genap?",
            "12",
            "15",
            "180",
            true, false, true
        ));

        // B. Indo (b_indo)
        SUBJECTS.put("b_indo", new QuestionSet(
            "Manakah penulisan kata baku yang benar menurut PUEBI?",
            "Apotik", "Apotek", "Raport", "Praktek", "B",
            "Apakah kata 'cepat' merupakan antonim dari kata 'lambat'?", true,
            "Manakah kalimat di bawah ini yang berbentuk kalimat pasif?",
            "Budi membaca buku sejarah",
            "Buku sejarah dibaca oleh Budi",
            "Apel itu dimakan dengan lahap oleh Rina",
            false, true, true
        ));

        // PKN (pkn)
        SUBJECTS.put("pkn", new QuestionSet(
            "Apakah lambang dari sila ke-3 dalam Pancasila?",
            "Bintang", "Rantai Emas", "Pohon Beringin", "Kepala Banteng", "C",
            "Apakah Pancasila dirumuskan pertama kali dalam sidang BPUPKI?", true,
            "Manakah perilaku di bawah ini yang mencerminkan musyawarah?",
            "Menghargai pendapat orang lain",
            "Memaksakan kehendak pribadi",
            "Menerima keputusan bersama dengan lapang dada",
            true, false, true
        ));

        // B. Inggris (b_inggris)
        SUBJECTS.put("b_inggris", new QuestionSet(
            "What is the past tense form of the verb 'go'?",
            "Goed", "Went", "Gone", "Going", "B",
            "Is the word 'apple' classified as a noun in English grammar?", true,
            "Which of the following words are classified as adjectives?",
            "Beautiful",
            "Run",
            "Happy",
            true, false, true
        ));
    }

    public static QuestionSet getQuestions(String subject) {
        return SUBJECTS.get(subject.toLowerCase());
    }

    public static String getSubjectDisplayName(String subject) {
        switch (subject.toLowerCase()) {
            case "pengetahuan_umum": return "Pengetahuan Umum";
            case "ipa": return "IPA";
            case "ips": return "IPS";
            case "mtk": return "Matematika";
            case "b_indo": return "Bahasa Indonesia";
            case "pkn": return "PKN";
            case "b_inggris": return "Bahasa Inggris";
            default: return subject;
        }
    }

    public static int[] evaluateExam(ExamSession session) {
        QuestionSet questions = getQuestions(session.getSubject());
        if (questions == null) {
            return new int[]{0, 0}; // [benar, salah]
        }

        int benar = 0;
        int salah = 0;

        // Soal 1 (Multiple Choice)
        String q1Ans = "";
        if (session.isAnsA()) q1Ans = "A";
        else if (session.isAnsB()) q1Ans = "B";
        else if (session.isAnsC()) q1Ans = "C";
        else if (session.isAnsD()) q1Ans = "D";

        if (q1Ans.equals(questions.q1Correct)) {
            benar++;
        } else {
            salah++;
        }

        // Soal 2 (True / False)
        if (session.getTrueOrFalse() != null) {
            if (session.getTrueOrFalse() == questions.q2Correct) {
                benar++;
            } else {
                salah++;
            }
        } else {
            salah++;
        }

        // Soal 3 (Multiple Statement Checklist)
        if (session.isStmt1() == questions.q3Correct1
            && session.isStmt2() == questions.q3Correct2
            && session.isStmt3() == questions.q3Correct3) {
            benar++;
        } else {
            salah++;
        }

        return new int[]{benar, salah};
    }
}
