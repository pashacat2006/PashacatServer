import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import java.util.jar.Manifest;
import org.yaml.snakeyaml.*;

@SuppressWarnings("unchecked")
class ConfigureServerShop {
    static final Path ROOT=Path.of("plugins/EconomyShopGUI"),BACKUP=Path.of("backups/2026-11-01-server-shop");
    static final Path SHOP_JAR=Path.of("plugins/FoliaUpdater/multithread/EconomyShopGUI-7.3.4.jar");
    static final Yaml yaml;
    static {DumperOptions options=new DumperOptions();options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);options.setAllowUnicode(true);options.setWidth(140);yaml=new Yaml(options);}
    static Map<String,Object> map(Object v){return (Map<String,Object>)v;}
    static Map<String,Object> read(Path path)throws Exception{return map(yaml.load(Files.readString(path)));}
    static void write(Path path,Object v)throws Exception {Files.createDirectories(path.getParent());Files.writeString(path,yaml.dump(v));}
    static Set<String> placeholders(Object v){Set<String> out=new TreeSet<>();Matcher m=Pattern.compile("%[A-Za-z][A-Za-z0-9_/.,:-]*%").matcher(String.valueOf(v));while(m.find())out.add(m.group());return out;}
    static void adaptJar(Path source,Path target,boolean fancy)throws Exception{
        Path temp=Path.of(target+".tmp");
        try(ZipFile zip=new ZipFile(source.toFile());ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(temp))){
            Map<String,byte[]> seen=new HashMap<>();
            var entries=zip.entries();while(entries.hasMoreElements()){
                ZipEntry e=entries.nextElement();if(e.getName().equals("paper-plugin.yml"))continue;
                byte[] content=zip.getInputStream(e).readAllBytes();
                if(fancy&&e.getName().equalsIgnoreCase("META-INF/MANIFEST.MF")){
                    Manifest manifest=new Manifest(new java.io.ByteArrayInputStream(content));
                    manifest.getMainAttributes().putValue("paperweight-mappings-namespace","mojang");
                    java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();manifest.write(bytes);content=bytes.toByteArray();
                }
                byte[] previous=seen.putIfAbsent(e.getName(),content);
                if(previous!=null){if(!Arrays.equals(previous,content))throw new IllegalStateException("Conflicting duplicate entry: "+e.getName());continue;}
                out.putNextEntry(new ZipEntry(e.getName()));out.write(content);out.closeEntry();
            }
            if(fancy){
                Map<String,Object> desc=map(yaml.load(zip.getInputStream(zip.getEntry("paper-plugin.yml"))));
                desc.remove("bootstrapper");desc.remove("loader");desc.remove("dependencies");
                desc.put("softdepend",List.of("Citizens","MiniPlaceholders","PlaceholderAPI","PlotSquared","SkinsRestorer"));
                out.putNextEntry(new ZipEntry("plugin.yml"));out.write(yaml.dump(desc).getBytes(StandardCharsets.UTF_8));out.closeEntry();
            }
        }
        Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);
    }
    public static void main(String[] args)throws Exception{
        Files.createDirectories(BACKUP);Files.createDirectories(ROOT.resolve("LanguageFiles"));
        Path originalJar=BACKUP.resolve("EconomyShopGUI-7.3.4-original.jar");if(!Files.exists(originalJar))Files.copy(SHOP_JAR,originalJar);
        Map<String,Object> en,ru;
        try(ZipFile zip=new ZipFile(originalJar.toFile())){
            en=map(yaml.load(zip.getInputStream(zip.getEntry("LanguageFiles/lang-en.yml"))));
            ru=map(yaml.load(zip.getInputStream(zip.getEntry("LanguageFiles/lang-ru.yml"))));
        }
        Map<String,Object> overrides=read(Path.of("tools/shop-language-ru.yml"));
        Map<String,Object> language=new LinkedHashMap<>(en);language.putAll(ru);
        for(var e:overrides.entrySet()){
            Object original=en.get(e.getKey());
            if(original!=null&&!placeholders(original).equals(placeholders(e.getValue())))throw new IllegalStateException("Language placeholders changed: "+e.getKey());
            language.put(e.getKey(),e.getValue());
        }
        for(var e:en.entrySet())if(!language.containsKey(e.getKey()))throw new IllegalStateException("Missing translation "+e.getKey());
        write(ROOT.resolve("LanguageFiles/lang-ru.yml"),language);
        Map<String,Object> config=read(BACKUP.resolve("original-config.yml"));
        config.put("language-file","lang-ru.yml");config.put("locale","ru-RU");config.put("per-player-languages",false);
        config.put("spawner-provider","DISABLED");config.put("enable-spawnerbreak",false);config.put("enable-spawnerplace",false);
        config.put("economy-provider","VAULT");config.put("override-sell-command",false);
        config.put("bought-items-lore",false);config.put("allow-unsafe-enchants",false);config.put("allow-illegal-stacks",false);
        config.put("use-vault-prefix-suffix",false);config.put("transaction-log",true);
        map(config.get("damaged-items")).put("enabled",false);
        config.put("sold-items-ignored-NBTtags",List.of("repair_cost"));
        map(map(config.get("currency-formats")).get("vault")).put("friendly","денег");
        map(map(map(config.get("transaction-screens-nav-bar")).get("items")).get(5)).put("name","&eПокупка / продажа");
        Map<String,Object> sellInfo=map(map(map(config.get("sellgui-nav-bar")).get("items")).get(5));
        sellInfo.put("name","&aПродать ресурсы");sellInfo.put("lore",List.of("&7Положите сюда предметы из магазина.","&7При закрытии меню они будут проданы.","&7Предметы без цены продажи вернутся вам."));
        Map<String,Object> mainNav=map(map(config.get("main-menu-nav-bar")).get("items"));
        map(mainNav.get(1)).put("name","&eВаш баланс");
        mainNav.put("5",Map.of("material","BOOK","name","&6Как торговать","lore",List.of("&aЛКМ по товару — купить","&6ПКМ по товару — продать","&7Цены указаны за один предмет.","&7Продажа ресурсов: /sellgui")));
        // SnakeYAML defaults use numeric navigation keys. Keep key types consistent.
        Object help=mainNav.remove("5");((Map)mainNav).put(5,help);
        write(ROOT.resolve("config.yml"),config);
        Path loader=Path.of("plugins/FoliaUpdater/config.yml");
        if(!Files.exists(BACKUP.resolve("foliaupdater-config.yml")))Files.copy(loader,BACKUP.resolve("foliaupdater-config.yml"));
        String loaderText=Files.readString(loader);
        if(!loaderText.contains("  EconomyShopGUI:"))loaderText+="\n  EconomyShopGUI: [Vault, Essentials, PlayerPoints, WorldGuard, PlaceholderAPI]\n  CrazyAuctions: [Vault, Essentials]\n  FancyNpcs: [PlaceholderAPI, SkinsRestorer]\n";
        yaml.load(loaderText);Files.writeString(loader,loaderText);
        adaptJar(originalJar,SHOP_JAR,false);
        Path fancy=Path.of("plugins/FancyNpcs-2.10.1-java21+2.jar"),fancyOriginal=BACKUP.resolve("FancyNpcs-2.10.1-java21+2-original.jar");
        if(!Files.exists(fancyOriginal))Files.copy(fancy,fancyOriginal);
        adaptJar(fancyOriginal,BACKUP.resolve("FancyNpcs-2.10.1-java21+2-foliaupdater.jar"),true);
        System.out.println("Configured Russian EconomyShopGUI: "+en.size()+" language keys, 6 categories, Vault currency, FoliaUpdater load order.");
        System.out.println("Prepared Bukkit manifests for EconomyShopGUI and FancyNpcs; plugin classes are unchanged.");
    }
}
