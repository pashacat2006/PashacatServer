import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.yaml.snakeyaml.Yaml;

@SuppressWarnings("unchecked")
class VerifyServerShop {
    static final Yaml yaml=new Yaml();
    static final Map<String,Double> costs=new HashMap<>(),sales=new HashMap<>();
    static final Map<String,Object> tags=new HashMap<>();
    static final List<Map<String,Object>> recipes=new ArrayList<>();
    static String id(String s){return s.replace("minecraft:","").toUpperCase(Locale.ROOT);}
    static Map<String,Object> map(Object v){return (Map<String,Object>)v;}
    static double ingredient(Object v,Set<String> visiting){
        if(v instanceof String s){if(!s.startsWith("#"))return costs.getOrDefault(id(s),Double.POSITIVE_INFINITY);
            if(!visiting.add(s))return Double.POSITIVE_INFINITY;Object t=tags.get(s.substring(1).replace("minecraft:",""));
            double best=Double.POSITIVE_INFINITY;if(t instanceof List<?> l)for(Object entry:l)best=Math.min(best,ingredient(entry,visiting));visiting.remove(s);return best;}
        if(v instanceof List<?> l){double best=Double.POSITIVE_INFINITY;for(Object x:l)best=Math.min(best,ingredient(x,visiting));return best;}
        if(v instanceof Map<?,?> m){if(m.containsKey("item"))return ingredient(m.get("item"),visiting);if(m.containsKey("tag"))return ingredient("#"+m.get("tag"),visiting);if(m.containsKey("id"))return ingredient(m.get("id"),visiting);}
        return Double.POSITIVE_INFINITY;
    }
    static double ingredient(Object v){return ingredient(v,new HashSet<>());}
    static double cost(Map<String,Object> r){
        String type=(String)r.get("type");double sum=0;
        switch(type){
            case "minecraft:crafting_shaped":
                Map<String,Object> keys=map(r.get("key"));for(Object row:(List<?>)r.get("pattern"))for(char c:row.toString().toCharArray())if(c!=' ')sum+=ingredient(keys.get(String.valueOf(c)));return sum;
            case "minecraft:crafting_shapeless":for(Object part:(List<?>)r.get("ingredients"))sum+=ingredient(part);return sum;
            case "minecraft:smelting":case "minecraft:blasting":case "minecraft:smoking":case "minecraft:campfire_cooking":case "minecraft:stonecutting":return ingredient(r.get("ingredient"));
            case "minecraft:smithing_transform":return ingredient(r.get("template"))+ingredient(r.get("base"))+ingredient(r.get("addition"));
            default:return Double.POSITIVE_INFINITY;
        }
    }
    static String resolve(String alias,Map<String,Object> aliases){
        Set<String> seen=new HashSet<>();while(seen.add(alias)){Object v=aliases.get(alias);if(v instanceof String s){alias=s;continue;}if(v instanceof Map<?,?> m&&m.get("material") instanceof String s)return id(s);break;}return id(alias);
    }
    static void sameClasses(Path original,Path adapted)throws Exception{
        try(ZipFile a=new ZipFile(original.toFile());ZipFile b=new ZipFile(adapted.toFile())){
            if(b.getEntry("paper-plugin.yml")!=null||b.getEntry("plugin.yml")==null)throw new IllegalStateException("Bukkit manifest missing");
            if(original.getFileName().toString().startsWith("FancyNpcs")){
                var manifest=new java.util.jar.Manifest(b.getInputStream(b.getEntry("META-INF/MANIFEST.MF")));
                if(!"mojang".equals(manifest.getMainAttributes().getValue("paperweight-mappings-namespace")))throw new IllegalStateException("FancyNpcs mappings namespace must be mojang");
            }
            var entries=a.entries();while(entries.hasMoreElements()){var e=entries.nextElement();if(e.getName().endsWith(".class")){var other=b.getEntry(e.getName());if(other==null||!Arrays.equals(a.getInputStream(e).readAllBytes(),b.getInputStream(other).readAllBytes()))throw new IllegalStateException("Class modified: "+e.getName());}}
        }
    }
    public static void main(String[] args)throws Exception{
        List<?> catalog=yaml.load(Files.readString(Path.of("tools/shop-catalog.json")));int items=0;Set<Integer> slots=new HashSet<>();
        for(Object category:catalog){Map<String,Object> c=map(category);if(!slots.add(((Number)c.get("slot")).intValue()))throw new IllegalStateException("Overlapping categories");
            Map<String,Object> section=yaml.load(Files.readString(Path.of("plugins/EconomyShopGUI/sections/"+c.get("id")+".yml")));
            Map<String,Object> shop=yaml.load(Files.readString(Path.of("plugins/EconomyShopGUI/shops/"+c.get("id")+".yml")));Set<Integer> itemSlots=new HashSet<>();
            Map<String,Object> configured=map(map(map(shop.get("pages")).get("page1")).get("items"));
            for(Object v:configured.values()){Map<String,Object> it=map(v);if(!itemSlots.add(((Number)it.get("slot")).intValue()))throw new IllegalStateException("Overlapping items");
                String material=(String)it.get("material");double buy=((Number)it.get("buy")).doubleValue(),sell=((Number)it.get("sell")).doubleValue();if(!(buy>0&&sell<buy))throw new IllegalStateException("Invalid price");
                if(costs.put(material,buy)!=null)throw new IllegalStateException("Duplicate material");if(sell>=0)sales.merge(material,sell,Math::max);items++;
            }
        }
        try(ZipFile jar=new ZipFile("versions/1.21.4/folia-1.21.4.jar")){
            var entries=jar.entries();while(entries.hasMoreElements()){var e=entries.nextElement();String p=e.getName();
                if(p.startsWith("data/minecraft/recipe/")&&p.endsWith(".json")){Map<String,Object> recipe=yaml.load(jar.getInputStream(e));if(recipe.get("result") instanceof Map<?,?>)recipes.add(recipe);}
                else if(p.startsWith("data/minecraft/tags/item/")&&p.endsWith(".json")){Map<String,Object> tag=yaml.load(jar.getInputStream(e));tags.put(p.substring("data/minecraft/tags/item/".length(),p.length()-5),tag.get("values"));}
            }
        }
        // A zero fuel cost is conservative: it catches even hypothetical free-smelting loops.
        for(int pass=0;pass<40;pass++){boolean changed=false;for(Map<String,Object> r:recipes){Map<String,Object> result=map(r.get("result"));if(!(result.get("id") instanceof String s))continue;
                double perItem=cost(r)/((Number)result.getOrDefault("count",1)).doubleValue();String material=id(s);if(perItem+1e-8<costs.getOrDefault(material,Double.POSITIVE_INFINITY)){costs.put(material,perItem);changed=true;}}
            if(!changed)break;
        }
        String aliasJson=Files.readString(Path.of("plugins/Essentials/items.json")).replaceAll("(?m)^#.*$","");Map<String,Object> aliases=yaml.load(aliasJson);
        Map<String,Object> worth=map(map(yaml.load(Files.readString(Path.of("plugins/Essentials/worth.yml")))).get("worth"));
        for(var e:worth.entrySet()){double price=-1;if(e.getValue() instanceof Number n)price=n.doubleValue();else if(e.getValue() instanceof Map<?,?> m)for(Object value:m.values())if(value instanceof Number n)price=Math.max(price,n.doubleValue());if(price>0)sales.merge(resolve(e.getKey(),aliases),price,Math::max);}
        for(var e:sales.entrySet()){double buy=costs.getOrDefault(e.getKey(),Double.POSITIVE_INFINITY);if(buy+1e-8<e.getValue())throw new IllegalStateException("Profitable resale/crafting loop: "+e.getKey()+" costs "+buy+", sells for "+e.getValue());}
        Map<String,Object> config=yaml.load(Files.readString(Path.of("plugins/EconomyShopGUI/config.yml")));if(!"lang-ru.yml".equals(config.get("language-file")))throw new IllegalStateException("Russian language disabled");
        Map<String,Object> language=yaml.load(Files.readString(Path.of("plugins/EconomyShopGUI/LanguageFiles/lang-ru.yml")));
        sameClasses(Path.of("backups/2026-11-01-server-shop/EconomyShopGUI-7.3.4-original.jar"),Path.of("plugins/FoliaUpdater/multithread/EconomyShopGUI-7.3.4.jar"));
        sameClasses(Path.of("backups/2026-11-01-server-shop/FancyNpcs-2.10.1-java21+2-original.jar"),Path.of("backups/2026-11-01-server-shop/FancyNpcs-2.10.1-java21+2-foliaupdater.jar"));
        System.out.println("Shop verified: "+catalog.size()+" categories, "+items+" items, "+language.size()+" language keys. No profitable resale via "+recipes.size()+" recipes or Essentials worth. Adapted plugin classes unchanged.");
    }
}
