import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.yaml.snakeyaml.Yaml;
class InspectShopLanguage {
    public static void main(String[] args)throws Exception {
        try(ZipFile jar=new ZipFile("plugins/FoliaUpdater/multithread/EconomyShopGUI-7.3.4.jar")){
            Yaml yaml=new Yaml();Map<?,?> en=yaml.load(jar.getInputStream(jar.getEntry("LanguageFiles/lang-en.yml")));
            Map<?,?> ru=yaml.load(jar.getInputStream(jar.getEntry("LanguageFiles/lang-ru.yml")));
            Map<Object,Object> missing=new LinkedHashMap<>();
            for(var e:en.entrySet())if(!ru.containsKey(e.getKey())||(ru.get(e.getKey()) instanceof String s&&!s.matches("(?s).*[А-Яа-яЁё].*")&&s.matches("(?s).*[a-zA-Z]{3} [a-zA-Z]{2}.*")))missing.put(e.getKey(),e.getValue());
            String text=yaml.dump(missing);Files.writeString(Path.of("backups/2026-11-01-server-shop/missing-translations.yml"),text);
            System.out.println("English keys: "+en.size()+"; Russian keys: "+ru.size()+"; Missing/untranslated: "+missing.size());System.out.println(text);
        }
    }
}
