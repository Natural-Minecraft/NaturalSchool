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
            if (ans == null) {
                salah++;
                continue;
            }

            boolean isCorrect = false;
            // 1. Check if direct choice code matches correct answer (e.g., "A", "B")
            if (ans.equalsIgnoreCase(q.correctAnswer)) {
                isCorrect = true;
            } else {
                // 2. Check if choice text matches correct answer (e.g., "BENAR", "SALAH")
                List<String> options = q.options;
                if (options == null || options.isEmpty()) {
                    if ("TRUE_FALSE".equalsIgnoreCase(q.questionType) 
                        || "TF".equalsIgnoreCase(q.questionType) 
                        || "BENAR_SALAH".equalsIgnoreCase(q.questionType)
                        || "BS".equalsIgnoreCase(q.questionType)) {
                        options = java.util.Arrays.asList("Benar", "Salah");
                    }
                }
                if (options != null && ans.length() == 1) {
                    char ch = ans.toUpperCase().charAt(0);
                    int idx = ch - 'A';
                    if (idx >= 0 && idx < options.size()) {
                        String optText = options.get(idx);
                        if (optText != null && optText.equalsIgnoreCase(q.correctAnswer)) {
                            isCorrect = true;
                        }
                    }
                }
            }

            if (isCorrect) {
                benar++;
            } else {
                salah++;
            }
        }

        return new int[]{benar, salah};
    }
}

