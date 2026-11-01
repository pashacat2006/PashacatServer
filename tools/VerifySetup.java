import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import org.yaml.snakeyaml.Yaml;

@SuppressWarnings("unchecked")
class VerifySetup {
    static Map map(Object v) { return (Map)v; }
    static Object at(Object v,String... keys) { for(String k:keys)v=map(v).get(k);return v; }
    static void check(boolean b,String message) { if(!b)throw new IllegalStateException(message); }
    public static void main(String[] args)throws Exception {
        Yaml yaml=new Yaml();
        Object msg=yaml.load(Files.readString(Path.of("plugins/FlectonePulse/message.yml")));
        for(String k:List.of("caps","flood","swear"))check(Boolean.TRUE.equals(at(msg,"format","moderation",k,"enable")),k+" disabled");
        var regexes=(List<String>)at(msg,"format","moderation","swear","trigger");
        List<Pattern> patterns=regexes.stream().map(r->Pattern.compile(r,Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE)).toList();
        for(String word:List.of("блядь","пиздец","хуй","пидор"))check(patterns.stream().anyMatch(p->p.matcher(word).find()),"Not filtered: "+word);
        for(String word:List.of("привет","спасибо","хулиган","бляха"))check(patterns.stream().noneMatch(p->p.matcher(word).find()),"False positive: "+word);
        Object wg=yaml.load(Files.readString(Path.of("plugins/WorldGuard/worlds/world/regions.yml")));
        check("allow".equals(at(wg,"regions","abandoned_house","flags","mob-spawning")),"Mob spawning blocked");
        check("allow".equals(at(wg,"regions","abandoned_house","flags","chest-access")),"Chests blocked");
        Object integration=yaml.load(Files.readString(Path.of("plugins/FlectonePulse/integration.yml")));
        check(Boolean.TRUE.equals(at(integration,"telegram","enable")),"Telegram disabled");
        check(!((String)at(integration,"telegram","token")).isBlank(),"Telegram token missing");
        for(String key:List.of("INTEGRATION_TELEGRAM","MESSAGE_CHAT_GLOBAL"))
            check(List.of("-5396516398").equals(at(integration,"telegram","message_channel",key)),"Incorrect Telegram mapping: "+key);
        try(ZipFile zip=new ZipFile("plugins/FoliaUpdater/multithread/FancyNpcs-2.10.1-java21+2.jar")) {
            Object plugin=yaml.load(zip.getInputStream(zip.getEntry("plugin.yml")));
            check(Boolean.TRUE.equals(at(plugin,"folia-supported")),"Missing Folia support");
            String main=(String)at(plugin,"main");
            byte[] cls=zip.getInputStream(zip.getEntry(main.replace('.','/')+".class")).readAllBytes();
            int major=((cls[6]&255)<<8)|(cls[7]&255);check(major<=65,"Requires Java newer than 21");
            System.out.println("FancyNpcs: "+at(plugin,"version")+", Folia supported, Java class version "+major);
        }
        System.out.println("YAML, filter regex examples, WorldGuard permissions: OK. Telegram enabled and both chat mappings verified.");
    }
}
