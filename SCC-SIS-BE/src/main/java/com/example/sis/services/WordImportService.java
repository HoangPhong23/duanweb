package com.example.sis.services;

import com.example.sis.dtos.quiz.OptionImportDTO;
import com.example.sis.dtos.quiz.QuestionImportDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WordImportService {
    
    private static final Logger logger = LoggerFactory.getLogger(WordImportService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Import câu hỏi từ file Word (.docx)
     * Hỗ trợ 2 định dạng:
     * 1. Định dạng text (Câu 1: ... A. ... B. ... Đáp án: A)
     * 2. Định dạng JSON trong Word
     */
    public List<QuestionImportDTO> importQuestionsFromWord(MultipartFile file) throws IOException {
        logger.info("📄 Importing questions from Word file: {}", file.getOriginalFilename());
        
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            
            // Đọc toàn bộ nội dung
            StringBuilder content = new StringBuilder();
            for (XWPFParagraph para : paragraphs) {
                content.append(para.getText()).append("\n");
            }
            
            String fullText = content.toString();
            
            // Kiểm tra định dạng JSON
            if (fullText.trim().startsWith("[") && fullText.trim().endsWith("]")) {
                logger.info("✅ Detected JSON format");
                return parseJsonFormat(fullText);
            } else {
                logger.info("✅ Detected text format");
                return parseTextFormat(fullText);
            }
        }
    }
    
    /**
     * Parse định dạng JSON
     * [
     *   {
     *     "questionText": "...",
     *     "points": 10,
     *     "options": [
     *       {"text": "...", "isCorrect": true},
     *       {"text": "...", "isCorrect": false}
     *     ]
     *   }
     * ]
     */
    private List<QuestionImportDTO> parseJsonFormat(String content) throws IOException {
        return objectMapper.readValue(content, new TypeReference<List<QuestionImportDTO>>() {});
    }
    
    /**
     * Parse định dạng text
     * Câu 1: Thẻ HTML nào dùng để tạo đoạn văn?
     * A. <p>
     * B. <div>
     * C. <span>
     * D. <text>
     * Đáp án: A
     */
    private List<QuestionImportDTO> parseTextFormat(String content) {
        List<QuestionImportDTO> questions = new ArrayList<>();
        
        // Pattern linh hoạt hơn: Chấp nhận Câu 1: hoặc Câu 1. hoặc Câu 1;
        // Sử dụng [\\s\\h] để bắt cả khoảng trắng thường và khoảng trắng đặc biệt (non-breaking space)
        Pattern questionPattern = Pattern.compile(
            "Câu[\\s\\h]+(\\d+)[\\s\\h]*[:.;][\\s\\h]*(.+?)(?=Câu[\\s\\h]+\\d+[\\s\\h]*[:.;]|$)", 
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        Matcher questionMatcher = questionPattern.matcher(content);
        
        while (questionMatcher.find()) {
            String questionBlock = questionMatcher.group(2).trim();
            QuestionImportDTO question = parseQuestionBlock(questionBlock);
            
            if (question != null) {
                questions.add(question);
            }
        }
        
        logger.info("✅ Parsed {} questions from text format", questions.size());
        return questions;
    }
    
    /**
     * Parse 1 block câu hỏi
     */
    private QuestionImportDTO parseQuestionBlock(String block) {
        try {
            // Tách dòng
            String[] lines = block.split("\\n");
            
            // Dòng đầu tiên là câu hỏi
            String questionText = lines[0].trim();
            
            // Parse options (A, B, C, D...)
            List<OptionImportDTO> options = new ArrayList<>();
            String correctAnswer = null;
            
            // Pattern cho options: A. hoặc A: hoặc A;
            Pattern optionPattern = Pattern.compile("^([A-Z])[\\s\\h]*[:.;][\\s\\h]*(.+)$", Pattern.CASE_INSENSITIVE);
            // Pattern cho đáp án: Chấp nhận cả "Đáp án:" và "Đáp án;"
            Pattern answerPattern = Pattern.compile("Đáp[\\s\\h]*án[\\s\\h]*[:.;][\\s\\h]*([A-Z])", Pattern.CASE_INSENSITIVE);
            
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                
                if (line.isEmpty()) continue;
                
                // Kiểm tra option
                Matcher optionMatcher = optionPattern.matcher(line);
                if (optionMatcher.matches()) {
                    String optionLetter = optionMatcher.group(1).toUpperCase();
                    String optionText = optionMatcher.group(2);
                    
                    OptionImportDTO option = new OptionImportDTO();
                    option.setText(optionText);
                    option.setIsCorrect(false); 
                    options.add(option);
                    continue;
                }
                
                // Kiểm tra đáp án
                Matcher answerMatcher = answerPattern.matcher(line);
                if (answerMatcher.find()) {
                    correctAnswer = answerMatcher.group(1).toUpperCase();
                }
            }
            
            // Set correct answer
            if (correctAnswer != null && !options.isEmpty()) {
                int correctIndex = correctAnswer.charAt(0) - 'A';
                if (correctIndex >= 0 && correctIndex < options.size()) {
                    options.get(correctIndex).setIsCorrect(true);
                }
            }
            
            // Tạo QuestionImportDTO
            if (!questionText.isEmpty() && !options.isEmpty()) {
                QuestionImportDTO question = new QuestionImportDTO();
                question.setQuestionText(questionText);
                question.setPoints(10); // Mặc định 10 điểm
                question.setOptions(options);
                return question;
            }
            
        } catch (Exception e) {
            logger.error("❌ Error parsing question block: {}", e.getMessage());
        }
        
        return null;
    }
}
