package dev.pashacat.titles;

import dev.pashacat.titles.TitleTransactions.*;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.luckperms.api.*;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.*;
import net.luckperms.api.query.QueryOptions;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public final class ServerTitles extends JavaPlugin implements CommandExecutor {
    private final Map<String,Title> titles=new LinkedHashMap<>();
    private final TitleTransactions transactions=new TitleTransactions();
    private LuckPerms luckPerms;
    private Economy economy;
    private Expansion expansion;
    private volatile boolean ready;
    @Override public void onEnable(){
        saveDefaultConfig();luckPerms=LuckPermsProvider.get();
        var provider=getServer().getServicesManager().getRegistration(Economy.class);
        if(provider==null)throw new IllegalStateException("Vault economy unavailable");economy=provider.getProvider();
        var section=getConfig().getConfigurationSection("titles");if(section==null)throw new IllegalStateException("Missing titles");
        for(String id:section.getKeys(false)){
            var s=section.getConfigurationSection(id);
            Title t=new Title(id,s.getString("name"),s.getString("suffix"),Kind.valueOf(s.getString("kind")),s.getLong("requirement"),s.getString("material"),s.getInt("slot"));
            titles.put(id,t);
        }
        Objects.requireNonNull(getCommand("servertitles")).setExecutor(this);
        expansion=new Expansion();if(!expansion.register())throw new IllegalStateException("Cannot register title placeholders");
        CompletableFuture<?>[] groups=titles.values().stream().map(t->luckPerms.getGroupManager().createAndLoadGroup(t.group()).thenCompose(group->{
            group.data().clear(n->n instanceof SuffixNode);
            group.data().add(SuffixNode.builder(t.suffix(),100000).build());
            return luckPerms.getGroupManager().saveGroup(group);
        })).toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(groups).thenCompose(v->luckPerms.getTrackManager().createAndLoadTrack("servertitles")).thenCompose(track->{
            for(Title title:titles.values())if(!track.containsGroup(title.group()))track.appendGroup(luckPerms.getGroupManager().getGroup(title.group()));
            return luckPerms.getTrackManager().saveTrack(track);
        }).whenComplete((ignored,error)->{if(error!=null)getLogger().log(Level.SEVERE,"Не удалось подготовить титулы",error);else {ready=true;getLogger().info("Титулы готовы: "+titles.size()+", экономика: "+economy.getName());}});
    }
    @Override public void onDisable(){ready=false;if(expansion!=null)expansion.unregister();}
    private User user(UUID uuid){return luckPerms.getUserManager().getUser(uuid);}
    private boolean managed(Node node){return node instanceof InheritanceNode n&&titles.values().stream().anyMatch(t->t.group().equals(n.getGroupName()));}
    private boolean ownership(Node node){return node instanceof PermissionNode n&&titles.values().stream().anyMatch(t->t.permission().equals(n.getPermission()));}
    private final class Profile implements TitleTransactions.Profile {
        private final User user;
        private Profile(User user){this.user=user;}
        public boolean owns(Title title){return user.data().toCollection().stream().anyMatch(n->n instanceof PermissionNode p&&p.getPermission().equals(title.permission())&&p.getValue()&&!p.hasExpiry());}
        public String selected(){return user.data().toCollection().stream().filter(ServerTitles.this::managed).filter(Node::getValue).map(n->((InheritanceNode)n).getGroupName()).map(g->g.substring("stitle_".length())).findFirst().orElse("none");}
        public Object checkpoint(){return user.data().toCollection().stream().filter(n->managed(n)||ownership(n)).toList();}
        public void select(Title title){
            user.data().clear(ServerTitles.this::managed);
            if(title!=null){user.data().add(PermissionNode.builder(title.permission()).build());user.data().add(InheritanceNode.builder(title.group()).build());}
        }
        @SuppressWarnings("unchecked") public void restore(Object snapshot){user.data().clear(n->managed(n)||ownership(n));for(Node node:(List<Node>)snapshot)user.data().add(node);}
        public CompletableFuture<Void> save(){return luckPerms.getUserManager().saveUser(user);}
    }
    private long ticks(Player p){return Integer.toUnsignedLong(p.getStatistic(Statistic.PLAY_ONE_MINUTE));}
    private long kills(Player p){return Math.max(0,p.getStatistic(Statistic.PLAYER_KILLS));}
    private void onPlayer(Player p,Runnable action){
        if(isEnabled())p.getScheduler().run(this,task->action.run(),()->getServer().getGlobalRegionScheduler().execute(this,action));
    }
    private void message(Player p,String text){if(p.isOnline())p.sendMessage(ChatColor.translateAlternateColorCodes('&',"&8[&6Титулы&8] &r"+text));}
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args){
        if(args.length>0&&args[0].equalsIgnoreCase("status")&&(sender instanceof ConsoleCommandSender||sender.hasPermission("servertitles.admin"))){
            String text="ServerTitles: ready="+ready+", titles="+titles.size()+", economy="+economy.getName();sender.sendMessage(text);getLogger().info(text);return true;
        }
        if(args.length>0&&args[0].equalsIgnoreCase("audit")&&sender instanceof ConsoleCommandSender){audit();return true;}
        if(!(sender instanceof Player player)){sender.sendMessage("Эта команда доступна только игрокам.");return true;}
        if(!player.hasPermission("servertitles.use")){message(player,"&cНет доступа к титулам.");return true;}
        if(!ready){message(player,"&eТитулы ещё загружаются. Попробуйте через несколько секунд.");return true;}
        User u=user(player.getUniqueId());if(u==null){message(player,"&cДанные прав ещё загружаются.");return true;}
        Title title;
        if(args.length==1&&args[0].equalsIgnoreCase("clear"))title=null;
        else if(args.length==2&&args[0].equalsIgnoreCase("select")&&titles.containsKey(args[1]))title=titles.get(args[1]);
        else {message(player,"&eОткройте меню командой /titles.");return true;}
        var account=new TitleTransactions.Account(){
            public boolean withdraw(long value){return economy.withdrawPlayer(player,value).transactionSuccess();}
            public boolean refund(long value){return economy.depositPlayer((OfflinePlayer)player,value).transactionSuccess();}
        };
        transactions.select(player.getUniqueId(),new Profile(u),account,title,ticks(player),kills(player),task->onPlayer(player,task),error->getLogger().log(Level.SEVERE,"Ошибка покупки/выбора титула для "+player.getUniqueId(),error))
            .thenAccept(result->onPlayer(player,()->{
                switch(result){
                    case SELECTED -> message(player,"&aВыбран титул "+title.suffix()+"&a. Полученные титулы можно менять бесплатно.");
                    case CLEARED -> message(player,"&aТитул снят. Все открытые титулы сохранены.");
                    case ALREADY_SELECTED -> message(player,"&eЭтот титул уже выбран.");
                    case BUSY -> message(player,"&eПредыдущая операция ещё выполняется.");
                    case REQUIREMENT -> message(player,"&cДля этого титула нужно "+(title.kind()==Kind.PLAYTIME?title.requirement()/72000+" ч. игрового времени":title.requirement()+" убийств игроков")+".");
                    case NO_MONEY -> message(player,"&cНедостаточно монет. Нужно: "+title.requirement()+".");
                    case STORAGE_ERROR -> message(player,"&cНе удалось сохранить выбор. Покупка отменена; если монеты были списаны, выполнен возврат.");
                }
            }));
        return true;
    }
    private void audit(){
        if(!ready){getLogger().warning("Аудит: титулы ещё не готовы.");return;}
        UUID probe=UUID.randomUUID();
        luckPerms.getUserManager().loadUser(probe).thenAccept(u->{
            try {
                u.data().add(InheritanceNode.builder("default").build());
                u.data().add(SuffixNode.builder("&7[Личный]",20).build());
                Title t=titles.values().iterator().next();new Profile(u).select(t);
                String suffix=u.getCachedData().getMetaData(QueryOptions.nonContextual()).getSuffix();
                if(!Objects.equals(suffix,t.suffix()))throw new IllegalStateException("More than one suffix: "+suffix);
                new Profile(u).select(null);
                String cleared=u.getCachedData().getMetaData(QueryOptions.nonContextual()).getSuffix();
                if(cleared==null||cleared.contains(t.name())||!cleared.contains("[Личный]"))throw new IllegalStateException("Clear lost existing suffix: "+cleared);
                getLogger().info("АУДИТ OK: показан только выбранный титул; после снятия вернулся обычный суффикс. Тестовый пользователь не сохранялся.");
            }catch(Throwable error){getLogger().log(Level.SEVERE,"АУДИТ FAILED",error);}finally{luckPerms.getUserManager().cleanupUser(u);}
        });
    }
    private final class Expansion extends PlaceholderExpansion {
        public String getIdentifier(){return "servertitles";}public String getAuthor(){return "Pashacat";}public String getVersion(){return "1.0.0";}public boolean persist(){return true;}
        @Override public String onRequest(OfflinePlayer offline,String params){
            Player p=offline==null?null:offline.getPlayer();User u=offline==null?null:user(offline.getUniqueId());Profile profile=u==null?null:new Profile(u);
            if(params.equals("ready"))return ready?"yes":"no";
            if(params.equals("selected_id"))return profile==null?"none":profile.selected();
            if(params.equals("selected")){Title t=profile==null?null:titles.get(profile.selected());return t==null?"&7Не выбран":t.suffix();}
            if(params.equals("playtime")){long minutes=p==null?0:ticks(p)/1200;return minutes/60+" ч. "+minutes%60+" мин.";}
            if(params.equals("kills"))return p==null?"0":String.valueOf(kills(p));
            if(params.startsWith("status_")){
                Title t=titles.get(params.substring(7));if(t==null)return "";
                if(profile!=null&&profile.selected().equals(t.id()))return "&aСейчас выбран";
                if(profile!=null&&profile.owns(t))return "&aПолучен · выбрать бесплатно";
                boolean eligible=p!=null&&((t.kind()==Kind.PLAYTIME&&ticks(p)>=t.requirement())||(t.kind()==Kind.KILLS&&kills(p)>=t.requirement()));
                return eligible?"&aДоступен · нажмите, чтобы получить":t.kind()==Kind.MONEY?"&eКупить и надеть":"&cПока недоступен";
            }
            return null;
        }
    }
}
