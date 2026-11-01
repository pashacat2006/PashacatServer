import java.nio.file.*;
import java.util.*;
import org.yaml.snakeyaml.Yaml;
@SuppressWarnings("unchecked")
class VerifyTitleMenu {
    static final Yaml YAML=new Yaml();
    static Map<String,Object> read(String path)throws Exception{return YAML.load(Files.readString(Path.of(path)));}
    static Map<String,Object> map(Object v){return (Map<String,Object>)v;}
    static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    public static void main(String[] args)throws Exception{
        Map<String,Object> titles=map(read("server-titles/resources/config.yml").get("titles"));
        Map<String,Object> menu=read("plugins/DeluxeMenus/gui_menus/title.yml"),items=map(menu.get("items"));
        check(((Number)menu.get("size")).intValue()==54,"Wrong menu size");Set<Integer> slots=new HashSet<>();
        for(var e:titles.entrySet()){
            String id=e.getKey();Map<String,Object> title=map(e.getValue()),normal=map(items.get(id)),selected=map(items.get(id+"_selected"));
            int slot=((Number)title.get("slot")).intValue();check(slots.add(slot),"Title slot overlaps: "+id);
            check(Objects.equals(normal.get("slot"),selected.get("slot")),"Selected slot mismatch");
            check(((Number)selected.get("priority")).intValue()<((Number)normal.get("priority")).intValue(),"Selected priority wrong");
            check(((List<?>)normal.get("left_click_commands")).equals(List.of("[close]","[player] servertitles select "+id)),"Purchase bypasses backend");
            check(((List<?>)normal.get("right_click_commands")).equals(normal.get("left_click_commands")),"Right click differs");
            long amount=((Number)title.get("requirement")).longValue();check(amount>0,"Nonpositive requirement");
            if("PLAYTIME".equals(title.get("kind")))check(amount%72000==0,"Hours must use 72000 ticks");
            check(title.get("suffix").toString().contains(title.get("name").toString()),"Missing title preview");
        }
        check(titles.size()==15,"Expected 15 titles");
        for(var e:items.entrySet()){Map<String,Object> item=map(e.getValue());if(item.containsKey("slots"))for(Object slot:(List<?>)item.get("slots"))check(!slots.contains(((Number)slot).intValue()),"Filler masks title");}
        Map<String,Object> dm=read("plugins/DeluxeMenus/config.yml");check("title.yml".equals(map(map(dm.get("gui_menus")).get("Title")).get("file")),"Title not registered");
        var meta=map(read("plugins/LuckPerms/config.yml").get("meta-formatting"));var before=map(read("backups/2026-11-01-title-menu/luckperms-config.yml").get("meta-formatting"));
        check(meta.get("prefix").equals(before.get("prefix")),"Rank prefixes changed");
        check(map(meta.get("suffix")).get("format").equals(List.of("highest")),"More than one suffix can be displayed");
        System.out.println("PASS: 15 titles, 54 slots, menu registration, backend-only purchases, tick thresholds, preserved rank prefixes and exactly one visible suffix.");
    }
}
