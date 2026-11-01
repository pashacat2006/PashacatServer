import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.yaml.snakeyaml.Yaml;

/** Reproducible localization of the official jar; only text constants and resources change. */
@SuppressWarnings("unchecked")
class LocalizeCrazyAuctions {
    static final Path BACKUP=Path.of("backups/2026-11-01-crazyauctions");
    static final Map<String,String> texts=new HashMap<>();
    static int patchedConstants;
    static Set<String> placeholders(String s){Set<String> v=new TreeSet<>();Matcher m=Pattern.compile("%[^%]+%").matcher(s);while(m.find())v.add(m.group());return v;}
    static void compare(Object a,Object b,String path){
        if(a instanceof Map<?,?> m){if(!(b instanceof Map<?,?> n)||!m.keySet().equals(n.keySet()))throw new IllegalStateException("Keys changed: "+path);for(Object k:m.keySet())compare(m.get(k),n.get(k),path+"."+k);}
        else if(a instanceof List<?> l){if(!(b instanceof List<?> r)||l.size()!=r.size())throw new IllegalStateException("List changed: "+path);for(int i=0;i<l.size();i++)compare(l.get(i),r.get(i),path+"["+i+"]");}
        else if(a instanceof String s&&b instanceof String t){if(!placeholders(s).equals(placeholders(t)))throw new IllegalStateException("Placeholders changed: "+path);if(!s.equals(t))texts.put(s,t);}
        else if(!Objects.equals(a,b))throw new IllegalStateException("Setting changed: "+path);
    }
    static String translate(String s){String t=texts.getOrDefault(s,s);if(!t.equals(s))patchedConstants++;return t;}
    static byte[] patch(byte[] original){
        ClassReader reader=new ClassReader(original);ClassWriter writer=new ClassWriter(0);
        reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
            @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] exceptions){
                return new MethodVisitor(Opcodes.ASM9,super.visitMethod(access,name,desc,sig,exceptions)){
                    @Override public void visitLdcInsn(Object v){super.visitLdcInsn(v instanceof String s?translate(s):v);}
                    @Override public void visitInvokeDynamicInsn(String name,String desc,Handle h,Object... args){
                        Object[] translated=args.clone();for(int i=0;i<args.length;i++)if(args[i] instanceof String s)translated[i]=translate(s);
                        super.visitInvokeDynamicInsn(name,desc,h,translated);
                    }
                };
            }
        },0);return writer.toByteArray();
    }
    public static void main(String[] args)throws Exception{
        Yaml yaml=new Yaml();
        for(String file:List.of("config.yml","messages.yml")){
            Object original=yaml.load(Files.readString(BACKUP.resolve("original-"+file)));
            Object translated=yaml.load(Files.readString(Path.of("plugins/CrazyAuctions",file)));
            compare(original,translated,file);
        }
        texts.put("N/A","Нет данных");
        texts.put("\u0001d \u0001h \u0001m \u0001s ","\u0001д \u0001ч \u0001м \u0001с ");
        texts.put("&cThe bidding and selling options are both disabled. Please contact the admin about this.","&cПродажи и торги отключены. Обратитесь к администратору.");
        texts.put("&cPlease do /crazyauctions help for more information.","&cСправка по аукциону: /crazyauctions help");
        texts.put("Vault was not found so the plugin will now disable.","Vault не найден. Аукцион будет отключён.");
        texts.put("&cAn error has occurred with the item builder.","&cОшибка создания предмета.");
        texts.put("&c&lERROR","&c&lОШИБКА");
        texts.put("&cThere is an error","&cОшибка создания предмета");
        texts.put("&cFor : &c\u0001","&cДанные: &c\u0001");
        texts.put("An error has occurred with the item builder: ","Ошибка создания предмета: ");
        texts.put("Material cannot be of type AIR or null, If you see this.","Материал предмета имеет значение AIR или null.");
        texts.put("in your console but do not have any invalid items. You can","Если все предметы в настройках указаны верно,");
        texts.put("ignore this as we use AIR for some niche cases internally.","это предупреждение можно игнорировать: AIR также используется внутри плагина.");
        texts.put("Checking \u0001 for illegal unicode.","Проверка предмета \u0001 на запрещённые символы Unicode.");
        texts.put("\u0001 has passed unicode checks.","Предмет \u0001 прошёл проверку Unicode.");
        texts.put("\u0001 has failed unicode checks and has been denied.","Предмет \u0001 не прошёл проверку Unicode и отклонён.");
        texts.put("\u0001 is not a valid color.","Недопустимый цвет: \u0001.");
        // Older enum fallback strings differ from the resource defaults.
        texts.put("&cThat player is not online at this time.","&cЭтот игрок сейчас не в сети.");
        texts.put("&7You have cancelled an item on the auction list, return your items with /ah expired.","&7Вы сняли предмет с продажи. Забрать его: &e/ah expired&7.");
        texts.put("&7An item you have in the Crazy Auctions has just expired.","&7Срок продажи вашего предмета истёк. Забрать его: &e/ah expired&7.");
        texts.put("&7You have just force cancelled a sale.","&7Продажа принудительно отменена.");
        texts.put("&7You have just force cancelled all sales.","&7Все продажи принудительно отменены.");
        texts.put("&cOne of your items was just force cancelled by an Admin.","&cАдминистратор снял один из ваших предметов с продажи.");
        texts.put("&cThat item isnt in the crazy auctions any more.","&cЭтот предмет уже снят с аукциона.");
        texts.put("&cYou cant list any more items to the Crazy Auctions.","&cВы достигли лимита объявлений на аукционе.");
        texts.put("&cYour bid is to low, please bid more.","&cВаша ставка слишком мала. Предложите больше.");
        texts.put("&cThat is not a currency. Please use Money or Tokens .","&cНеизвестная валюта. Используйте Money (деньги).");
        texts.put("&cYour sell price is to low the minimum is &a$10&c.","&cЦена слишком мала. Минимальная цена: &a$10&c.");
        texts.put("&cYour sell price is to high the maximum is &a$1000000&c.","&cЦена слишком велика. Максимальная цена: &a$1000000&c.");
        texts.put("&cYour starting bid price is to low the minimum is &a$100&c.","&cНачальная ставка слишком мала. Минимум: &a$100&c.");
        texts.put("&cYour starting bid price is to high the maximum is &a$1000000&c.","&cНачальная ставка слишком велика. Максимум: &a$1000000&c.");
        texts.put("&7You have just bought a item for &a$%price%&7.","&7Вы купили предмет за &a$%price%&7.");
        texts.put("&7You have just won a bid for &a$%price%&7. Do /Ah Collect to collect your winnings.","&7Вы выиграли торги за &a$%price%&7. Забрать предмет: &e/ah collect&7.");
        texts.put("&7%player% has won your item you from a bid for &a$%price%.","&7Игрок &e%player% &7выиграл ваш предмет со ставкой &a$%price%&7.");
        texts.put("&7You have just added a item to the crazy auctions for &a$%price%&7.","&7Вы выставили предмет на аукцион за &a$%price%&7.");
        texts.put("&9/Ah Expired/Collect - &eView and manage your cancelled and expired items.","&9/ah expired/collect &7— &eзабрать снятые с продажи предметы и выигрыши.");
        Path output=args.length>0?Path.of(args[0]):Path.of("plugins/FoliaUpdater/multithread/CrazyAuctions-1.7.0.jar"),temp=Path.of(output+".ru.tmp");
        int classes=0;
        try(ZipFile zip=new ZipFile(BACKUP.resolve("CrazyAuctions-1.7.0-original.jar").toFile());ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(temp))){
            var entries=zip.entries();while(entries.hasMoreElements()){
                ZipEntry e=entries.nextElement();byte[] data=zip.getInputStream(e).readAllBytes();
                if(e.getName().equals("config.yml")||e.getName().equals("messages.yml"))data=Files.readAllBytes(Path.of("plugins/CrazyAuctions",e.getName()));
                else if(e.getName().equals("plugin.yml")){
                    String s=new String(data,StandardCharsets.UTF_8).replace("version: '1.7.0'","version: '1.7.0-ru.1'")
                        .replace("description: 'Auction off items in style.'","description: 'Аукцион предметов: продажи и торги.'")
                        .replace("description: Opens the Crazy Auctions GUI.","description: Открыть меню аукциона.")
                        .replace("softdepend: [Vault]","softdepend: [Vault, FoliaUpdater]");
                    data=s.getBytes(StandardCharsets.UTF_8);
                }else if(e.getName().startsWith("com/badbones69/crazyauctions/")&&e.getName().endsWith(".class")){data=patch(data);classes++;}
                out.putNextEntry(new ZipEntry(e.getName()));out.write(data);out.closeEntry();
            }
        }
        if(patchedConstants<50)throw new IllegalStateException("Too few translated constants: "+patchedConstants);
        Files.move(temp,output,StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Built CrazyAuctions 1.7.0-ru.1: "+classes+" classes checked, "+patchedConstants+" text constants translated. YAML keys, settings and placeholders verified.");
    }
}
