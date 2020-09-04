package ai.clams.mmif.formats

/**
 * @author Keith Suderman
 */
class VocabularyException  extends Exception {
    VocabularyException() {
    }

    VocabularyException(String message) {
        super(message)
    }

    VocabularyException(String message, Throwable cause) {
        super(message, cause)
    }

    VocabularyException(Throwable message) {
        super(message)
    }

    VocabularyException(String message, Throwable cause, boolean var3, boolean var4) {
        super(message, cause, var3, var4)
    }
}
