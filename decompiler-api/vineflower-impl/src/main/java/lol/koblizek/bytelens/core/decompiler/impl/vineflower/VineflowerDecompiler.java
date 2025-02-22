package lol.koblizek.bytelens.core.decompiler.impl.vineflower;

import lol.koblizek.bytelens.core.decompiler.api.Decompiler;
import lol.koblizek.bytelens.core.decompiler.api.Option;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.decompiler.DirectoryResultSaver;
import org.jetbrains.java.decompiler.main.decompiler.SingleFileSaver;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VineflowerDecompiler extends Decompiler {

    public VineflowerDecompiler(Map<String, Object> options) {
        super(options);
    }

    public VineflowerDecompiler() {
        super(IFernflowerPreferences.DEFAULTS);
    }

    private Fernflower construct(IResultSaver saver) {
        return new Fernflower(saver, options, new VineflowerLogger(LOGGER));
    }

    @Override
    public String decompilePreview(byte[] bytecode) {
        try (StringWriter writer = new StringWriter()) {
            Fernflower ff = construct(new PreviewResultSaver(LOGGER, writer));
            ff.addSource(new BytecodeContextSource(bytecode));
            ff.decompileContext();
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Failed to decompile preview", e);
            return "/*" + e.getMessage() + "*/\n";
        }
    }

    @Override
    public void decompile(Path in, Path out) {
        IResultSaver saver;
        if (Files.isDirectory(out))
            saver = new DirectoryResultSaver(out.toFile());
        else {
            saver = new SingleFileSaver(out.toFile());
        }
        Fernflower fernflower = construct(saver);
        fernflower.addSource(in.toFile());
        fernflower.decompileContext();
    }

    @Override
    public List<Option> getSupportedOptions() {
        List<Option> opts = new ArrayList<>();
        for (Field field : IFernflowerPreferences.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(IFernflowerPreferences.Name.class)) {
                String name = field.getAnnotation(IFernflowerPreferences.Name.class).value();
                String desc = field.getAnnotation(IFernflowerPreferences.Description.class).value();
                String shortName = field.getAnnotation(IFernflowerPreferences.ShortName.class).value();
                opts.add(new Option(name, desc, shortName));
            }
        }
        return opts;
    }
}
