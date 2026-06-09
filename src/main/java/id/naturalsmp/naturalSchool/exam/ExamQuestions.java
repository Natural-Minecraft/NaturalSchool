package id.naturalsmp.naturalSchool.exam;

import java.util.List;

public final class ExamQuestions {

    public static class Question {
        public final String packetId;
        public final int academicClass;
        public final int questionNumber;
        public final String questionType;
        public final String questionText;
        public final List<String> options;
        public final String correctAnswer;
        public final List<Integer> correctIndices;

        public Question(String packetId, int academicClass, int questionNumber, String questionType,
                        String questionText, List<String> options, String correctAnswer, List<Integer> correctIndices) {
            this.packetId = packetId;
            this.academicClass = academicClass;
            this.questionNumber = questionNumber;
            this.questionType = questionType;
            this.questionText = questionText;
            this.options = options;
            this.correctAnswer = correctAnswer;
            this.correctIndices = correctIndices;
        }
    }

    public static String getSubjectName(int subjectId) {
        switch (subjectId) {
            case 1: return "Pengetahuan Umum";
            case 2: return "Pendidikan Pancasila";
            case 3: return "Bahasa Indonesia";
            case 4: return "Bahasa Inggris";
            case 5: return "Matematika";
            case 6: return "Ilmu Pengetahuan Alam";
            case 7: return "Ilmu Pengetahuan Sosial";
            default: return "Unknown Subject";
        }
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

    public static int[] evaluateExam(ExamSession session, List<Question> questions) {
        if (questions == null) {
            return new int[]{0, 0}; // [benar, salah]
        }

        int benar = 0;
        int salah = 0;

        for (int i = 0; i < questions.size(); i++) {
            String ans = session.getAnswer(i);
            Question q = questions.get(i);
            if (ans != null && ans.equalsIgnoreCase(q.correctAnswer)) {
                benar++;
            } else {
                salah++;
            }
        }

        return new int[]{benar, salah};
    }
}

