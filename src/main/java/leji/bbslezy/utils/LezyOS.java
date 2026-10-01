package leji.bbslezy.utils;


import java.util.function.Supplier;

public class LezyOS
{
    public static Supplier<String> osName = () -> System.getProperty("os.name", "");

    public static boolean isWindows()
    {
        return osName.get().toLowerCase().contains("win");
    }

    public static boolean isLinuxLike()
    {
        String os = osName.get().toLowerCase();

        return os.contains("nux") || os.contains("nix") || os.contains("aix");
    }
}
