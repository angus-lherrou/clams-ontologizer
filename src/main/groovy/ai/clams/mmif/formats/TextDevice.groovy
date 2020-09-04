package ai.clams.mmif.formats


/**
 * The TextDevice class is used to wrap the Console returned by System.console()
 * <p>
 * System.console() will be null is situations where there is no console device
 * available (i.e. running inside an IDE).
 *
 * @author Keith Suderman
 */
abstract class TextDevice {
    public static TextDevice create() {
        if (System.console()) {
            return new ConsoleDevice()
        }
        return new CharacterDevice()
    }
    abstract String readLine()
    abstract char[] readPassword()
    abstract void print(String s)
    abstract void println(String s)
    abstract TextDevice printf(String format, Object... params)
}

class ConsoleDevice extends TextDevice {
    Console console

    public ConsoleDevice() {
        console = System.console()
        printf "Creating a ConsoleDevice"
    }

    String readLine() {
        return console.readLine()
    }

    char[] readPassword() {
        return console.readPassword()
    }

    void print(String s) {
        console.printf(s)
    }

    void println(String s) {
        console.printf(s)
        console.printf("\n")
    }

    TextDevice printf(String format, Object... params) {
        console.printf(format, params)
        return this
    }
}

class CharacterDevice extends TextDevice {
    Reader reader
    PrintWriter writer

    public CharacterDevice() {
        reader = new BufferedReader(new InputStreamReader(System.in))
        writer = new PrintWriter(System.out)
        println "Creating a CharacterDevice."
    }

    String readLine() {
        return reader.readLine()
    }

    char[] readPassword() {
        return reader.readLine().toCharArray()
    }

    void print(String s) {
        writer.print(s)
        writer.flush()
    }

    void println(String s) {
        writer.print(s)
        writer.print('\n')
        writer.flush()
    }

    TextDevice printf(String format, Object... params) {
        writer.printf(format, params)
        writer.flush()
        return this
    }
}