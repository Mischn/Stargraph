package net.stargraph.core.graph.batch;

import org.apache.jena.atlas.io.AWriter;
import org.apache.jena.atlas.io.AWriterBase;
import org.apache.jena.atlas.lib.Closeable;

public class BatchUtils {

    public static class AStringWriter extends AWriterBase implements AWriter, Closeable {
        protected StringBuilder stringBuilder;

        public AStringWriter() {
            this.stringBuilder = new StringBuilder();
        }

        @Override
        public void print(char ch) {
            stringBuilder.append(ch);
        }

        @Override
        public void print(String string) {
            stringBuilder.append(string);
        }

        @Override
        public void print(char[] cbuf) {
            stringBuilder.append(cbuf);
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}

        @Override
        public void printf(String fmt, Object... args) {
            print(String.format(fmt, args));
        }

        @Override
        public void println(String obj) {
            print(obj);
            print("\n");
        }

        @Override
        public void println() {
            print("\n");
        }

        @Override
        public String toString() {
            return stringBuilder.toString();
        }

        public void clear() {
            this.stringBuilder.delete(0, stringBuilder.length());
        }
    }
}
