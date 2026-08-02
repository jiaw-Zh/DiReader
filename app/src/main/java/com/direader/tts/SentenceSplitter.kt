package com.direader.tts

/**
 * Utility object for splitting Chinese text into sentences for TTS processing.
 */
object SentenceSplitter {
    private const val MAX_SENTENCE_LENGTH = 300
    private const val SECONDARY_SPLIT_THRESHOLD = 200

    private val primaryPunctuation = setOf('。', '！', '？', '.', '!', '?')
    private val secondaryPunctuation = setOf('；', '：', '，', ';', ':', ',')

    /**
     * Splits the given text into a list of sentences suitable for TTS.
     * 
     * @param text The input text to split.
     * @return A list of trimmed, non-empty sentences.
     */
    fun split(text: String): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split('\n')
        
        for (paragraph in paragraphs) {
            val trimmedPara = paragraph.trim()
            if (trimmedPara.isEmpty()) continue
            
            var currentSentence = StringBuilder()
            var inQuotes = false
            
            for (i in trimmedPara.indices) {
                val char = trimmedPara[i]
                currentSentence.append(char)
                
                // Track Chinese quote state (U+201C and U+201D)
                if (char == '\u201C') { // Left quote
                    inQuotes = true
                } else if (char == '\u201D') { // Right quote
                    inQuotes = false
                }
                
                if (!inQuotes && primaryPunctuation.contains(char)) {
                    val sentence = currentSentence.toString().trim()
                    if (sentence.isNotEmpty()) {
                        result.addAll(processSentence(sentence))
                    }
                    currentSentence.clear()
                }
            }
            
            val remaining = currentSentence.toString().trim()
            if (remaining.isNotEmpty()) {
                result.addAll(processSentence(remaining))
            }
        }
        
        return result.filter { it.isNotEmpty() }
    }
    
    private fun processSentence(sentence: String): List<String> {
        if (sentence.length <= SECONDARY_SPLIT_THRESHOLD) {
            return listOf(sentence)
        }
        
        val result = mutableListOf<String>()
        var currentChunk = StringBuilder()
        var inQuotes = false
        
        for (char in sentence) {
            currentChunk.append(char)
            
            if (char == '\u201C') {
                inQuotes = true
            } else if (char == '\u201D') {
                inQuotes = false
            }
            
            if (!inQuotes && secondaryPunctuation.contains(char) && currentChunk.length >= SECONDARY_SPLIT_THRESHOLD) {
                result.add(currentChunk.toString().trim())
                currentChunk.clear()
            } else if (currentChunk.length >= MAX_SENTENCE_LENGTH) {
                result.add(currentChunk.toString().trim())
                currentChunk.clear()
            }
        }
        
        val remaining = currentChunk.toString().trim()
        if (remaining.isNotEmpty()) {
            result.add(remaining)
        }
        
        return result
    }
}
