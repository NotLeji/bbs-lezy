package leji.bbslezy.vfx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every uniform declared in a core shader JSON must be declared and actually used in the linked
 * GLSL, otherwise Minecraft logs "could not find uniform named ..." at boot. The GLSL compiler
 * drops unused declarations, so a declaration alone is not enough.
 */
public class ShaderUniformDeclarationsTest
{
    private static final Path SHADERS = Path.of("src/main/resources/assets/bbslezy/shaders/core");
    private static final Pattern DECLARATION = Pattern.compile("uniform\\s+\\w+\\s+(\\w+)\\s*;");
    private static final Pattern NAME = Pattern.compile("\"name\"\\s*:\\s*\"(\\w+)\"");

    @Test
    public void jsonUniformsAreDeclaredAndUsed() throws IOException
    {
        List<String> problems = new ArrayList<>();
        int checked = 0;

        try (Stream<Path> files = Files.list(SHADERS))
        {
            for (Path json : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".json"))::iterator)
            {
                List<String> uniforms = new ArrayList<>();
                Matcher names = NAME.matcher(uniformsSection(Files.readString(json)));

                while (names.find())
                {
                    uniforms.add(names.group(1));
                }

                String glsl = read(json, ".vsh") + "\n" + read(json, ".fsh");
                Matcher declarations = DECLARATION.matcher(glsl);
                List<String> declared = new ArrayList<>();

                while (declarations.find())
                {
                    declared.add(declarations.group(1));
                }

                String body = DECLARATION.matcher(glsl).replaceAll("");

                for (String uniform : uniforms)
                {
                    checked++;

                    if (!declared.contains(uniform))
                    {
                        problems.add(json.getFileName() + ": " + uniform + " is not declared in GLSL");
                    }
                    else if (!Pattern.compile("\\b" + Pattern.quote(uniform) + "\\b").matcher(body).find())
                    {
                        problems.add(json.getFileName() + ": " + uniform + " is declared but unused");
                    }
                }
            }
        }

        assertTrue(checked > 0, "no shader uniforms scanned");
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    /** Slice the "uniforms" array so sampler/attribute names are not mistaken for uniforms. */
    private static String uniformsSection(String source)
    {
        Matcher start = Pattern.compile("\"uniforms\"\\s*:\\s*\\[").matcher(source);

        if (!start.find())
        {
            return "";
        }

        int depth = 0;

        for (int i = start.end() - 1; i < source.length(); i++)
        {
            char c = source.charAt(i);

            if (c == '[')
            {
                depth++;
            }
            else if (c == ']' && --depth == 0)
            {
                return source.substring(start.end(), i);
            }
        }

        return "";
    }

    private static String read(Path json, String extension) throws IOException
    {
        Path path = Path.of(json.toString().replace(".json", extension));

        return Files.exists(path) ? Files.readString(path) : "";
    }
}
